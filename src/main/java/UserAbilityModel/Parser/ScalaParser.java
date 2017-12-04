package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class ScalaParser implements ILangParser {

	/*
	 *  Scala是类java语言  语言的引入 形式 为import + 包名
	 *  分析这一种种情况就可以了。 同时在处理过程中要注意 代码注释对于分析的影响
	 * @see UserAbilityModel.ILangParser#parse(java.lang.String)
	 */
	public List<String> parse(String code) {

		ArrayList<String> packages = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(code));
		
		try {
			String line = null;
			Boolean flag = false;
			
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("import")) {
					flag = true;
					int endIndex = line.indexOf(';');
					line = line.substring(6, endIndex);
					line = line.trim();
					packages.add(line);
				}
				else {
					if (!flag) continue;
					else break;
				}
			}
			
		} catch (Exception e) {
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
}