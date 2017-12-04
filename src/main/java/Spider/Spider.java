package Spider;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import au.com.bytecode.opencsv.CSVReader;

public class Spider {

	private final int NTHREADS = 10;
	private final int NLINES = 100;
	private final int CAPACITY = 200;

	private BlockingQueue<DownloadTask> tasks = new LinkedBlockingQueue<DownloadTask>(
			CAPACITY);
	private ThreadPoolExecutor es = new ThreadPoolExecutor(NTHREADS, NTHREADS,
			0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	private void producer() {
		int metric = 0;
		try {
			CSVReader reader = new CSVReader(new FileReader("commits.csv"), ',');
			String[] nextLine = null;
			int rowIndex = 0;
			ArrayList<String> tmp = new ArrayList<String>();

			while ((nextLine = reader.readNext()) != null) {
				rowIndex++;
				tmp.add(nextLine[1]);

				if (rowIndex == NLINES) {
					tasks.put(new DownloadTask(tmp));
					tmp = new ArrayList<String>();
					rowIndex = 0;

					metric++;
					System.out.println("## Producer processed " + metric
							* NLINES + "lines of commits.csv");
				}
			}

			tasks.put(new DownloadTask(tmp));

			System.out.println("Finished!");

			reader.close();
		} catch (Exception e) {
			System.out.println("*********Producer failed!");
			System.out.println("Metric: " + metric);
			e.printStackTrace();
		}
	}

	private void downloadFiles() {
		while (true) {
			try {
				DownloadTask task = tasks.take();
				task.process();
			} catch (Exception e) {
				System.out.println("task failed");
				e.printStackTrace();
			}
		}
	}

	private void consumer() {
		for (int i = 0; i < NTHREADS; i++) {
			System.out.println("添加一个消费者工作线程");
			es.execute(new Runnable() {
				public void run() {
					downloadFiles();
				}
			});
		}
	}

	public static void main(String[] args) {

		final Spider sp = new Spider();

		new Thread() {
			public void run() {
				System.out.println("Producer start!");
				sp.producer();
			}
		}.start();

		new Thread() {
			public void run() {
				System.out.println("Consumer start!");
				sp.consumer();
			}
		}.start();
	}

}
