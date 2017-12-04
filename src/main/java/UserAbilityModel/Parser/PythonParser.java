package UserAbilityModel.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class PythonParser implements ILangParser {

	public List<String> parse(String code) {
		ArrayList<String> packages = new ArrayList<String>();
		
		BufferedReader reader = new BufferedReader(new StringReader(code));
		try {
			String line = null;
			Boolean flag = false;
			
			while ((line = reader.readLine()) != null) {
				int len = line.indexOf('#');
				if(len!=-1)
					line = line.substring(0, len);
				line = line.trim();
				if(line.length()==0) continue;
				
				if (line.startsWith("import")) {
					flag = true;
					line = line.substring(6,line.length());
					line = line.trim();
					String[] strs = line.split(",");
					for (int i = 0; i < strs.length; i++) {
						strs[i] = strs[i].trim();
						
						if( strs[i].equals("as"))
							break;
						
						packages.add(strs[i]);
					}
				}
				else if (line.startsWith("from")) {
					flag = true;
					
					line = line.substring(4,line.length());
					String[] strs = line.split(" ");
					
					String pre = null;					
					Boolean tag = false;
					for (int i = 0; i < strs.length; i++) {
						
						strs[i] = strs[i].trim();
						
						if (strs[i].length() == 0)
							continue;
						
						if (strs[i].equals("import")) {
							tag = true;
							continue;
						}
						
						if (!tag) {
							pre = strs[i];
						}
						else {
							String[] ps = strs[i].split(",");
							for (int j = 0; j < ps.length; j++) {
								ps[j] = ps[j].trim();
								if (ps[j].length() == 0)
									continue;
								packages.add(pre+"."+ps[j]);
							}
						}
					}
				}
				else if (flag) {
					break;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return packages;
	}
	
	public static void main(String[] args) throws Exception {
		String code = "";
		File file = new File("C:\\Users\\buaaxzl\\Desktop\\jee_result.py");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			code += line + "\n";
		}
		br.close();
		for (String each : new PythonParser().parse(code))
			System.out.println(each);
	}

}
