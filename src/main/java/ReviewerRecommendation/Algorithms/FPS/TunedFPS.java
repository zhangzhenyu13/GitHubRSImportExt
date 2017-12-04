package ReviewerRecommendation.Algorithms.FPS;

import java.util.List;
import java.util.Map;

import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.ReReEntrance.Pair;

public class TunedFPS extends FPS {
	
	public Double calculate(String path, List<String> paths, FilePathComparator fpc) {
		double score = 0.0;
		for (String p : paths) {
			if (fpc.similar(path, p) > score)
				score = fpc.similar(path, p);
		}
//		score = score / paths.size();
		return score;
	}
	
	public Double calculate(List<String> filesn, List<String> filesp, 
											FilePathComparator fpc) {
		double score = 0.0;
		for (String a : filesn)
			for (String b : filesp)
				score = score + fpc.similar(a, b);

		score = score / (filesn.size() * filesp.size());
		return score;
	}
	
	
	@Override
	public void reviewerExpertise(int i, List<String> filesn, ReReEntrance ent, 
			FilePathComparator fpc) {
		
//		List<String> filesp = getFiles(i);
//		
//		double scorep = 0.0;
//		for (String a : filesn)
//			for (String b : filesp)
//				scorep = scorep + fpc.similar(a, b);
//
//		scorep = scorep / (filesn.size() * filesp.size());
//		
//		List<String> revs = ent.getCodeReviewers(i);
//		for (String reviewer : revs) {
//			if (reviewers.containsKey(reviewer))
//				reviewers.put(reviewer, reviewers.get(reviewer)+scorep);
//			else
//				reviewers.put(reviewer, scorep);
//		}
		
//***********************************************************************	
		List<String> filesp = getFiles(i);
		Map<String, List<Pair>> contribution = ent.getReviewerContribution(i);
		double scorep = 0.0;
		for (String a : filesn)
			for (String b : filesp)
				scorep = scorep + fpc.similar(a, b);

		scorep = scorep / (filesn.size() * filesp.size());
		
		List<String> revs = ent.getCodeReviewers(i);
		for (String reviewer : revs) {
			int cont = contribution.get(reviewer).size();
			
			if (reviewers.containsKey(reviewer))
				reviewers.put(reviewer, reviewers.get(reviewer)+scorep*cont);
			else
				reviewers.put(reviewer, scorep*cont);
		}
		
//*********************************************************************************		
//		
//		List<String> filesp = getFiles(i);
//		Map<String, Integer> contribution = getReviewerContribution(i);
//		double scorep = calculate(filesn, filesp, fpc);
//		double factor = 0;
//		
//		List<Pair> pairs = IssueCommentContribution.get(i);
//		if (pairs != null)
//			for (Pair pair : pairs) {
//				String reviewer = pair.getReviewer();
//				if (reviewers.containsKey(reviewer))
//					reviewers.put(reviewer, reviewers.get(reviewer) + 
//							factor*scorep);
//				else
//					reviewers.put(reviewer, factor*scorep);
//			}
//		
//		List<Pair> ps = CommitCommentContribution.get(i);
//		if (ps != null)
//			for (Pair pair : ps) {
//				String reviewer = pair.getReviewer();
//				Double score = calculate(pair.getPath(), filesn, fpc);
////				score += scorep;
//				if (reviewers.containsKey(reviewer))
//					reviewers.put(reviewer, reviewers.get(reviewer) + 
//							   		(1-factor)*score);
//				else
//					reviewers.put(reviewer, (1-factor)*score);
//			}
	}
}
