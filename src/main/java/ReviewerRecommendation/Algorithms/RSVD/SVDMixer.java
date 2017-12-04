package ReviewerRecommendation.Algorithms.RSVD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.SortMapElement;
import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.Algorithms.RecommendAlgo;

public class SVDMixer implements RecommendAlgo{

	private SVDRecommendationPR prRec = new SVDRecommendationPR();
	private SVDRecommendationUser userRec = new SVDRecommendationUser();
	
	@Override
	public List<String> recommend(int i, ReReEntrance ent) {
		
		List<String> rec2 = prRec.recommend(i, ent);
		List<String> rec1 = userRec.recommend(i, ent);
		
		if (rec1 == null || rec1.size() == 0) return rec2;
		if (rec2 == null || rec2.size() == 0) return rec1;
		
		Map<String, Double> score2 = prRec.getReviewerScores();
		Map<String, Double> score1 = userRec.getReviewerScores();
		
		Set<String> allReviewers = new HashSet<String>();
		
		double totalPR = 0.0;
		for (Map.Entry<String, Double> each : score2.entrySet()) {
			totalPR += each.getValue();
			allReviewers.add(each.getKey());
		}
		
		double totalUser = 0.0;
		for (Map.Entry<String, Double> each : score1.entrySet()) {
			totalUser += each.getValue();
			allReviewers.add(each.getKey());
		}
		
		Map<String, Double> score = new HashMap<String, Double>();
		for (String rev : allReviewers) {
			double a, b;
			if (score2.containsKey(rev)) a = score2.get(rev);
			else a = 0.0;
			if (score1.containsKey(rev)) b = score1.get(rev);
			else b = 0.0;
			
			double tmp = 0.0;
			
			if (totalPR > 0.0)
				tmp = 0.5 * a / totalPR;
			if (totalUser > 0.0)
				tmp += 0.5 * b / totalUser;
			
			score.put(rev, tmp);
		}
		
		List<Map.Entry<String, Double>> entryList = SortMapElement.sortDouble(score);
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
