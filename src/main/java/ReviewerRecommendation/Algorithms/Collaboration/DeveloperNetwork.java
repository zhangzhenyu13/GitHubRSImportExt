package ReviewerRecommendation.Algorithms.Collaboration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;

import ReviewerRecommendation.ReReEntrance;
import ReviewerRecommendation.Algorithms.RecommendAlgo;

public class DeveloperNetwork implements RecommendAlgo{

	private ReReEntrance entrance;
	private Map<String, Set<String>> collaborationNet = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> reviewNet = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> committerAuthorNet = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> commentNet = new HashMap<String, Set<String>>();
	
//	private Set<String> reviewers = new HashSet<String>();
//	private Set<String> committers = new HashSet<String>();
	
	private boolean isBuild = false;
	
	public DeveloperNetwork(ReReEntrance ent) {
		this.entrance = ent;
	}
	public DeveloperNetwork(){}
	
	private List<RepositoryCommit> extractCommitsByTime(Date begin, Date end) {
		Map<String, RepositoryCommit> commitsMap = entrance.getDp().getCommits();
		List<RepositoryCommit> commits = new ArrayList<RepositoryCommit>();
		for (Map.Entry<String, RepositoryCommit> each : commitsMap.entrySet())
			if (each.getValue().getCommit().getCommitter().getDate().before(end) &&
					each.getValue().getCommit().getCommitter().getDate().after(begin))
				commits.add(each.getValue());
		return commits;
	}
	
	private Map<String, List<String>> fileToDevelopers = 
			new HashMap<String, List<String>>();
	
	private void buildCollaborationNetwork(int cur) {
		Date now = entrance.getPrList().get(cur).getCreatedAt();
		
		List<RepositoryCommit> commits = extractCommitsByTime(new Date(1L), now);
		
		Map<String, List<RepositoryCommit>> fileToCommitsMap = 
				new HashMap<String, List<RepositoryCommit>>();
		
		for (RepositoryCommit cmt : commits) {
			List<CommitFile> commitFiles = cmt.getFiles();
			List<String> files = new ArrayList<String>();
			for (CommitFile each : commitFiles) files.add(each.getFilename());
			for (String each : files) 
				if (fileToCommitsMap.containsKey(each))
					fileToCommitsMap.get(each).add(cmt);
				else {
					List<RepositoryCommit> tmp = new ArrayList<RepositoryCommit>();
					tmp.add(cmt);
					fileToCommitsMap.put(each, tmp);
				}
		}
		
		for (Map.Entry<String, List<RepositoryCommit>> each : fileToCommitsMap.entrySet()) {
			List<RepositoryCommit> val = each.getValue();
			
			sortCommitsByTime(val);
			
			List<String> developers = new ArrayList<String>();
			if (val.size() > 1) {
				for (int i = 0; i < val.size(); i++)
					if (val.get(i).getAuthor() == null ||
							val.get(i).getAuthor().getLogin() == null) continue;
					else
						developers.add(val.get(i).getAuthor().getLogin());
				
				fileToDevelopers.put(each.getKey(), developers);
				
//				committers.addAll(developers);
				
				for (int i = 0; i < developers.size() - 1; i++)
					if (collaborationNet.containsKey(developers.get(i)))
						collaborationNet.get(developers.get(i)).
							addAll(developers.subList(i+1, developers.size()));
					else
						collaborationNet.put(developers.get(i), 
								new HashSet<String>(developers.subList(i+1, developers.size())));
			}
		}
	}

	private void buildReviewNetwork(int cur) {
		for (int i = 0; i < cur; i++) {
			updateReviewNetwork(i);
		}
	}
//	int committerAuthor = 0;
	private void buildCommitterAuthorNetwork(int cur) {
		Date end = entrance.getPrList().get(cur).getCreatedAt();
		Date begin = new Date(1L);
		List<RepositoryCommit> commits = extractCommitsByTime(begin, end);	
		enrichCommitterAutorNetwork(commits);
		
//		for (Map.Entry<String, Set<String>> each : committerAuthorNet.entrySet())
//			committerAuthor += each.getValue().size();
//		committerAuthor /= 2;
	}
	private void enrichCommitterAutorNetwork(List<RepositoryCommit> commits) {
		for (RepositoryCommit commit : commits) {
			if (commit.getAuthor() == null) continue;
			if (commit.getCommitter() == null) continue;
			String author = commit.getAuthor().getLogin();
			String committer = commit.getCommitter().getLogin();
			if (author == null || committer == null) continue;
	//			committers.add(author);
	//			committers.add(committer);
			
			if (!author.equals(committer)) {
	//				committerAuthor++;
				if (committerAuthorNet.containsKey(author))
					committerAuthorNet.get(author).add(committer);
				else {
					Set<String> tmp = new HashSet<String>();
					tmp.add(committer);
					committerAuthorNet.put(author, tmp);
				}
				if (committerAuthorNet.containsKey(committer))
					committerAuthorNet.get(committer).add(author);
				else {
					Set<String> tmp = new HashSet<String>();
					tmp.add(author);
					committerAuthorNet.put(committer, tmp);
				}
			}
		}
	}
	
