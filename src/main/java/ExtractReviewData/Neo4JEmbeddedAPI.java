package ExtractReviewData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

public class Neo4JEmbeddedAPI {
	
//	private static enum RelTypes implements RelationshipType
//	{
//	    KNOWS
//	}
	
	public void buildIndex() throws InterruptedException {
		File dbPath = new File("/sdpdata1/neo4j-enterprise-3.0.6/data/databases/graph.db");
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder( dbPath )
	    .loadPropertiesFromFile( "/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j.conf" )
	    .loadPropertiesFromFile("/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j-wrapper.conf")
	    .newGraphDatabase();
		registerShutdownHook( graphDb );
		
//		Node firstNode;
//		Node secondNode;
//		Relationship relationship;
//		
//		firstNode = graphDb.createNode();
//		firstNode.setProperty( "message", "Hello, " );
//		secondNode = graphDb.createNode();
//		secondNode.setProperty( "message", "World!" );
//
//		relationship = firstNode.createRelationshipTo( secondNode, RelTypes.KNOWS );
//		relationship.setProperty( "message", "brave Neo4j " );
//		
//		firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
//		firstNode.delete();
//		secondNode.delete();
		
		IndexDefinition indexDefinition = null;
		try ( Transaction tx = graphDb.beginTx() )
		{
			int flag = 0;
		    Schema schema = graphDb.schema();
		    Iterable<IndexDefinition> indexes = schema.getIndexes();
		    for (IndexDefinition id : indexes) {
		    	Iterable<String> strs = id.getPropertyKeys();
		    	Label la = id.getLabel();
		    	if (la.name().equals("Commit") && 
		    			strs.iterator().next().equals("sha")) {
		    		flag = 1;
		    		indexDefinition = id;
		    		break;
		    	}
		    }
		    
		    if (flag == 0)
		    	indexDefinition = schema.indexFor( Label.label( "Commit" ) )
				            .on( "sha" )
				            .create();
		    tx.success();
		}
		
		try ( Transaction tx = graphDb.beginTx() )
		{
		    Schema schema = graphDb.schema();
		    while (true) {
		    	Thread.sleep(3000);
		    	System.out.println( String.format( "Percent complete: %1.0f%%",
		            schema.getIndexPopulationProgress( indexDefinition )
		            	.getCompletedPercentage() ) );
		    	
		    }
		}
		
//		try ( Transaction tx = graphDb.beginTx() )
//		{
//		    Schema schema = graphDb.schema();
//		    schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
//		}
//		
//		graphDb.shutdown();
	}
	
	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running application).
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	    } );
	}
	
	public void addAttribute() {
		File dbPath = new File("/sdpdata1/neo4j-enterprise-3.0.6/data/databases/graph.db");
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder( dbPath )
	    .loadPropertiesFromFile( "/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j.conf" )
	    .loadPropertiesFromFile("/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j-wrapper.conf")
	    .newGraphDatabase();
		registerShutdownHook( graphDb );
		
		Label label = Label.label( "Project" );
		try ( Transaction tx = graphDb.beginTx();
		      ResourceIterator<Node> projects = graphDb.findNodes(label) )
		{
			int cnt = 0;
			int modified = 0;
		    while (projects.hasNext()) {
		    	cnt ++;
		    	Node pro = projects.next();
		    	if (!pro.hasProperty("isForked")) {
		    		pro.setProperty("isForked", 0);
		    		modified ++;
		    	}
		    	
		    	if (cnt % 2000 == 0) {
		    		System.out.println("cnt: " + cnt + " modified: " + modified);
		    	}
		    	
		    }
		    projects.close();
		    tx.success();
		}
		graphDb.shutdown();
	}
	
	public void hewei() {
		File dbPath = new File("/storage2/chenjf/neo4j-enterprise-2.3.1/data/graph.db");
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder( dbPath )
//	    .loadPropertiesFromFile( "/storage2/chenjf/neo4j-enterprise-2.3.1/conf/neo4j.conf" )
//	    .loadPropertiesFromFile("/storage2/chenjf/neo4j-enterprise-2.3.1/conf/neo4j-wrapper.conf")
	    .newGraphDatabase();
		registerShutdownHook( graphDb );
		
		try ( Transaction tx = graphDb.beginTx() ) {
			
			System.out.println("begin");
			Relationship rs = graphDb.getRelationshipById(13613085);
			if (rs != null) System.out.println(rs.toString());
			else System.out.println("这条边不存在");
			rs.delete();
			System.out.println("end");
			tx.success();
		}
		graphDb.shutdown();
	}
	
	public void addAttributeByTraversal() {
		File dbPath = new File("/sdpdata1/neo4j-enterprise-3.0.6/data/databases/graph.db");
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder( dbPath )
	    .loadPropertiesFromFile( "/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j.conf" )
	    .loadPropertiesFromFile("/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j-wrapper.conf")
	    .newGraphDatabase();
		registerShutdownHook( graphDb );
		
		Label label = Label.label( "Project" );
		try ( Transaction tx = graphDb.beginTx();
		      ResourceIterator<Node> projects = graphDb.findNodes(label, "isForked", 0);
			  BufferedWriter bWriter = new BufferedWriter(new FileWriter("/sdpdata2/xiazhenglin/prCnt", true)))
		{
			RelationshipType relation = null;
			for (RelationshipType rt : graphDb.getAllRelationshipTypes()) {
				if (rt.name().equals("BaseRepo")) {
					relation = rt;
					break;
				}
			}

			TraversalDescription td = graphDb.traversalDescription()
										.breadthFirst()
										.relationships(relation)
										.uniqueness(Uniqueness.NODE_PATH)
										.evaluator(Evaluators.fromDepth(1))
										.evaluator(Evaluators.toDepth(1));
			
			int page = 10000000;
			
			int metric = 0;
		    while (projects.hasNext()) {
		    	Node pro = projects.next();
		    	
		    	metric ++;
		    	if (metric % 2000 == 0)
		    		System.out.println("processed: " + metric);
		    	if (metric <= page) continue;
		    	
		    	int cnt = 0;
		    	Traverser traverser = td.traverse(pro);
		    	for (Path path : traverser) {
//		    		System.out.println(Paths.defaultPathToString(path));
		    		cnt++;
		    	}
		    	pro.setProperty("prCnt", cnt);
		    	bWriter.write(cnt + " " + pro.getProperty("url") + "\n");
		    	
//		    	metric ++;
//		    	if (metric % 2000 == 0)
//		    		System.out.println("processed: " + metric);
//		    	if (metric >= page) break;
		    }
		    
		    projects.close();
		    tx.success();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		graphDb.shutdown();
	}
	
	public void getCollaCnt() {
		File dbPath = new File("/sdpdata1/neo4j-enterprise-3.0.6/data/databases/graph.db");
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
	    .newEmbeddedDatabaseBuilder( dbPath )
	    .loadPropertiesFromFile( "/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j.conf" )
	    .loadPropertiesFromFile("/sdpdata1/neo4j-enterprise-3.0.6/conf/neo4j-wrapper.conf")
	    .newGraphDatabase();
		registerShutdownHook( graphDb );
		
		Label label = Label.label( "Project" );
		try ( Transaction tx = graphDb.beginTx();
		      ResourceIterator<Node> projects = graphDb.findNodes(label, "isForked", 0);
			  BufferedWriter bWriter = new BufferedWriter(new FileWriter("/sdpdata2/xiazhenglin/collaboratorCnt")))
		{
			
			int metric = 0;
		    while (projects.hasNext()) {
		    	Node pro = projects.next();
		    	bWriter.write(pro.getProperty("collaboratorCnt") + " " + pro.getProperty("url") + "\n");
		    	
		    	metric ++;
		    	if (metric % 2000 == 0)
		    		System.out.println("processed: " + metric);
		    }
		    
		    projects.close();
		    tx.success();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		graphDb.shutdown();
	}
	
	public static void main(String[] args) throws InterruptedException {
		new Neo4JEmbeddedAPI().buildIndex();
	}
}
