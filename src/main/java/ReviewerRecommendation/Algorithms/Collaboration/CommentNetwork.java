package ReviewerRecommendation.Algorithms.Collaboration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.eclipse.egit.github.core.PullRequest;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.Modularity;
import org.openide.util.Lookup;

import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.ReReEntrance.Pair;
import ReviewerRecommendation.Algorithms.RecommendAlgo;

public class CommentNetwork implements RecommendAlgo{
	
	class Tuple {
		String name;
		Double val;
		Tuple(String name, Double val) {
			this.name = name;
			this.val = val;
		}
		public String getName() {
			return name;
		}
		public Double getVal() {
			return val;
		}
	}
	
	class MyNode {
		Node node;
		Integer indegree;
		public MyNode(Node node, int in) {
			this.node = node;
			this.indegree = in;
		}
		public Node getNode() {
			return node;
		}
		public Integer getIndegree() {
			return indegree;
		}
	}
	
	private boolean isBuild = false;
	public Map<String, PriorityQueue<Tuple>> commentNet = new HashMap<String, PriorityQueue<Tuple>>();
	
	private Set<String> reviewersSet = new HashSet<String>();
	private Map<Integer, Set<String>> reviewersPerPR = new HashMap<Integer, Set<String>>();
	
	private List<Map.Entry<String, PriorityQueue<MyNode>>> communityList = null;
	
	private Map<String, Double> reviewersScore;
	
	public Map<String, Double> getReviewersScore() {
		return reviewersScore;
	}

	private void trainModel(int cur, ReReEntrance ent) {
		constructNetwork(cur, ent);
		mineFrequentItemsets(cur, ent);
		communityDetection();
	}

	private void communityDetection() {
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Get controllers and models
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);
//        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
//        AppearanceModel appearanceModel = appearanceController.getModel();
//        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        
        DirectedGraph directedGraph = graphModel.getDirectedGraph();

        for (Map.Entry<String, PriorityQueue<Tuple>> each : commentNet.entrySet()) {
        	String ni = each.getKey();
        	for (Tuple t : each.getValue()) {
        		String nj = t.getName();
        		double val = t.getVal();
        		
        		Node a, b;
        		if (directedGraph.getNode(ni) == null) {
        			a = graphModel.factory().newNode(ni);
        			directedGraph.addNode(a);
        		}
        		else
        			a = directedGraph.getNode(ni);
        		
        		if (directedGraph.getNode(nj) == null) {
        			b = graphModel.factory().newNode(nj);
        			directedGraph.addNode(b);
        		}
        		else
        			b = directedGraph.getNode(nj);
        		Edge edge = graphModel.factory().newEdge(a, b, 0, val, true);
                directedGraph.addEdge(edge);
        	}
        }

        System.out.println("Nodes: " + directedGraph.getNodeCount() + 
        		" Edges: " + directedGraph.getEdgeCount());
        
        //Run modularity algorithm - community detection
        Modularity modularity = new Modularity();
        modularity.setRandom(true);
        modularity.setResolution(1.0);
        modularity.setUseWeight(false);
        modularity.execute(graphModel);
        
//        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
//        Function func2 = appearanceModel.getNodeFunction(directedGraph, modColumn, PartitionElementColorTransformer.class);
//
//        Partition partition2 = ((PartitionFunction) func2).getPartition();
//        System.out.println(partition2.size() + " partitions found");
//        Palette palette2 = PaletteManager.getInstance().randomPalette(partition2.size());
//        partition2.setColors(palette2.getColors());
//        appearanceController.transform(func2);
//
//        //Export
//        
//        try {
//            ec.exportFile(new File("C:\\Users\\buaaxzl\\Desktop\\partition2.pdf"));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            return;
//        }
//        
//        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
//        GraphDistance distance = new GraphDistance();
//        distance.setDirected(true);
//        distance.execute(graphModel);
//        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        
        Map<String, PriorityQueue<MyNode>> communities = new HashMap<String, PriorityQueue<MyNode>>();
        
