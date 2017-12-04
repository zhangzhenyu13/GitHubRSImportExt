package ReviewerRecommendation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;

import tools.SortMapElement;
import ReviewerRecommendation.Algorithms.IRandCN;
import ReviewerRecommendation.Algorithms.RecommendAlgo;
import ReviewerRecommendation.Algorithms.Activeness.Activeness;
import ReviewerRecommendation.Algorithms.Collaboration.CommentNetwork;
import ReviewerRecommendation.Algorithms.FPS.FPS;
import ReviewerRecommendation.Algorithms.IR.IRBasedRecommendation;
import ReviewerRecommendation.Algorithms.NBClassification.TIEComposer;
import ReviewerRecommendation.Algorithms.NBClassification.WVToolProcess;
import ReviewerRecommendation.Algorithms.RSVD.SVDMixer;
import ReviewerRecommendation.Algorithms.RSVD.SVDRecommendationFile;
import ReviewerRecommendation.Algorithms.RSVD.SVDRecommendationPR;
import ReviewerRecommendation.Algorithms.RSVD.SVDRecommendationUser;
import ReviewerRecommendation.DataProcess.DataPreparation;
import au.com.bytecode.opencsv.CSVWriter;
import edu.udo.cs.wvtool.generic.vectorcreation.TFIDF;

public class ReReEntrance {
	public static class Pair {
		String reviewer;
		String path;
		String comment;
		Date date;
		public String getReviewer() {
			return reviewer;
		}
		public String getPath() {
			return path;
		}
		public Date getDate() {
			return date;
		}
		public Pair(String rev, String path, String com, Date date) {
			this.reviewer = rev;
			this.path = path;
			this.comment = com;
			this.date = date;
		}
		public Pair(String rev, String com, Date date) {
			this.reviewer = rev;
			this.comment = com;
			this.date = date;
		}
	}
	
	private String repo = "netty";
	private String owner = "netty";
	private DataPreparation dp = new DataPreparation();
	
	public static String platform = "windows";
	
	public int k = 10;  //1, 3, 5, 10
	
	public int M = 1;
	public int N = 500;     //500
	public int cnt = 100;
	private List<PullRequest> prList = new ArrayList<PullRequest>();

	private Map<Integer, List<String>> prReviewersFromCommitComment = new HashMap<Integer, List<String>>();
	private Map<Integer, List<String>> prReviewersFromIssueComment = new HashMap<Integer, List<String>>();
	
	private Map<Integer, List<Pair>> CommitCommentContribution = new HashMap<Integer, List<Pair>>();
	private Map<Integer, List<Pair>> IssueCommentContribution = new HashMap<Integer, List<Pair>>();
	
	public void setK(int k) {
		this.k = k;
	}
	public int getK() {
		return k;
	}
	
	public String getRepo() {
		return repo;
	}

	public String getOwner() {
		return owner;
	}
	
	public ReReEntrance(String repo, String owner, String platform) {
		this.repo = repo;
		this.owner = owner;
		ReReEntrance.platform = platform;
		
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		prepareData();
		preProcessPRCodeReviewers();
		
		filterData();
		preProcessContribution();
	}

	private List<CommitComment> getCommitComments(int i) {
		return dp.getPrComments().get(prList.get(i).getNumber()+"");
	}
	
	private List<Comment> getIssueComments(int i) {
		return dp.getIssueComments().get(prList.get(i).getNumber()+"");
	}
	
	private void preProcessContribution() {
		for (int i = 0; i < prList.size(); i++) {
			List<CommitComment> ccs = getCommitComments(i);
			if (ccs != null) {
				List<Pair> pairs1 = new ArrayList<Pair>();
				for (CommitComment cc : ccs) {
					if (cc.getUser() == null) continue;
					String reviewer = cc.getUser().getLogin();
					String path = cc.getPath();
					String contribution = cc.getBody();
					Date date = cc.getCreatedAt();
					pairs1.add(new Pair(reviewer, path, contribution, date));
				}
				CommitCommentContribution.put(i, pairs1);
			}
			
			List<Comment> comments = getIssueComments(i);
			if (comments != null) {
				List<Pair> pairs2 = new ArrayList<Pair>();
				for (Comment com : comments) {
					if (com.getUser() == null) continue;
					String reviewer = com.getUser().getLogin();
					String contribution = com.getBody();
					Date date = com.getCreatedAt();
					pairs2.add(new Pair(reviewer, contribution, date));
				}
				IssueCommentContribution.put(i, pairs2);
			}
		}
	}
	
