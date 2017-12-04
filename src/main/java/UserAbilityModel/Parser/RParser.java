package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class RParser implements ILangParser {

	/*
	 *  R语言是解释执行的，引入 形式 为（ library(包名)）  或者 （require(包名)）
	 *  分析这两种情况就可以了。 同时在处理过程中要注意 代码注释对于分析的影响
	 *  因为R语言是解释执行的，且导入包可以发生在代码的任何位置所以要全部扫描
	 * @see UserAbilityModel.ILangParser#parse(java.lang.String)
	 */
	public List<String> parse(String code) {

		ArrayList<String> packages = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(code));
		
		try {
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("library")) {
					int endIndex = line.indexOf(')');
					line = line.substring(7, endIndex);
					line = line.trim();
					packages.add(line);
				}
					else if (line.startsWith("require")) {
						int endIndex = line.indexOf(')');
						line = line.substring(7, endIndex);
						line = line.trim();
						packages.add(line);
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