	private void buildCommentNetwork(int cur) {
		int number = entrance.getPrList().get(cur).getNumber();
		enrichCommentNetwork(0, number);
	}
	private void enrichCommentNetwork(int start, int end) {
		Map<String, List<Comment>> issueComments = new HashMap<String, List<Comment>>();
		Map<String, Issue> issues = new HashMap<String, Issue>();
		
		/*****/
		Map<String, PullRequest> prMap = entrance.getDp().getPrs();
		Set<Integer> prNumbers = new HashSet<Integer>();
		for (Map.Entry<String, PullRequest> each : prMap.entrySet()) {
			prNumbers.add(each.getValue().getNumber());
		}
		/*****/
		
		for (Map.Entry<String, Issue> each : entrance.getDp().getIssues().entrySet())
			if (each.getValue().getNumber() < end &&
					each.getValue().getNumber() >= start) {
				
				/*****/
				if (prNumbers.contains(each.getValue().getNumber()))
					continue;
				/*****/
				
				issues.put(each.getKey(), each.getValue());
				issueComments.put(each.getKey(), 
						entrance.getDp().getIssueComments().get(each.getKey()));
			}
		
		for (Map.Entry<String, List<Comment>> each : issueComments.entrySet()) {
			String key = each.getKey();
			if (issues.get(key).getUser() == null ||
					issues.get(key).getUser().getLogin() == null) continue;
			String issuer = issues.get(key).getUser().getLogin();
			
			for (Comment com : each.getValue()) {
				if (com.getUser() == null ||
						com.getUser().getLogin() == null) continue;
				String user = com.getUser().getLogin();
				if (commentNet.containsKey(issuer))
					commentNet.get(issuer).add(user);
				else {
					Set<String> tmp = new HashSet<String>();
					tmp.add(user);
					commentNet.put(issuer, tmp);
				}
			}
		}
	}
	
	public void compareTwoNetwork() {
		int cur = entrance.getPrList().size() - 1;
		buildCollaborationNetwork(cur);
		buildReviewNetwork(cur);
		buildCommitterAuthorNetwork(cur);
		buildCommentNetwork(cur);
		
//		System.out.println("all reviewers: " + reviewers.size());
//		System.out.println("all committers: " + committers.size());
		
		int hit = 0;
		int loss = 0;
		int lack = 0;
		
		for (Map.Entry<String, Set<String>> each : reviewNet.entrySet()) {
			Set<String> val = each.getValue();
			for (String v : val) {
				if (	!collaborationNet.containsKey(each.getKey()))//  && !collaborationNet.containsKey(v))// &&
//						!committerAuthorNet.containsKey(each.getKey()) )//&&
//						!commentNet.containsKey(each.getKey()))
					lack ++;
				
				if ( 	(collaborationNet.containsKey(each.getKey()) && 
						collaborationNet.get(each.getKey()).contains(v)
						) //||
//						(
//							collaborationNet.containsKey(v) && 
//							collaborationNet.get(v).contains(each.getKey())	
//						)
//						(committerAuthorNet.containsKey(each.getKey()) && 
//								committerAuthorNet.get(each.getKey()).contains(v)
//						)// ||
//						(commentNet.containsKey(each.getKey()) && 
//								commentNet.get(each.getKey()).contains(v)
//						)
					)
					hit ++;
				else
					loss ++;
			}
		}
		
		int totalCollaboration = 0;
		for (Map.Entry<String, Set<String>> each : collaborationNet.entrySet())
			totalCollaboration += each.getValue().size();
		
		int totalCN = 0;
		for (Map.Entry<String, Set<String>> each : commentNet.entrySet())
			totalCN += each.getValue().size();
		
		System.out.println("hit : " + hit);
		System.out.println("loss : " + loss);
		System.out.println("lack : " + lack);
		System.out.println("Collaboration total edges : " + totalCollaboration);
		System.out.println("Comment Network total edges : " + totalCN);
//		System.out.println("committer-author : " + committerAuthor);
	}
	
	private void sortCommitsByTime(List<RepositoryCommit> commits) {
		Collections.sort(commits, new Comparator<RepositoryCommit>() {
			@Override
			public int compare(RepositoryCommit o1, RepositoryCommit o2) {
				Date d1 = o1.getCommit().getCommitter().getDate();
				Date d2 = o2.getCommit().getCommitter().getDate();
				return d1.compareTo(d2);
			}
		});
	}
	
