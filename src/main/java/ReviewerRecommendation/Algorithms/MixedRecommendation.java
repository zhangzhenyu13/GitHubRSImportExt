package ReviewerRecommendation.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.SortMapElement;
import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.Algorithms.FPS.TunedFPS;
import ReviewerRecommendation.Algorithms.NBClassification.Classifier;

public class MixedRecommendation implements RecommendAlgo{

	private final Classifier nb = new Classifier();
	private final TunedFPS fps = new TunedFPS();
	private Map<String, Integer> score;

	public Map<String, Integer> getScore() {
		return score;
	}

	@Override
	public List<String> recommend(int i, ReReEntrance ent) {
		if (ent.getCodeReviewers(i) == null || ent.getCodeReviewers(i).size() == 0)
			return null;
		
		List<String> result = nb.recommend(i, ent);
		fps.recommend(i, ent);
		
		Map<String, Double> confScore = nb.getConfScore();
		Map<String, Double> reviewers = fps.getReviewers();
		
		if (reviewers.size() == 0)
			return result;
		
		Set<String> allReviewers = new HashSet<String>();
		allReviewers.addAll(confScore.keySet());
		allReviewers.addAll(reviewers.keySet());
		
		List<String> sortList1 = new ArrayList<String>();
		List<String> sortList2 = new ArrayList<String>();
		
		List<Map.Entry<String, Double>> entryList1;
		List<Map.Entry<String, Double>> entryList2;
		entryList1 = SortMapElement.sortDouble(reviewers);
		entryList2 = SortMapElement.sortDouble(confScore);
		
		for (Map.Entry<String, Double> each : entryList1) sortList1.add(each.getKey());
		for (Map.Entry<String, Double> each : entryList2) sortList2.add(each.getKey());
		
		score = new HashMap<String, Integer>();
		for (String rev : allReviewers) {
			Integer a, b;
			if (sortList1.contains(rev)) a = sortList1.indexOf(rev);
			else a = 10;
			if (sortList2.contains(rev)) b = sortList2.indexOf(rev);
			else b = 10;
			
			score.put(rev, a+b);
		}
		List<Map.Entry<String, Integer>> entryList = SortMapElement.sortInteger(score);
		
//		Map<String, Double> score = new HashMap<String, Double>();
//		for (String rev : allReviewers) {
//			Integer a, b;
//			if (sortList1.contains(rev)) a = sortList1.indexOf(rev);
//			else a = 10;
//			if (sortList2.contains(rev)) b = sortList2.indexOf(rev);
//			else b = 10;
//			
//			score.put(rev, (a>b?b:a) + Math.log(Math.abs(a-b)) );
//		}
//		List<Map.Entry<String, Double>> entryList = SortMapElement.sortDoubleFromSmallToLarge(score);
		
		
		String author = ent.getPrList().get(i).getUser().getLogin();
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

}
