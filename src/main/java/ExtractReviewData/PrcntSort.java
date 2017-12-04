package ExtractReviewData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PrcntSort {
	
	private static final int PrCntThreshold = 2000;
	private static final int CollaboratorCntThreshold = 0;
	private static final int CommitCntThreshold = 2000;
	
	public List<Map.Entry<String, Integer>> buildMap(String file, HashMap<String, Integer> map) {
		List<Map.Entry<String, Integer>> list = null;
		try (BufferedReader bReader = new BufferedReader(new FileReader(file))) {
			String line = null;
			while ((line = bReader.readLine()) != null) {
				String[] words = line.split(" ");
				int val = 0;
				try {
					val = Integer.parseInt(words[0]);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				if (words[1] != null && words[1].length() > 0) 
					map.put(words[1],val);
			}
			
			list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return o2.getValue() - o1.getValue();
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	@SuppressWarnings("unused")
	public void extractProjects() {
		HashMap<String, Integer> prCntMap = new HashMap<String, Integer>();
		HashMap<String, Integer> collaboratorCntMap = new HashMap<String, Integer>();
		HashMap<String, Integer> commitCntMap = new HashMap<String, Integer>();
		
		List<Map.Entry<String, Integer>> prCntList = null;
		List<Map.Entry<String, Integer>> collaboratorCntList = null;
		List<Map.Entry<String, Integer>> commitCntList = null;
		
		prCntList = buildMap("/sdpdata2/xiazhenglin/prCnt", prCntMap);
		collaboratorCntList = buildMap("/sdpdata2/xiazhenglin/collaboratorCnt", collaboratorCntMap);
		commitCntList = buildMap("/sdpdata2/xiazhenglin/commitCnt", commitCntMap);
		
		try (BufferedWriter bWriter = new BufferedWriter(new FileWriter("/sdpdata2/xiazhenglin/projects_1"))) {
			int index = 0;
			while (prCntList.get(index).getValue() > PrCntThreshold) {
				String url = prCntList.get(index).getKey();
				if (collaboratorCntMap.get(url) > CollaboratorCntThreshold && 
						commitCntMap.get(url) > CommitCntThreshold) {
					
					bWriter.write(prCntList.get(index).getValue() + " " + 
							collaboratorCntMap.get(url) + " " +
							commitCntMap.get(url) + " " + url + "\n");
				}
				index++;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new PrcntSort().extractProjects();
	}
}