	public Map<String, List<Pair>> getReviewerContribution(int i) {
		List<Pair> pairs1 = IssueCommentContribution.get(i);
		List<Pair> pairs2 = CommitCommentContribution.get(i);
		Map<String, List<Pair>> ret = new HashMap<String, List<Pair>>();
		for (Pair each : pairs1)
			if (ret.containsKey(each.getReviewer()))
				ret.get(each.getReviewer()).add(each);
			else {
				List<Pair> tmp = new ArrayList<Pair>();
				tmp.add(each);
				ret.put(each.getReviewer(), tmp);
			}
		
		for (Pair each : pairs2)
			if (ret.containsKey(each.getReviewer()))
				ret.get(each.getReviewer()).add(each);
			else {
				List<Pair> tmp = new ArrayList<Pair>();
				tmp.add(each);
				ret.put(each.getReviewer(), tmp);
			}
		
		String author = "";
		if (prList.get(i).getUser() != null)
			author = prList.get(i).getUser().getLogin();
		ret.remove(author);
		
		return ret;
	}
	
	private void filterData() {
		Set<Integer> removal = new HashSet<Integer>();
		
		Map<String, Map<Integer, Double>>  wordVector = null;
		WVToolProcess wvtp = new WVToolProcess(new TFIDF());
		try {
			wvtp.process("pr", prList.size(), this);
			wordVector = wvtp.getWordVector();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < prList.size(); i++) {
			if (getCodeReviewers(i) == null || getCodeReviewers(i).size() < 2) {
				removal.add(i);
			}
			if (wordVector.get(prList.get(i).getNumber()+"").size() < 5)
				removal.add(i);
		}
		
		List<PullRequest> prListTmp = new ArrayList<PullRequest>();
		Map<Integer, Integer> indexMap = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < prList.size(); i++) {
			if (!removal.contains(i)) {
				prListTmp.add(prList.get(i));
				indexMap.put(prListTmp.size()-1, i);
			}
			else {
				prReviewersFromCommitComment.remove(i);
				prReviewersFromIssueComment.remove(i);
			}
		}
		
		for (Map.Entry<Integer, Integer> each : indexMap.entrySet()) {
			List<String> val = prReviewersFromCommitComment.get(each.getValue());
			prReviewersFromCommitComment.remove(each.getValue());
			prReviewersFromCommitComment.put(each.getKey(), val);
			
			List<String> val1 = prReviewersFromIssueComment.get(each.getValue());
			prReviewersFromIssueComment.remove(each.getValue());
			prReviewersFromIssueComment.put(each.getKey(), val1);
		}
		
