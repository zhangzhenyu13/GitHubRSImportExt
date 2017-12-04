package Spider;

import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import tools.FileFilter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

class File {
	private String sha;
	private String filename;
	private String status;
	private String additions;
	private String deletions;
	private String changes;
	private String blob_url;
	private String raw_url;
	private String contents_url;
	private String patch;

	public String getSha() {
		return sha;
	}

	public void setSha(String sha) {
		this.sha = sha;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAdditions() {
		return additions;
	}

	public void setAdditions(String additions) {
		this.additions = additions;
	}

	public String getDeletions() {
		return deletions;
	}

	public void setDeletions(String deletions) {
		this.deletions = deletions;
	}

	public String getChanges() {
		return changes;
	}

	public void setChanges(String changes) {
		this.changes = changes;
	}

	public String getBlob_url() {
		return blob_url;
	}

	public void setBlob_url(String blob_url) {
		this.blob_url = blob_url;
	}

	public String getRaw_url() {
		return raw_url;
	}

	public void setRaw_url(String raw_url) {
		this.raw_url = raw_url;
	}

	public String getContents_url() {
		return contents_url;
	}

	public void setContents_url(String contents_url) {
		this.contents_url = contents_url;
	}

	public String getPatch() {
		return patch;
	}

	public void setPatch(String patch) {
		this.patch = patch;
	}
}

class Files {
	private List<File> files = new ArrayList<File>();

	public List<File> getFiles() {
		return files;
	}

	public void setFiles(List<File> files) {
		this.files = files;
	}

}

public class DownloadTask {
	private final static int MTHTREADS = 3;
	private static MongoClient mongoClient = new MongoClient(
			Arrays.asList(new ServerAddress("192.168.7.113", 30000),
					new ServerAddress("192.168.7.114", 30000)));
	private ArrayList<String> shas = new ArrayList<String>();

	public DownloadTask(ArrayList<String> shas) {
		this.shas = shas;
	}

	public void process() {
		MongoDatabase readDataBase = mongoClient.getDatabase("github");
		MongoCollection<Document> readCollection = readDataBase
				.getCollection("commits");

		for (String each : shas) {
			try {
				Document doc = readCollection.find(Filters.eq("sha", each))
						.projection(fields(include("files"), excludeId()))
						.first();
				if (doc != null) {
					Files files = JSON.parseObject(doc.toJson(), Files.class,
							Feature.IgnoreNotMatch);
					ArrayList<String> rawUrl = new ArrayList<String>();

					for (File file : files.getFiles())
						rawUrl.add(file.getRaw_url());

					downloadAndInsert(rawUrl);
				}
			} catch (Exception e) {
				System.out.println("commit sha : " + each + " | failed");
				e.printStackTrace();
			}
		}
	}

	private void downloadAndInsert(List<String> urls) throws Exception {
		MongoDatabase writeDataBase = mongoClient.getDatabase("CodeFiles");
		MongoCollection<Document> writeCollection = writeDataBase
				.getCollection("files");

		ExecutorService executor = Executors.newFixedThreadPool(MTHTREADS);
		CompletionService<List<String>> cs = new ExecutorCompletionService<List<String>>(
				executor);

		int taskNum = 0;

		for (final String address : urls) {
			try {
				// 从url中提取获得 用户/项目/项目文件路径
				String[] ss = address.split("/");
				String path = ss[3] + "/" + ss[4];
				for (int i = 7; i < ss.length; i++)
					path = path + "/" + ss[i];

				final String PATH = path;
				// 获得 commit sha
				final String commitSha = ss[6];

				System.out.println("**path: " + path);

				if (!FileFilter.filter(path))
					continue;
				// 查询数据库 进行 查重
				long cnt = writeCollection.count(Filters.eq("path", path));
				if (cnt > 0)
					continue;

				cnt = writeCollection.count(Filters.eq("sha", commitSha));
				if (cnt > 0)
					continue;

				cs.submit(new Callable<List<String>>() {

					public List<String> call() throws Exception {

						// download file
						URL url = new URL(address);
						HttpURLConnection conn = (HttpURLConnection) url
								.openConnection();
						conn.setConnectTimeout(10 * 1000);
						conn.setRequestProperty("User-Agent",
								"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
						InputStream inputStream = conn.getInputStream();
						String file = readInputStream(inputStream);
						return Arrays.asList(PATH, commitSha, file);
					}

				});

				taskNum++;
			} catch (Exception e) {
				System.out.println("**url : " + address + " failed");
				e.printStackTrace();
			}
		}

		for (int i = 0; i < taskNum; i++) {
			Future<List<String>> f = null;
			try {
				f = cs.poll(120, TimeUnit.SECONDS);
				List<String> record = null;
				if (f != null) {
					record = f.get();
					if (record.get(2) != null) {
						Document doc = new Document("path", record.get(0))
								.append("sha", record.get(1)).append(
										"fileContent", record.get(2));
						writeCollection.insertOne(doc);
					}
				}
			} catch (ExecutionException e) {
				System.out.println("下载文件， 抛出异常");
				e.printStackTrace();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			} finally {
				if (f != null)
					f.cancel(true);
			}
		}

		executor.shutdownNow();
	}

	private String readInputStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int len = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		String ret = bos.toString();
		bos.close();
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("dd");
		MongoDatabase database = mongoClient.getDatabase("github");
		MongoCollection<Document> collection = database
				.getCollection("commits");
		Document docc = collection
				.find(Filters.eq("sha",
						"2e411c8e6319d43a1a0df890938a842d6bdf248d"))
				.projection(fields(include("files"), excludeId())).first();

		Files files = JSON.parseObject(docc.toJson(), Files.class,
				Feature.IgnoreNotMatch);
		System.out.println(files.getFiles().get(0).getRaw_url());
		System.out.println(files.getFiles().get(1).getRaw_url());

		// 测试
		// MongoDatabase writeDataBase = mongoClient.getDatabase("CodeFiles");
		// MongoCollection<Document> writeCollection =
		// writeDataBase.getCollection("files");
		//
		// String address = files.getFiles().get(0).getRaw_url();
		//
		// String[] ss = address.split("/");
		// String path = ss[3] + "/" + ss[4];
		// for(int i=7;i<ss.length;i++)
		// path = path + "/" + ss[i];
		// //获得 commit sha
		// String commitSha = ss[6];
		//
		// System.out.println(path);
		// System.out.println(commitSha);
		//
		// // 查询数据库 进行 查重
		// long cnt = writeCollection.count(Filters.eq("path", path));
		// if(cnt > 0) return;
		//
		// cnt = writeCollection.count(Filters.eq("sha", commitSha));
		// if(cnt > 0) return;
		//
		// System.out.println("download begin!");
		//
		// URL url = new URL(address);
		// HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		// conn.setConnectTimeout(3*1000);
		// conn.setRequestProperty("User-Agent",
		// "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
		// InputStream inputStream = conn.getInputStream();
		// String file = readInputStream(inputStream);
		//
		// Document doc = new Document("path",path).append("sha",
		// commitSha).append("fileContent", file);
		// writeCollection.insertOne(doc);

	}
}