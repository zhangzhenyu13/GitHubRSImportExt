package ReviewerRecommendation.Model;

import org.eclipse.egit.github.core.PullRequest;

public class NewPullRequest extends PullRequest{

	private static final long serialVersionUID = 3213500236266789938L;
	
	private String mergeCommitSha;

	public String getMergeCommitSha() {
		return mergeCommitSha;
	}

	public void setMergeCommitSha(String mergeCommitSha) {
		this.mergeCommitSha = mergeCommitSha;
	}
		
}