		prList = prListTmp;
		System.out.println("Filtered Prs: " + prList.size());
	}
	
	public int temporalLocality(int cur) {
		int ret = 0;
		int cnt = 0;
		Date now = this.getPrList().get(cur).getCreatedAt();
		long nowMilli = now.getTime();
		for (int i = cur - 1; i >= 0; i--) {
			
			Date past = this.getPrList().get(i).getCreatedAt();
			long pastMilli = past.getTime();
			if (nowMilli - pastMilli > M*24*3600*1000) {
				ret = i;
				cnt ++;
				break;
			}
			cnt ++;
		}
		
		if (cnt < N) {
			for (int i = ret-1; i >= 0; i--) {
				cnt ++;
				if (cnt >= N)  {
					ret = i;
					break;
				}
			}
		}
		return ret;
	}
	
	public DataPreparation getDp() {
		return dp;
	}
	
	public List<PullRequest> getPrList() {
		return prList;
	}
	
	private void prepareData() {
		dp.prepare(owner+"-"+repo);
		
		Map<String, PullRequest> prs = dp.getPrs();
		
		for (Map.Entry<String, PullRequest> each : prs.entrySet()) {
			if (each.getValue().getState().equals("closed") || 
					each.getValue().isMerged())
				prList.add(each.getValue());
		}
		
		System.out.println("prList length: " + prList.size());
		
		Collections.sort(prList, new Comparator<PullRequest>() {
			@Override
			public int compare(PullRequest o1, PullRequest o2) {
				return o1.getCreatedAt().compareTo(o2.getCreatedAt());
			}
		});
	}
	
	private void preProcessPRCodeReviewers() {
		for (int i = 0; i < prList.size(); i++) {
			PullRequest pr = prList.get(i);
			List<CommitComment> prComments = dp.getPrComments().get(pr.getNumber()+"");
			List<Comment> issueComments = dp.getIssueComments().get(pr.getNumber()+"");
			
			int flag = 0;
			if (prComments == null) {
				prReviewersFromCommitComment.put(i, null);
				flag ++;
			}
			if (issueComments == null) {
				prReviewersFromIssueComment.put(i, null);
				flag ++;
			}
			if (flag == 2)
				continue;
			
			List<String> reviewersFromCommitComment = new ArrayList<String>();
			List<String> reviewersFromIssueComment = new ArrayList<String>();
			if (prComments != null)
				for (CommitComment each : prComments) {
					if (each.getUser() == null)
						continue;
					if (!reviewersFromCommitComment.contains(each.getUser().getLogin()))
						reviewersFromCommitComment.add(each.getUser().getLogin());
				}
			
			if (issueComments != null)
				for (Comment each : issueComments) {
					if (each.getUser() == null)
						continue;
					if (!reviewersFromIssueComment.contains(each.getUser().getLogin()))
						reviewersFromIssueComment.add(each.getUser().getLogin());
				}
			
			prReviewersFromCommitComment.put(i, reviewersFromCommitComment);
			prReviewersFromIssueComment.put(i, reviewersFromIssueComment);
		}
	}
	
	public List<String> getCodeReviewers(int rp) {
		if (prReviewersFromCommitComment.get(rp) == null
				&& prReviewersFromIssueComment.get(rp) == null)
			return new ArrayList<String>();
		
		Set<String> reviewers = new HashSet<String>();
		reviewers.addAll(prReviewersFromCommitComment.get(rp));
		reviewers.addAll(prReviewersFromIssueComment.get(rp));
		String author = "";
		if (prList.get(rp).getUser() != null)
			author = prList.get(rp).getUser().getLogin();
		if (reviewers.contains(author))
			reviewers.remove(author);
		
		List<String> ret = new ArrayList<String>(reviewers);
		return ret;
	}
	
	public void start(RecommendAlgo ra) {
		int len = prList.size();
		int correct = 0;
		int num = 0;
		int runExp = 0;
		
		for (int i = len - cnt; i < len && i >= 0; i++) {
			System.out.println("begin " + i + " pull request");
			
			List<String> reviewers = ra.recommend(i, this);
			
			try {
				if (isCorrect(i, reviewers))
					correct++;
			} catch (RuntimeException e) {
				runExp ++;
				System.out.println("*********************RuntimeException " 
									+ e.getMessage());
				num --;
			}
			num ++;
			System.out.println("finished " + i + " pull request");
		}
		System.out.println("correct: " + correct);
		System.out.println("total: " + num + " \n"
				+ "top-" + k + " accuracy is " + (correct/(num+0.0)));
		System.out.println("Runtime Exception : " + runExp);
	}
	
	public boolean isCorrect(int rn, List<String> reviewers) {
		if (reviewers == null) throw new RuntimeException("recommend nobody");
		
		List<String> trueReviewers = getCodeReviewers(rn);
		if (trueReviewers == null || trueReviewers.size() == 0) 
			throw new RuntimeException("doestn't have reviewers");
		
		for (String each : reviewers) 
			for (String tr : trueReviewers)
				if (each.equals(tr))
					return true;
		return false;
	}
	
	public boolean hasRankNet = false;
	private Map<String, Map<String, Long>> rankNet = new HashMap<String, Map<String, Long>>();
	
	public void constructRankNet(int end) {
		if (hasRankNet) return;
		for (int i = 0; i < end; i++) {
			if (prList.get(i).getUser() == null) continue;
			String from = prList.get(i).getUser().getLogin();
			if (!rankNet.containsKey(from)) 
				rankNet.put(from, new HashMap<String, Long>());
			Date created = prList.get(i).getCreatedAt();
			Map<String, List<Pair>> tmp = getReviewerContribution(i);
			for (Map.Entry<String, List<Pair>> each : tmp.entrySet()) {
				String to = each.getKey();
				Date earliest = new Date(System.currentTimeMillis());
				for (Pair pair : each.getValue())
					if (pair.getDate().before(earliest))
						earliest = pair.getDate();
				if (!rankNet.get(from).containsKey(to))
					rankNet.get(from).put(to, earliest.getTime() - created.getTime());
				else
					rankNet.get(from).put(to, rankNet.get(from).get(to) + 
							earliest.getTime() - created.getTime());
			}
		}
		hasRankNet = true;
	}
	
	private List<String> rankOptimization(List<String> reviewers, int i) {
		if (prList.get(i).getUser() == null) return reviewers;
		String author = prList.get(i).getUser().getLogin();
		if (!rankNet.containsKey(author)) return reviewers;
		
		Map<String, Long> common = new HashMap<String, Long>();
		for (String reviewer : reviewers)
			if (rankNet.get(author).containsKey(reviewer))
				common.put(reviewer, rankNet.get(author).get(reviewer));
		
		List<Map.Entry<String, Long>> sorted = SortMapElement.sortLong(common);
		int cursor = 0;
		for (int j = 0; j < reviewers.size(); j++) {
			String rev = reviewers.get(j);
			if (common.containsKey(rev)) {
				reviewers.set(j, sorted.get(cursor).getKey());
				cursor ++;
			}
		}
		return reviewers;
	}
	
	private double evaluateRank(List<String> reviewers, List<String> ranked) {
		List<Integer> a = new ArrayList<Integer>();
		List<Integer> b = new ArrayList<Integer>();
		List<String> common = new ArrayList<String>();
		
		for (String rev : ranked) {
			if (reviewers.contains(rev))
				common.add(rev);
		}
		
		if (common.size() < 2) return 0.0;
		
		for (String rev : common) {
			a.add(ranked.indexOf(rev) + 1);
			b.add(reviewers.indexOf(rev) + 1);
		}
		return similarity(a, b);
	}
	
	private double similarity(List<Integer> a, List<Integer> b) {
		int n = a.size();
		double xTwice  = 0.0, yTwice = 0.0, xTotal = 0.0, yTotal = 0.0, xy = 0.0;
		for (int k = 0; k < n; k++) {
			double x = a.get(k);
			double y = b.get(k);
			xTwice += x*x;
			yTwice += y*y;
			xTotal += x;
			yTotal += y;
			xy += x * y;
		}
		double pearson = 0.0;

		pearson = (xy - xTotal*yTotal/n)/
					Math.sqrt((xTwice - xTotal*xTotal/n)*(yTwice - yTotal*yTotal/n));
		return pearson;
//		return ((n+0.0)/(n+20.0))*pearson;
	}
	
	private List<String> getRankedReviewers(int i) {
		List<String> rankedTruth = new ArrayList<String>();
		Map<String, List<Pair>> cont = getReviewerContribution(i);
		Map<String, Date> reviewerTimePair = new HashMap<String, Date>();
		for (Map.Entry<String, List<Pair>> each : cont.entrySet()) {
			Date early = new Date(System.currentTimeMillis());
			for (Pair pair : each.getValue()) {
				if (pair.getDate().before(early))
					early = pair.getDate();
			}
			reviewerTimePair.put(each.getKey(), early);
		}
		List<Map.Entry<String, Date>> sorted = SortMapElement.sortDate(reviewerTimePair);
		for (Map.Entry<String, Date> each : sorted)
			rankedTruth.add(each.getKey());
		return rankedTruth;
	}
	
	private int methodNum = -1;
	public void startEvalByFmeasure(RecommendAlgo ra) {
		System.out.println(ra.getClass());
		
		int len = prList.size();
		int num = 0;
		double[] precision = new double[10];
		double[] recall = new double[10];
		double rankMetric1 = 0.0;
		double rankMetric2 = 0.0;
		double mrr = 0.0;
		methodNum ++;
		
		for (int p = 0; p < 10; p++) {
			precision[p] = 0;
			recall[p] = 0;
		}
//		constructRankNet(origin);
		setK(10);
		
		int start = len - 100;
		while (start + cnt <= len) {
			
			if (ra instanceof IRandCN) {
				CommentNetwork cn = new CommentNetwork();
				((IRandCN)ra).setCn(cn);
			}
			
			if (ra instanceof CommentNetwork) {
				ra = new CommentNetwork();
			}
			
			if (ra instanceof SVDRecommendationPR) {
				ra = new SVDRecommendationPR();
			}
			
			if (ra instanceof SVDRecommendationUser) {
				ra = new SVDRecommendationUser();
			}
			
			for (int i = start; i < start+cnt; i++) {
				List<String> reviewers = ra.recommend(i, this);
				try {
					if (reviewers == null || reviewers.size() == 0) 
						throw new RuntimeException("recommend nobody");
					List<String> trueReviewers = getCodeReviewers(i);
					if (trueReviewers == null || trueReviewers.size() == 0) 
						throw new RuntimeException("doestn't have reviewers");
					
	//				List<String> ranked = getRankedReviewers(i);
	//				rankMetric1 += evaluateRank(reviewers, ranked); 
	//				reviewers = rankOptimization(reviewers, i);
	//				rankMetric2 += evaluateRank(reviewers, ranked); 
					
//					System.out.println(reviewers.get(0));
//					System.out.println(trueReviewers);
					
					int cnt = 0;
					int flag = 0;
					for (int j = 0; j < 10; j++) {
						if (trueReviewers.contains(reviewers.get(j)))
							cnt ++;
						
						if (flag == 0 && cnt == 1) {
							mrr += (1.0 / (j+1.0));
							flag = 1;
						}
						
						precision[j] += ((cnt+0.0) / (j+1.0));
						recall[j] += ((cnt+0.0) / (trueReviewers.size()+0.0));
					}
	//				System.out.println(prList.get(i).getNumber() + " " + reviewers);
					
				} catch (RuntimeException e) {
					num --;
				}
				num ++;
			}
			start = start + cnt;
		}
			
		outputResult(num, precision, recall, mrr, rankMetric1, rankMetric2, methodNum);
	}
	
	private static void outputResult(int num, double[] precision, double[] recall, double mrr,
			double rankMetric1, double rankMetric2, int methodNum) {
		
//		for (int i = 0; i < 10; i++) {
//			lineOfFmeasure[i] = Double.toString(
//					(2*(precision[i]/num)*(recall[i]/num))/((precision[i]/num)+(recall[i]/num)));
//			
//			PrecisionRecall[i][column] = Double.toString(precision[i]/num);
//			PrecisionRecall[i][column+1] = Double.toString(recall[i]/num);
//			
//			rankMetric[i][column] = Double.toString(rankMetric1/num);
//			rankMetric[i][column+1] = Double.toString(rankMetric2/num);
//		}
//		lineOfMRR[methodNum] = Double.toString(mrr/(num+0.0));
		
		for (int i = 0; i < 10; i++) {
			System.out.println("topK : " + (i+1));
			System.out.println("total : " + num);
			System.out.println("Precision: " + precision[i]/num);
			System.out.println("Recall : " + recall[i]/num);
			System.out.println("rankMetric old: " + rankMetric1/num);
			System.out.println("rankMetric new: " + rankMetric2/num);
			System.out.println("F-Measure: " + 
					(2*(precision[i]/num)*(recall[i]/num))/((precision[i]/num)+(recall[i]/num)));
			System.out.println();
		}
		System.out.println("MRR: " + (mrr/(num+0.0)));
		System.out.println("----");
	}
	
	public static int column = 0;
	public static CSVWriter csvWriterOfFmeasure;
	public static CSVWriter csvWriterOfPrecisionRecall;
	public static CSVWriter csvWriterOfMRR;
	public static CSVWriter csvWriterOfRankMetric;
	
	public static String[] lineOfFmeasure = new String[10];
	public static String[][] PrecisionRecall = new String[10][2];
	public static String[] lineOfMRR = new String[1];
	public static String[][] rankMetric = new String[10][2];
	
	private static void writeFmeasure() {
		csvWriterOfFmeasure.writeNext(lineOfFmeasure);
		column = column + 2;
		
		try {
			csvWriterOfFmeasure.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
	}
	
	private static void writePrecisionRecallAndRankMetric() {
		List<String[]> tmp = new ArrayList<String[]>();
		for (int i = 0; i < PrecisionRecall.length; i++)
			tmp.add(PrecisionRecall[i]);
		csvWriterOfPrecisionRecall.writeAll(tmp);
		
		tmp = new ArrayList<String[]>();
		for (int i = 0; i < rankMetric.length; i++)
			tmp.add(rankMetric[i]);
		csvWriterOfRankMetric.writeAll(tmp);
		
		csvWriterOfMRR.writeNext(lineOfMRR);
		
		try {
			csvWriterOfPrecisionRecall.flush();
			csvWriterOfRankMetric.flush();
			csvWriterOfMRR.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
//		new ReReEntrance("saltstack", "salt", "windows").startEvalByFmeasure(new AnotherFPS());
//		new ReReEntrance("netty", "netty", "windows").startEvalByFmeasure(new TunedFPS());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo", "windows").startEvalByFmeasure(new Classifier());
		
		
		ReReEntrance tmp = new ReReEntrance("netty", "netty", "windows"); //.startEvalByFmeasure(new SVDRecommendationUser());
		
		tmp.startEvalByFmeasure(new SVDRecommendationUser());
		
		int cnt = 0;
		for (int i = 0; i < tmp.getPrList().size(); i ++) {
//			if (tmp.getCodeReviewers(i).contains("jasontedor")) {
//			}
			
			if (tmp.getPrList().get(i).getUser().getLogin().equals("tbrooks8")) {
				System.out.println("author: " + tmp.getPrList().get(i).getNumber());
			}
			else
				if (tmp.getCodeReviewers(i).contains("tbrooks8")) {
					System.out.println(tmp.getPrList().get(i).getNumber());
				}
		}
		System.out.println(cnt);
		
		
//		new ReReEntrance("netty", "netty", "windows").startEvalByFmeasure(new CommentNetwork());
//		new ReReEntrance("rails", "rails", "windows").startEvalByFmeasure(new FPS());
//		new ReReEntrance("rails", "rails", "windows").startEvalByFmeasure(new Activeness());
//		new ReReEntrance("etcd", "coreos", "windows").startEvalByFmeasure(new SVDRecommendationPR());
//		new ReReEntrance("etcd", "coreos", "windows").startEvalByFmeasure(new FPS());
//		new ReReEntrance("etcd", "coreos", "windows").startEvalByFmeasure(new Activeness());
//		new ReReEntrance("akka", "akka", "windows").startEvalByFmeasure(new SVDRecommendationPR());
//		new ReReEntrance("akka", "akka", "windows").startEvalByFmeasure(new FPS());
//		new ReReEntrance("akka", "akka", "windows").startEvalByFmeasure(new SVDRecommendationFile());
//		new ReReEntrance("netty", "netty", "windows").startEvalByFmeasure(new SVDMixer());
//		new ReReEntrance("jquery", "jquery", "windows").startEvalByFmeasure(new SVDRecommendationPR());
//		new ReReEntrance("akka", "akka", "windows").startEvalByFmeasure(new SVDRecommendationPR());
//		new ReReEntrance("akka", "akka", "w0indows").startEvalByFmeasure(new Activeness());
//		new ReReEntrance("node", "nodejs", "windows").startEvalByFmeasure(new FPS());
//		new ReReEntrance("akka", "akka", "windows").startEvalByFmeasure(new Activeness());
//		new ReReEntrance("netty", "netty", "windows").startEvalByFmeasure(new IRBasedRecommendation());
//		new ReReEntrance("netty", "netty", "windows").startEvalByFmeasure(new MixedRecommendation());
//		new ReReEntrance("netty", "netty").startEvalByFmeasure(new ChainMixer());
//		new ReReEntrance("angular.js", "angular", "windows").startEvalByFmeasure(new CommentNetwork());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo").startEvalByFmeasure(new TunedFPS());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo").startEvalByFmeasure(new FPS());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo").startEvalByFmeasure(new TIEComposer());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo").startEvalByFmeasure(new Classifier());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo").startEvalByFmeasure(new MixedRecommendation());
//		new ReReEntrance("zeroclickinfo-goodies", "duckduckgo", "windows").startEvalByFmeasure(new ChainMixer());
//		new ReReEntrance("jquery", "jquery", "windows").startEvalByFmeasure(new Classifier());
		
//		File file1 = new File("/home/xzl/Fmeasure.csv");  
//		BufferedWriter writer1 = new BufferedWriter(new FileWriter(file1));  
//        csvWriterOfFmeasure = new CSVWriter(writer1, ',');
//        
//        File file2 = new File("/home/xzl/PrecisionRecall.csv");  
//		BufferedWriter writer2 = new BufferedWriter(new FileWriter(file2));  
//		csvWriterOfPrecisionRecall = new CSVWriter(writer2, ',');
//		
//		File file3 = new File("/home/xzl/RankMetric.csv");  
//		BufferedWriter writer3 = new BufferedWriter(new FileWriter(file3));  
//		csvWriterOfRankMetric = new CSVWriter(writer3, ',');
//        
//        File file4 = new File("/home/xzl/MRR.csv");  
//		BufferedWriter writer4 = new BufferedWriter(new FileWriter(file4));  
//		csvWriterOfMRR = new CSVWriter(writer4, ',');
//        
//		
//		String[][] projects = {
//				{"angular.js","angular"},
////				{"zeroclickinfo-goodies","duckduckgo"},
////				{"jquery","jquery"},
//				{"netty","netty"},
////				{"rails","rails"},
//				{"salt","saltstack"},
//				{"akka","akka"},
////				{"manageiq","ManageIQ"},
////				{"docker","docker"},
////				{"rust","rust-lang"},                // rust
////				{"cocos2d-x", "cocos2d"},          //C++
////				{"atom","atom"},                   //CoffeScript
//				{"ipython", "ipython"},
//				{"zf2", "zendframework"},
//				{"scala", "scala"},
////				{"cakephp", "cakephp"},
////				{"bitcoin", "bitcoin"},
//				{"symfony", "symfony"},
////				{"gitlabhq", "gitlabhq"},
////				{"scikit-learn", "scikit-learn"},
////				{"node", "nodejs"},
////				{"etcd", "coreos"}
//		};
//		
//		int start = 0;
//		int end = projects.length;
//		
//		if (args.length == 2) {
//			start = Integer.parseInt(args[1]);
//			end = start + 1;
//		}
//		
//		for (int j = start; j < end; j++) {
//			String repo = projects[j][0];
//			String owner = projects[j][1];
//			
//			System.out.println(owner + " " + repo);
//			
//			ReReEntrance ent = new ReReEntrance(repo, owner, args[0]);
//			column  = 0;
//			
////			System.out.println(owner + "/" + repo);
////			Set<String> allReviewers = new HashSet<String>();
////			for (int i = 0; i < ent.getPrList().size(); i++)
////				allReviewers.addAll(ent.getCodeReviewers(i));
////			System.out.println(allReviewers.size());
////			
////			System.out.println(ent.getDp().getIssues().size());
////			System.out.println(ent.getPrList().size());
////			System.out.println(ent.getPrList().get(0).getCreatedAt());
////			System.out.println(ent.getPrList().get(ent.getPrList().size()-1).getCreatedAt());
////			System.out.println("*************");
////			
////			ent.startEvalByFmeasure(new FPS());
////			writeFmeasure();
////			
////			for (int i = 1; i <= 10; i++) {
////				ent.setK(i);
////				ent.startEvalByFmeasure(new Classifier());
////			}
////			writeFmeasureAndAccuracy();
////			
////			ent.startEvalByFmeasure(new TIEComposer());
////			writeFmeasure();
////			
////			ent.startEvalByFmeasure(new IRBasedRecommendation());
////			writeFmeasure();
//			
////			CommentNetwork cn = new CommentNetwork();
////			for (int i = 1; i <= 10; i++) {
////				ent.setK(i);
////				ent.startEvalByFmeasure(cn);
////			}
////			writeFmeasureAndAccuracy();
//			
////			ent.startEvalByFmeasure(new IRandCN());
////			writeFmeasure();
////			
////			ent.startEvalByFmeasure(new Activeness());
////			writeFmeasure();
////			
//			ent.startEvalByFmeasure(new SVDRecommendationPR());
//			writeFmeasure();
//			
////			ent.startEvalByFmeasure(new SVDRecommendationFile());
////			writeFmeasure();
//			
////			ent.startEvalByFmeasure(new CommentNetwork());
////			writeFmeasure();
////			
////			ent.startEvalByFmeasure(new SVDRecommendationUser());
////			writeFmeasure();
////			
////			ent.startEvalByFmeasure(new SVDMixer());
////			writeFmeasure();
//			
//			writePrecisionRecallAndRankMetric();
//			
//			csvWriterOfFmeasure.writeNext(new String[]{owner,"*","*"});
//			csvWriterOfPrecisionRecall.writeNext(new String[]{owner,"*","*"});
//			csvWriterOfRankMetric.writeNext(new String[]{owner,"*","*"});
//			csvWriterOfMRR.writeNext(new String[]{owner,"*","*"});
//			
//			System.out.println("*********************");
//		}
//		csvWriterOfFmeasure.close();
//		csvWriterOfPrecisionRecall.close();
//		csvWriterOfMRR.close();
//		csvWriterOfRankMetric.close();
	}
}
