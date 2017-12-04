package ReviewerRecommendation.Model;

import java.util.Date;

import org.eclipse.egit.github.core.User;

public class PullRequestComment {

	private String url;
	private int id;
	private String diffHunk;
	private String path;
	private int position;
	private int originalPositon;
	private String commitId;
	private String originalCommitId;
	private User user;
	private String body;
	private Date createdAt;
	private Date updatedAt;
	private String htmlUrl;
	private String pullRequestUrl;
	private String repo;
	private String owner;
	private String pullreqId;
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDiffHunk() {
		return diffHunk;
	}
	public void setDiffHunk(String diffHunk) {
		this.diffHunk = diffHunk;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public int getOriginalPositon() {
		return originalPositon;
	}
	public void setOriginalPositon(int originalPositon) {
		this.originalPositon = originalPositon;
	}
	public String getCommitId() {
		return commitId;
	}
	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}
	public String getOriginalCommitId() {
		return originalCommitId;
	}
	public void setOriginalCommitId(String originalCommitId) {
		this.originalCommitId = originalCommitId;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public Date getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public String getHtmlUrl() {
		return htmlUrl;
	}
	public void setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
	}
	public String getPullRequestUrl() {
		return pullRequestUrl;
	}
	public void setPullRequestUrl(String pullRequestUrl) {
		this.pullRequestUrl = pullRequestUrl;
	}
	public String getRepo() {
		return repo;
	}
	public void setRepo(String repo) {
		this.repo = repo;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getPullreqId() {
		return pullreqId;
	}
	public void setPullreqId(String pullreqId) {
		this.pullreqId = pullreqId;
	}
}
