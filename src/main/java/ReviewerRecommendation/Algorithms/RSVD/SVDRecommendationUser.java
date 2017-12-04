package ReviewerRecommendation.Algorithms.RSVD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.SortMapElement;
import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.ReReEntrance.Pair;
import ReviewerRecommendation.Algorithms.RecommendAlgo;

public class SVDRecommendationUser implements RecommendAlgo{
	
	private boolean isBuild = false;
	private List<String> reviewerList;
	private RatingMatrix mat;
	
//	private BasicSVD svd;
//	private Blender blender;
//	private NeighborhoodModel neigh;
//	private SVDModel svdModel;
	private BlenderWithOutImplicitFeedBack blenderW;
	
	private Map<String, Double> reviewerScores;
	
	public Map<String, Double> getReviewerScores() {
		return reviewerScores;
	}

	@Override
	public List<String> recommend(int cur, ReReEntrance ent) {
		if (!isBuild) {
			isBuild = true;
			mat = constructMatrix(cur, ent);
			
			double lamdaValue = 0.18, stepValue = 0.0002;
			int kValue = 5;
			blenderW = new BlenderWithOutImplicitFeedBack(ent);
			blenderW.setK(kValue);
			blenderW.setLamda(lamdaValue);
			
			Trainer trainer = new Trainer(mat, null);
			trainer.setStep(stepValue);
			trainer.setModel(blenderW);
			trainer.setMat(mat);
			trainer.setTimes(200);
			trainer.setRatio(0.0001);
			trainer.train();
			
//			CrossValidation crossVal = new CrossValidation(mat, ent);
//			blender = (Blender)crossVal.validate("Blender");
//			blenderW = (BlenderWithOutImplicitFeedBack)crossVal.validate("BlenderW");
//			neigh = (NeighborhoodModel)crossVal.validate("Neigh");
//			svdModel = (SVDModel)crossVal.validate("SVDModel");
		}
		
		List<String> exp = new ArrayList<String>();
		
		reviewerScores = new HashMap<String, Double>();
		
		if (ent.getPrList().get(cur).getUser() == null) return null;
		String author = ent.getPrList().get(cur).getUser().getLogin();
		if (!reviewerList.contains(author)) return null;
		
		int row = reviewerList.indexOf(author);
		for (int i = 0; i < reviewerList.size(); i ++) {
			if (reviewerList.get(i).equals(author)) continue;
			if (mat.isExist(row, i))
				reviewerScores.put(reviewerList.get(i), mat.getVal(row, i));
			else {		
				if (ent.getCodeReviewers(cur).contains(reviewerList.get(i))) {
					exp.add(reviewerList.get(i));					
				}
				reviewerScores.put(reviewerList.get(i), blenderW.predictVal(row, i));
			}
		}
		
		List<Map.Entry<String, Double>> entryList = SortMapElement.sortDouble(reviewerScores);
		
		
		for (int i = 0; i < entryList.size(); i++) {
			if (exp.contains(entryList.get(i).getKey())) {
				if (i < 10) {
					System.out.println(i+1);
					System.out.println(entryList.get(i).getValue());
					System.out.println(ent.getPrList().get(cur).getNumber());
					System.out.println(entryList.get(i).getKey());
					System.out.println(ent.getPrList().get(cur).getUser().getLogin());
					System.out.println("**********");
				}
			}
		}
		
		
		
		int upper = ent.k;
		List<String> ret = new ArrayList<String>();
		for (int j = 0 ; j < upper; j++)
			if (j < entryList.size()) {
				if (!author.equals(entryList.get(j).getKey()))
					ret.add(entryList.get(j).getKey());
				else
					upper ++;
			}
			else
				break;
		
		return ret;
	}
	
	private RatingMatrix constructMatrix(int cur, ReReEntrance ent) {
		int beginIndex = ent.temporalLocality(cur);
		beginIndex = 0;

		Map<String, Map<Integer, List<String>>> ds = 
				new HashMap<String, Map<Integer, List<String>>>();
		Set<String> allReviewers = new HashSet<String>();
		
		for (int j = beginIndex; j < cur; j++) {
			List<String> revs = ent.getCodeReviewers(j);
			allReviewers.addAll(revs);
			
			if (ent.getPrList().get(j).getUser() != null) {
				String author = ent.getPrList().get(j).getUser().getLogin();
				allReviewers.add(author);
				
				if (!ds.containsKey(author)) {
					Map<Integer, List<String>> inner = new HashMap<Integer, List<String>>();
					inner.put(j, revs);
					ds.put(author, inner);
				}
				else
					ds.get(author).put(j, revs);
			}
		}
		
		reviewerList = new ArrayList<String>(allReviewers);
		
		RatingMatrix retMat = new RatingMatrix(reviewerList, reviewerList);
		
		double endTime = ent.getPrList().get(cur).getCreatedAt().getTime() + 0.0;
		double startTime = ent.getPrList().get(beginIndex).getCreatedAt().getTime()+0.0 - 1000.0*3600.0;
		
		for (Map.Entry<String, Map<Integer, List<String>>> each : ds.entrySet()) {
			String row = each.getKey();
			int rowIndex = reviewerList.indexOf(row);
			for (Map.Entry<Integer, List<String>> pr : each.getValue().entrySet()) {
				List<String> cols = pr.getValue();
				int prid = pr.getKey();
//				double preTime = ent.getPrList().get(prid).getCreatedAt().getTime()+0.0;
				Map<String, List<Pair>> tt = ent.getReviewerContribution(prid);
				
				for (String col : cols) {
					
					List<Pair> pairs = tt.get(col);
					Collections.sort(pairs, new Comparator<Pair>() {
						@Override
						public int compare(Pair o1, Pair o2) {
							return o2.getDate().compareTo(o1.getDate());
						}
					});
					
					
					double val = 0.0;
					double lamda = 1.0;
					for (Pair p : pairs) {
						double commentTime = p.getDate().getTime()+0.0;
						val += lamda * ((commentTime - startTime)/(endTime - startTime));
						lamda *= 0.8;
					}
					
					int colIndex = reviewerList.indexOf(col);
					retMat.setVal(rowIndex, colIndex, 
							retMat.getVal(rowIndex, colIndex) + val);
				}
			}
		}
		
		System.out.println("reviewers: " + retMat.getX());
		
		return retMat;
	}
}
