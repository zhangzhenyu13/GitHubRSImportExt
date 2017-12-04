package ExtractReviewData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;

public class QueryNeo4j {

	private static final Driver driver = GraphDatabase.driver("bolt://192.168.7.124:7687",
			AuthTokens.basic("neo4j", "neo4j"));;
	
	public Driver acquireDriver(List<String> uris, AuthToken authToken,
			Config config) {
		for (String uri : uris) {
			try {
				return GraphDatabase.driver(uri, authToken, config);
			} catch (ServiceUnavailableException ex) {
				// This URI failed, so loop around again if we have another.
			}
		}
		throw new ServiceUnavailableException("No valid database URI found");
	}
	
	/*
	 * 设置project节点的url属性的索引
	 */
	public void setProjectUrlIndex() throws IOException {
		System.out.println("begin");
		try (Session session = driver.session()) {
				session.run("CREATE INDEX ON :Project(url)");
		}
		System.out.println("finished");
	}
	
	/*
	 * 设置project节点的isForked(1)属性
	 */
	public void setProjectIsForkedToOne() throws IOException {
		System.out.println("begin");
		try (Session session = driver.session()) {
				session.run("Match (a:Project) Where exists((a)-[:ForkFrom]->(:Project)) SET a.isForked = 1");
		}
		System.out.println("finished");
	}
	
	/*
	 * 设置project节点的isForked(0)属性
	 */
	public void setProjectIsForkedToZero() throws IOException {
		System.out.println("begin");
		try (Session session = driver.session()) {
				session.run("Match (a:Project) Where (NOT(exists(a.isForked))) SET a.isForked = 0");
		}
		System.out.println("finished");
	}
	
	/*
	 * 统计每个project的 pr数量，并将该值设为project的一个属性
	 */
	public void setProjectPRCount() throws IOException {
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(
				"/sdpdata2/xiazhenglin/prCnt"));

		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult result = tx
						.run("Match (a:Project {deleted:0})<-[r:BaseRepo]-(pr:PullRequest) "
								+ "Where (NOT (a:Project)-[:ForkFrom]->(:Project)) "
								+ "With a, count(r) AS prCount "
								+ "SET a.prCnt = prCount "
								+ "Return a.prCnt AS cnt, a.url AS url");
				while (result.hasNext()) {
					Record record = result.next();
					bWriter.write(record.get("cnt").asInt() + " "
							+ record.get("url").asString() + "\n");
				}
				tx.success();
			}
		}
		bWriter.close();
		System.out.println("finished");
	}

	/*
	 * 统计每个project的参与者的数目
	 */
	public void setProjectCollaboratorCount() throws IOException {
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(
				"/sdpdata2/xiazhenglin/collaboratorCnt"));
		System.out.println("begin");
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult result = tx
						.run("Match (a:Project {isForked:0})<-[r:MemberOf]-(b:User) " +
								"With a, count(r) AS num " +
								"SET a.collaboratorCnt = num " +
								"Return a.collaboratorCnt AS cnt, a.url AS url");
				
				while (result.hasNext()) {
					Record record = result.next();
					bWriter.write(record.get("cnt").asInt() + " "
							+ record.get("url").asString() + "\n");
				}
				tx.success();
			}
		}
		bWriter.close();
		System.out.println("finished");
	}
	
	/*
	 * 统计每个project的commit的数目
	 */
	public void setProjectCommitCount() throws IOException {
		BufferedWriter bWriter = new BufferedWriter(new FileWriter(
				"/sdpdata2/xiazhenglin/commitCnt"));
		System.out.println("begin");
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult result = tx
						.run("Match (a:Project {isForked:0})<-[r:BelongTo]-(b:Commit) " +
								"With a, count(r) AS num " +
								"SET a.commitCnt = num " +
								"Return a.commitCnt AS cnt, a.url AS url");
				
				while (result.hasNext()) {
					Record record = result.next();
					bWriter.write(record.get("cnt").asInt() + " "
							+ record.get("url").asString() + "\n");
				}
				tx.success();
			}
		}
		bWriter.close();
		System.out.println("finished");
	}
	
	public static void main(String[] args) throws IOException {
		new QueryNeo4j().setProjectCollaboratorCount();
		driver.close();
	}
}