        for (Node n : directedGraph.getNodes()) {
        	String key = ((Integer)n.getAttribute(Modularity.MODULARITY_CLASS)).toString();
        	int in = directedGraph.getInDegree(n);
        	MyNode myNode = new MyNode(n, in);
        	
        	if (communities.containsKey(key))
    			communities.get(key).offer(myNode);
    		else {
    			PriorityQueue<MyNode> tmp = new PriorityQueue<MyNode>(11, new Comparator<MyNode>() {
					@Override
					public int compare(MyNode o1, MyNode o2) {
						return o2.getIndegree() - o1.getIndegree();
					}
				});
    			tmp.offer(myNode);
    			communities.put(key, tmp);
    		}
        }
        
        communityList = 
        		new ArrayList<Map.Entry<String, PriorityQueue<MyNode>>>(communities.entrySet());
        
        Collections.sort(communityList, new Comparator<Map.Entry<String, PriorityQueue<MyNode>>>() {
			@Override
			public int compare(Entry<String, PriorityQueue<MyNode>> o1,
					Entry<String, PriorityQueue<MyNode>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		});
        
//        for (Map.Entry<String, PriorityQueue<MyNode>> each : communityList)
//        	System.out.println(each.getValue().size());
        
        pc.closeCurrentProject();
	}

	private void mineFrequentItemsets(int cur, ReReEntrance ent) {
		for (int i = 0; i < cur; i++) {
			Set<String> tmp = new HashSet<String>();
			
			if (ent.getPrList().get(i).getUser() != null) {
				String author = ent.getPrList().get(i).getUser().getLogin();
				tmp.add(author);
			}
			List<String> revs = ent.getCodeReviewers(i);
			tmp.addAll(revs);
			reviewersPerPR.put(i, tmp);
			reviewersSet.addAll(tmp);
		}
//		List<String> reviewersList = new ArrayList<String>(reviewersSet);
//		ArrayList<Attribute> atts = new ArrayList<Attribute>();
//		
//		for (int i = 0; i < reviewersList.size(); i++) {
//			ArrayList<String> labels = new ArrayList<String>();
//			labels.add("F");
//			labels.add("T");
//			Attribute nomial = new Attribute(reviewersList.get(i), labels);
//			atts.add(nomial);
//		}
//		Instances data = new Instances("reviewers co-occurance", atts, 0);
//		
//		for (Map.Entry<Integer, List<String>> each : reviewersPerPR.entrySet()) {
//			List<String> val = each.getValue();
//			SparseInstance ins = new SparseInstance(reviewersList.size());
//			for (String a : val) {
//				int index = reviewersList.indexOf(a);
//				ins.setValue(data.attribute(index), "T");
//			}
//			data.add(ins);
//		}
//	    // build associator
//	    Apriori apriori = new Apriori();
//	    
//	    double deltaValue = 0.05;
//	    double lowerBoundMinSupportValue = 0.05;
//	    double minMetricValue = 0.2;
//	    int numRulesValue = 20;
//	    double upperBoundMinSupportValue = 1.0;
//	    
//	    apriori.setDelta(deltaValue);
//	    apriori.setLowerBoundMinSupport(lowerBoundMinSupportValue);
//	    apriori.setNumRules(numRulesValue);
//	    apriori.setUpperBoundMinSupport(upperBoundMinSupportValue);
//	    apriori.setMinMetric(minMetricValue);
//	    
//	    try {
//			apriori.buildAssociations(data);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	    
//	    System.out.println(apriori.getAllTheRules().length);
//	    
//	    AssociationRules rules = apriori.getAssociationRules();
//	    for (AssociationRule each : rules.getRules()) {
//	    	System.out.println(each.getTotalTransactions());
//	    	List<Item> conseq = (List<Item>) each.getConsequence();
//	    	List<Item> premise = (List<Item>) each.getPremise();
//	    	
//	    	for (Item item : premise) {
//	    		System.out.print(item.toString() + " ");
//	    	}
//	    	System.out.print("--->");
//	    	for (Item item : conseq) {
//	    		System.out.print(item.toString() + " ");
//	    	}
//	    	System.out.println();
//	    }
//	    
//	    System.out.println(apriori);
	}

