package ExtractReviewData;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

public class ProjectRetriever {

	private static MongoClient mongoClient = null;
	private static GitHubClient ghClient = null;
	private static Driver driver = null;
			
	/*
	 * The three methods below are for FPS
	 */
	public void setGHClient() {
		ghClient = new GitHubClient()
		.setOAuth2Token("");
	}
	public GitHubClient getGHClient() {
		return ghClient;
	}
	public void initialMongo() {
		MongoClientURI connectionString = new MongoClientURI(
				"mongodb://192.168.7.113:30000,192.168.7.114:30000/?authSource=github");
		mongoClient = new MongoClient(connectionString);
	}
	
	public void initial() {
		MongoClientURI connectionString = new MongoClientURI(
//				"mongodb://ghtorrentro:ghtorrentro@127.0.0.1:27017/?authSource=github");
				"mongodb://192.168.7.113:30000,192.168.7.114:30000/?authSource=github");
		mongoClient = new MongoClient(connectionString);
		
		driver = GraphDatabase.driver("bolt://192.168.7.124:7687",
										AuthTokens.basic("neo4j", "buaaxzl"));
		ghClient = new GitHubClient()
				.setOAuth2Token("");  //buaaxzl
//				.setOAuth2Token("");  //actxzl
//				.setOAuth2Token("");  //xzlbuaa
	}
	
	private int t = 0;
	private void updateToken() {
		String[] tokens = {"",
				"",
				""};
		t = (t+1) % tokens.length;
		ghClient.setOAuth2Token(tokens[t]);
	}
	
	public MongoCollection<Document> getCollection(String collection) {
		return mongoClient.getDatabase("github").getCollection(collection);
	}
	
	public void close() {
		if (mongoClient != null) 
			mongoClient.close();
		driver.close();
	}
	
