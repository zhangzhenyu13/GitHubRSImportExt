package ReviewerRecommendation.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tools.SortMapElement;
import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.Algorithms.Collaboration.DeveloperNetwork;

public class ChainMixer implements RecommendAlgo{
	
	private final MixedRecommendation mixed = new MixedRecommendation(); 
	private final DeveloperNetwork dn = new DeveloperNetwork();
	
	@Override
	public List<String> recommend(int i, ReReEntrance ent) {
		if (ent.getCodeReviewers(i) == null || ent.getCodeReviewers(i).size() == 0)
			return null;
		
		List<String> candidates = dn.recommend(i, ent);
//		System.out.println(candidates.size());
		
		
		List<String> result = mixed.recommend(i, ent);
		Map<String, Integer> score = mixed.getScore();
		List<Map.Entry<String, Integer>> entryList = SortMapElement.sortInteger(score);
		
		String author = ent.getPrList().get(i).getUser().getLogin();
		List<String> ret = new ArrayList<String>();
		
		int upper = ent.k;
		if (candidates.size() <= ent.k) {
			ret = result;
		}
		else {
			for (int j = 0 ; j < upper; j++)
				if (j < entryList.size()) {
					if (candidates.contains(entryList.get(j).getKey()) &&
							!author.equals(entryList.get(j).getKey()))
						ret.add(entryList.get(j).getKey());
					else
						upper ++;
				}
				else
					break;
		}
		
		
		if (ret.size() < ent.k) {
			int miss = ent.k - ret.size();
			for (Map.Entry<String, Integer> each : entryList) {
				if (!ret.contains(each.getKey())) {
					ret.add(each.getKey());
					miss --;
				}
				else continue;
				if (miss == 0) break;
			}
		}
		return ret;
	}
}