	private void constructNetwork(int cur, ReReEntrance ent) {
		Date startTime = ent.getPrList().get(0).getCreatedAt();
		Date endTime = ent.getPrList().get(cur).getCreatedAt();
		
		Map<String, Map<String, Map<Integer, List<Pair>>>> record = 
				new HashMap<String, Map<String, Map<Integer, List<Pair>>>>();
		
		for (int i = 0; i < cur; i++) {
			if (ent.getPrList().get(i).getUser() == null) continue;
			String author = ent.getPrList().get(i).getUser().getLogin();
			
			Map<String, List<Pair>> cont = ent.getReviewerContribution(i);
			Map<String, Map<Integer, List<Pair>>> comments = 
					new HashMap<String, Map<Integer, List<Pair>>>();
			
			for (Map.Entry<String, List<Pair>> each : cont.entrySet()) {
				Map<Integer, List<Pair>> tmp = new HashMap<Integer, List<Pair>>();
				tmp.put(i, each.getValue());
				comments.put(each.getKey(), tmp);
			}
			
			if (record.containsKey(author)) {
				Map<String, Map<Integer, List<Pair>>> tmp = record.get(author);
				for (Map.Entry<String, Map<Integer, List<Pair>>> each : comments.entrySet()) {
					if (tmp.containsKey(each.getKey())) {
						if (each.getValue().size() != 1)
							System.out.println("**********************");
						tmp.get(each.getKey()).putAll(each.getValue());
					}
					else
						tmp.put(each.getKey(), each.getValue());
				}
			}
			else {
				record.put(author, comments);
			}
		}
		
		for (Map.Entry<String,  Map<String, Map<Integer, List<Pair>>>> each1 : record.entrySet()) {
			String vi = each1.getKey();
			for (Map.Entry<String, Map<Integer, List<Pair>>> each2 : each1.getValue().entrySet()) {
				String vj = each2.getKey();
				
				double val = 0.0;
				for (Map.Entry<Integer, List<Pair>> each3 : each2.getValue().entrySet()) {
					double lamda = 1.0;
					List<Pair> pairs = each3.getValue();
					Collections.sort(pairs, new Comparator<Pair>() {
						@Override
						public int compare(Pair o1, Pair o2) {
							return o2.getDate().compareTo(o1.getDate());
						}
					});
					
					for (Pair comment : pairs) {
						val += lamda * ((comment.getDate().getTime()+0.0 - startTime.getTime()+0.0)/
											(endTime.getTime()+0.0 - startTime.getTime()+0.0));
						lamda = lamda * 0.8;
					}
				}
				
				if (commentNet.containsKey(vi))
					commentNet.get(vi).add(new Tuple(vj, val));
				else {
					PriorityQueue<Tuple> tmp = new PriorityQueue<Tuple>(11, new Comparator<Tuple>() {
						@Override
						public int compare(Tuple o1, Tuple o2) {
							return o2.getVal().compareTo(o1.getVal());
						}
					});
					tmp.add(new Tuple(vj, val));
					
					commentNet.put(vi, tmp);
				}
			}
		}
	}
	
	//*****Metric******
	int total = 0;
	int pac = 0, pacCorrect = 0;
	int pncA = 0, pncACorrect = 0;
	int pncG = 0, pncGCorrect = 0;
	//*****************
	