	public void retrieveProject(String project) throws IOException {
		String[] tokens = project.split("/");
		String name = tokens[tokens.length-1];
		String ownerLogin = tokens[tokens.length-2];
		
		File file = new File("/home/xzl/"
								+ ownerLogin + "-" + name);
		if (!file.exists()) file.mkdir();
		
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + "/repos.json")),"utf-8"))) {
			
			MongoCollection<Document> repos = getCollection("repos");
			Document proDoc = repos.find(and(eq("name", name), 
											eq("owner.login", ownerLogin))).first();
			if (proDoc == null) {
				System.out.println(ownerLogin + " " + name + " not find");
				return;
			}
			else {
				bWriter.write(proDoc.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
		
		System.out.println("Retrieve Commit and commments");
		retrieveCommitsAndComments(name, ownerLogin);
		
		System.out.println("Retrieve issue and comments");
		retrieveIssuesAndCommentsAndEvents(name, ownerLogin);
		
		System.out.println("Retrieve pull request and comments");
		retrievePRAndPRComments(name, ownerLogin);
		
		System.out.println("finished");
	}

	private void retrieveIssuesAndCommentsAndEvents(String name, String ownerLogin) throws IOException {
		
		ArrayList<Integer> issueNumber = new ArrayList<Integer>();
		/*
		 * Retrieve issues
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + "/issues.json")),"utf-8"))) {
			
			MongoCollection<Document> issues = getCollection("issues");
				
			MongoCursor<Document> cursor = issues.find(and(eq("repo", name),
												eq("owner", ownerLogin))).iterator();
			int cnt = 0;
			Document total = new Document();
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				if (doc != null) {
					issueNumber.add(doc.getInteger("number"));
					total.append(doc.getInteger("number") + "", doc);
					System.out.println("Get " + (++cnt) + " issues");
				}
			}
		
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
		
		/*
		 * Retrieve issue comments
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + "/issue_comments.json")),"utf-8"))) {
			
			MongoCollection<Document> issueComments = getCollection("issue_comments");
			Document total = new Document();
			for (Integer num : issueNumber) {
				MongoCursor<Document> cursor = issueComments.find(and(eq("repo", name),
												eq("owner", ownerLogin),
												eq("issue_id", num))).iterator();
				System.out.println("Get issue " + num + " comments");
				int cnt = 0;
				Document tmp = new Document();
				while (cursor.hasNext()) {
					Document doc = cursor.next();
					if (doc != null) {
						tmp.append(doc.getInteger("id").toString(), doc);
						System.out.println("Get " + (++cnt) + " issue comments");
					}
				}
				if (!tmp.isEmpty())	total.append(num+"", tmp);
				cursor.close();
			}
			
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
		
		/*
		 * Retrieve issue events
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + "/issue_events.json")),"utf-8"))) {
			MongoCollection<Document> issueEvents = getCollection("issue_events");
			Document total = new Document();
			for (Integer num : issueNumber) {
				MongoCursor<Document> cursor = issueEvents.find(and(eq("repo", name),
																	eq("owner", ownerLogin),
																	eq("issue_id", num))).iterator();
				System.out.println("Get issue " + num + " events");
				int cnt = 0;
				Document tmp = new Document();
				while (cursor.hasNext()) {
					Document doc = cursor.next();
					if (doc != null) {
						tmp.append(doc.getInteger("id")+"", doc);
						System.out.println("Get " + (++cnt) + " issue events");
					}
				}
				if (!tmp.isEmpty())	total.append(num+"", tmp);
				cursor.close();
			}
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
	}
	
	
	private void retrievePRAndPRComments(String name, String ownerLogin) throws IOException {
		ArrayList<Integer> nums = new ArrayList<Integer>();
		
		/*
		 * Retrieve pull_requests data 
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + "/prs.json")),"utf-8"))) {
			Document total = new Document();
			MongoCollection<Document> prs = getCollection("pull_requests");
			MongoCursor<Document> cursor = prs.find(and(eq("repo", name),
														eq("owner", ownerLogin)))
														.iterator();
			int cnt = 0;
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				if (doc != null) {
					nums.add(doc.getInteger("number"));
					total.append(doc.getInteger("number")+"", doc);
					System.out.println("Get " + (++cnt) + " pull request");
				}
			}
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
		
		/*
		 * Retrieve pull_request's comments
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + 
										"/pull_request_comments.json")),"utf-8"))) {
			
			Document total = new Document();
			MongoCollection<Document> PRComment = getCollection("pull_request_comments");
			for (Integer num : nums) {
				MongoCursor<Document> cursor = PRComment.find(and(eq("repo",name)
																, eq("owner",ownerLogin)
																, eq("pullreq_id",num))).iterator();
				System.out.println("Get pull request " + num +" comments");
				int cnt = 0;
				Document tmp = new Document();
				while (cursor.hasNext()) {
					Document doc = cursor.next();
					if (doc != null) {
						tmp.append(doc.getInteger("id")+"", doc);
						System.out.println("Get " + (++cnt) + " pull request comments");
					}
				}
				if (!tmp.isEmpty()) total.append(num+"", tmp);
				cursor.close();
			}
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
	}

	private void retrieveCommitsAndComments(String name, String ownerLogin) throws IOException {
		
		ArrayList<String> shas = new ArrayList<String>();
		
		/*
		 * Query Neo4j to find the relevant commits
		 */
		String url = "https://api.github.com/repos/" + ownerLogin + "/" + name;
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult result = tx
						.run("Match (a:Project {url:\"" + url + "\"})"
								+ " With a" 
								+ " Match (a)<-[:BelongTo]-(cmt:Commit)"
								+ " Return cmt.sha AS sha");
				while (result.hasNext()) {
					Record record = result.next();
					shas.add(record.get("sha").asString());
				}
				tx.success();
			}
		}
		
		System.out.println("There is "+shas.size()+" commits");
		
		/*
		 * Retrieve commit data
		 */
		
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
									+ ownerLogin + "-" + name + "/commits.json")),"utf-8"))) {
			Document total = new Document();
			MongoCollection<Document> commits = getCollection("commits");
			int cnt = 0;
			for (String sha : shas) {
				Document doc = commits.find(eq("sha", sha)).first();
				if (doc != null) {
					total.append(sha, doc);
					System.out.println("Get " + (++cnt) + " " + sha);
				}
				else {
					System.out.println("*****using GitHub API*******Get " + (++cnt) + " " + sha);
					RepositoryCommit commit = null;
					try {
						commit = getCommitData(name, ownerLogin, sha);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					if (commit != null)
						doc = Document.parse(JSON.toJSONString(commit, 
												SerializerFeature.UseISO8601DateFormat));
					if (doc == null)
						System.out.println("-------GitHub API Get Commit failed!");
					else {
						total.append(sha, doc);
						commits.insertOne(doc);
					}
				}
			}
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
		
//		/*
//		 * Retrieve commit data
//		 */
//		try (BufferedWriter bWriter = new BufferedWriter(
//				new OutputStreamWriter(
//						new FileOutputStream(
//								new File("/home/xzl/"
//										+ ownerLogin + "-" + name + "/commits.json")),"utf-8"))) {
//			Document total = new Document();
//			MongoCollection<Document> commits = getCollection("commits");
//			
//			List<RepositoryCommit> cmits = getAllCommitData(name, ownerLogin);
//			if (cmits == null) {
//				System.out.println("****GitHub*** get commit failed");
//				throw new RuntimeException();
//			}
//			else System.out.println("GET " + cmits.size() + " commits");
//			
//			for (RepositoryCommit cmt : cmits) {
//				Document doc = Document.parse(JSON.toJSONString(cmt));
//				try {
//					commits.insertOne(doc);
//				}catch (Exception e) {
//					e.printStackTrace();
//				}
//				total.append(cmt.getSha(), doc);
//				shas.add(cmt.getSha());
//			}
//			
//			if (!total.isEmpty()) {
//				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
//				bWriter.flush();
//			}
//		}
		
		/*
		 * Retrieve commit comments data
		 */
		try (BufferedWriter bWriter = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(
								new File("/home/xzl/"
										+ ownerLogin + "-" + name + 
										"/commit_comments.json")),"utf-8"))) {
			
			Document total = new Document();
			MongoCollection<Document> CCCollecton = getCollection("commit_comments");
			for (String each : shas) {
				MongoCursor<Document> cursor = CCCollecton.find(
												eq("commit_id",each)).iterator();
				System.out.println("Get SHA " + each +" comments:");
				int cnt = 0;
				Document tmp = new Document();
				while (cursor.hasNext()) {
					Document doc = cursor.next();
					if (doc != null) {
						tmp.append(doc.getInteger("id")+"", doc);
						System.out.println("Get " + (++cnt) + "commit comments");
					}
				}
				if (!tmp.isEmpty()) total.append(each, tmp);
				else System.out.println("sha do not has any Comments or sha is too fresh");
				cursor.close();
			}
			if (!total.isEmpty()) {
				bWriter.write(total.toJson(new JsonWriterSettings(JsonMode.STRICT)));
				bWriter.flush();
			}
		}
	}

	public RepositoryCommit getCommitData (String name, String ownerLogin, String sha) throws IOException {
		RepositoryId ri = new RepositoryId(ownerLogin, name);
		
		if (ghClient.getRemainingRequests() <= 1 && 
				ghClient.getRemainingRequests() != -1) {
			System.out.println("Limit : " + ghClient.getRequestLimit());
			updateToken();
		}
		CommitService cs = new CommitService(ghClient);
		return cs.getCommit(ri, sha);
	}
	
	private void getProjects(String user, String project) throws IOException {
		initial();
		
//		try (BufferedReader bReader = new BufferedReader(
//				new FileReader("/home/xzl/projects_1"))) {
//			String line = null;
//			while ((line = bReader.readLine()) != null ) {
//				String[] words = line.split(" ");
//				String url = words[words.length - 1];
//				if (url != null)
//					retrieveProject(url);
//			}
//		}
		retrieveProject("https://api.github.com/repos/" + user + "/" + project);
		close();
	}
	public static void main(String[] args) throws IOException {
		new ProjectRetriever().getProjects(args[0], args[1]);
		
		/*
		 * Test GitHub API Library
		 */
//		ghClient = new GitHubClient().setOAuth2Token("fa6791f29adceeca7f2031b30032d5bcaf673ad2");
//		RepositoryService rs = new RepositoryService(ghClient);
//		List<Repository> repos = rs.getRepositories();
//		System.out.println(repos.get(0).getGitUrl());
//		System.out.println(repos.size());
//		System.out.println(ghClient.getRequestLimit());
//		System.out.println(ghClient.getRemainingRequests());
	}
}