	@Override
	public List<String> recommend(int i, ReReEntrance ent) {
		this.entrance = ent;
		if (ent.getCodeReviewers(i) == null || ent.getCodeReviewers(i).size() == 0)
			return null;
		
		if (!isBuild) {
			buildCollaborationNetwork(i);
			buildReviewNetwork(i);
			buildCommitterAuthorNetwork(i);
			buildCommentNetwork(i);
			isBuild = true;
		}
		Set<String> candidates = getRelevantDevelopers(i);
		if (candidates == null) {
			updateNetwork(i);
			return null;
		}
		
		if (ent.getPrList().get(i).getUser() != null) {
			String author = ent.getPrList().get(i).getUser().getLogin();
			candidates.remove(author);
		}
		
		updateNetwork(i);
		return new ArrayList<String>(candidates);
	}
	
	private void updateNetwork(int i) {
		/*
		 * update collaborationNet
		 */
		updateCollaborationNetwork(i);
		/*
		 * update reviewNet
		 */
		updateReviewNetwork(i);
		/*
		 * update committerAutorNet
		 */
		updateCommitterAuthorNetwork(i);
		/*
		 * update commentNet
		 */
		updateCommentNetwork(i);
	}
	private void updateCommentNetwork(int i) {
		int begin = entrance.getPrList().get(i).getNumber();
		int end;
		if ((i+1) < entrance.getPrList().size())
			end = entrance.getPrList().get(i+1).getNumber();
		else end = begin;
		enrichCommentNetwork(begin, end);
	}
	private void updateCommitterAuthorNetwork(int i) {
		Date begin = entrance.getPrList().get(i).getCreatedAt();
		Date end = null;
		if ((i+1) < entrance.getPrList().size())
			end = entrance.getPrList().get(i+1).getCreatedAt();
		else end = new Date(System.currentTimeMillis());
		List<RepositoryCommit> commits = extractCommitsByTime(begin, end);	
		enrichCommitterAutorNetwork(commits);
	}
	
	private void updateCollaborationNetwork(int i) {
		Date begin = entrance.getPrList().get(i).getCreatedAt();
		Date end = null;
		if ((i+1) < entrance.getPrList().size())
			end = entrance.getPrList().get(i+1).getCreatedAt();
		else end = new Date(System.currentTimeMillis());

		List<RepositoryCommit> commits = extractCommitsByTime(begin, end);
		sortCommitsByTime(commits);
		
		for (RepositoryCommit cmt : commits) {
			if (cmt.getAuthor() == null ||
					cmt.getAuthor().getLogin() == null) continue;
				
			String author = cmt.getAuthor().getLogin();
			
			List<CommitFile> commitFiles = cmt.getFiles();
			List<String> files = new ArrayList<String>();
			for (CommitFile each : commitFiles) files.add(each.getFilename());
			
			for (String each : files) {
				if (fileToDevelopers.containsKey(each)) {
					List<String> devs = fileToDevelopers.get(each);
					for (String dev : devs)
						if (collaborationNet.containsKey(dev))
							collaborationNet.get(dev).add(author);
						else
							collaborationNet.put(dev, 
									new HashSet<String>(Arrays.asList(author)));
					fileToDevelopers.get(each).add(author);
				}
				else {
					fileToDevelopers.put(each, 
							new ArrayList<String>(Arrays.asList(author)));
				}
			}
		}
	}
	
	private void updateReviewNetwork(int i) {
		if (entrance.getPrList().get(i).getUser() == null ||
				entrance.getPrList().get(i).getUser().getLogin() == null) return;
		
		String author = entrance.getPrList().get(i).getUser().getLogin();
		List<String> revs = entrance.getCodeReviewers(i);
		if (revs != null)
			if (reviewNet.containsKey(author))
				reviewNet.get(author).addAll(revs);
			else
				reviewNet.put(author, new HashSet<String>(revs));
	}
	
	private Set<String> getRelevantDevelopers(int i) {
		String user;
		if (entrance.getPrList().get(i).getUser() == null ||
				entrance.getPrList().get(i).getUser().getLogin() == null) 
			return null;
			
		user = entrance.getPrList().get(i).getUser().getLogin();
		Set<String> ret = new HashSet<String>();
		if (collaborationNet.containsKey(user))
			ret.addAll(collaborationNet.get(user));
		if (reviewNet.containsKey(user))
			ret.addAll(reviewNet.get(user));
		if (committerAuthorNet.containsKey(user))
			ret.addAll(committerAuthorNet.get(user));
		if (commentNet.containsKey(user))
			ret.addAll(commentNet.get(user));
		return ret;
	}

	public static void main(String[] args) {
		ReReEntrance ent = new ReReEntrance("netty", "netty", "windows");
		new DeveloperNetwork(ent).compareTwoNetwork();
	}
}