	@Override
	public List<String> recommend(int i, ReReEntrance ent) {
		if (!isBuild) {
			isBuild = true;
			trainModel(i, ent);
		}
		
		reviewersScore = new HashMap<String, Double>();
		
		Set<String> ret = new HashSet<String>();
		String user = ent.getPrList().get(i).getUser().getLogin();
		if (user == null) return null;
		
		total++;
		if (commentNet.containsKey(user)) {
			pac ++;
			recommendForPAC(ent, ret, user);
			
//			debug(i, ent, ret, 1);
		}
		else return null;
		
		
//		else {
//			/*
//			 * process PNC Apriori
//			 */
//			if (reviewersSet.contains(user)) {
//				pncA ++;
//				
//				Map<String, Integer> count = new HashMap<String, Integer>();
//				for (Map.Entry<Integer, Set<String>> each : reviewersPerPR.entrySet()) {
//					if (each.getValue().contains(user)) {
//						for (String person : each.getValue()) {
//							if (person.equals(user)) continue;
//							if (count.containsKey(person))
//								count.put(person, count.get(person) + 1);
//							else
//								count.put(person, 1);
//						}
//					}
//				}
//				
//				List<Map.Entry<String, Integer>> res = SortMapElement.sortIntegerDesc(count);
//				for (int j = 0; j < ent.getK() && j < res.size(); j++) {
//					ret.add(res.get(j).getKey());
//					reviewersScore.put(res.get(j).getKey(), res.get(j).getValue().doubleValue());
//				}
//				
////				debug(i, ent, ret, 2);
//			}
//			/*
//			 * process PNC gephi
//			 */
//			else {
//				pncG ++;
//				
//				List<PriorityQueue<MyNode>> copy = new ArrayList<PriorityQueue<MyNode>>();
//				for (Map.Entry<String, PriorityQueue<MyNode>> each : communityList) {
//					if (each.getValue().size() < 2) continue;
//					copy.add(new PriorityQueue<MyNode>(each.getValue()));
//				}
//				
//				while( ret.size() != ent.getK()) {
//					for (PriorityQueue<MyNode> each : copy) {
//						if (each.size() == 0) continue;
//						MyNode node = each.poll();
//						ret.add((String)node.getNode().getId());
//						reviewersScore.put((String)node.getNode().getId(), 
//											node.getIndegree().doubleValue());
//						if (ret.size() == ent.getK())
//							break;
//					}
//				}
//				
////				debug(i, ent, ret, 3);
//			}
//		}
		
//		System.out.println(total + " " + pac + " " + pncA + " " + pncG);
//		System.out.println(total + " " + pacCorrect + " " + pncACorrect + " " + pncGCorrect);
		return new ArrayList<String>(ret);
	}

	private void debug(int i, ReReEntrance ent, Set<String> ret, int condition) {
		List<String> trueReviewers = ent.getCodeReviewers(i);
		int cnt = 0;
		for (String each : ret) 
			for (String tr : trueReviewers)
				if (each.equals(tr))
					cnt ++;
		
		if (cnt > 0) {
			if (condition == 1) pacCorrect ++;
			if (condition == 2) pncACorrect ++;
			if (condition == 3) pncGCorrect ++;
		}
	}

	private void recommendForPAC(ReReEntrance ent, Set<String> ret, String user) {
		Queue<String> q = new LinkedList<String>();
		q.offer(user);
		while (!q.isEmpty()) {
			String str = q.poll();
			PriorityQueue<Tuple> inner = commentNet.get(str);
			if (inner == null) continue;
			
			PriorityQueue<Tuple> tmp = new PriorityQueue<Tuple>(inner);
			while (!tmp.isEmpty()) {
				Tuple t = tmp.poll();
				if (ret.contains(t.getName())) continue;
				ret.add(t.getName());
				reviewersScore.put(t.getName(), t.getVal());
				q.offer(t.getName());
				if (ret.size() == ent.getK())
					break;
			}
			if (ret.size() == ent.getK())
				break;
		}
	}
	
	public static void main(String[] args) {
		ReReEntrance ent = new ReReEntrance("netty", "netty", "windows");
		CommentNetwork cn = new CommentNetwork();
		int index = 0;
		for (PullRequest pr : ent.getPrList()) {
			if (pr.getNumber() == 6146)
				break;
			index ++;
		}
		cn.constructNetwork(index, ent);
		
		int val = 0;
		int cnt = 0;
		for ( Map.Entry<String, PriorityQueue<Tuple>> each : cn.commentNet.entrySet()) {
			if (each.getKey().equals("tbrooks8"))
				for (Tuple t : each.getValue())
					System.out.println(each.getKey() + "-->" + t.getName() + " val: " + t.getVal());
//			
			if (each.getKey().equals("jasontedor"))
				for (Tuple t : each.getValue())
					System.out.println(each.getKey() + "-->" + t.getName() + " val: " + t.getVal());
			
			for (Tuple t : each.getValue()) {
				if (t.getName().equals("jasontedor")) {
//					val += t.getVal();
//					cnt ++;
					System.out.println(each.getKey() + "-->" + t.getName() + " val: " + t.getVal());
				}
			}
			
			for (Tuple t : each.getValue()) {
				if (t.getName().equals("tbrooks8")) {
//					val += t.getVal();
//					cnt ++;
					System.out.println(each.getKey() + "-->" + t.getName() + " val: " + t.getVal());
				}
			}
		}
		
		System.out.println(cnt + " " + val);
	}
}
