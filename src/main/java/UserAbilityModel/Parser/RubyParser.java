package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class RubyParser implements ILangParser {

	/*
	 *  ruby 语言的引入 形式 为(load 包名,)  或者 (require 包名)
	 *  （include 模块 ）表示插入了一个模块 （extend 模块）表示引入了一个模块
	 *  分析这四种情况就可以了。 同时在处理过程中要注意 代码注释对于分析的影响
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
				if (line.startsWith("require")) {
					flag = true;
					int endIndex = line.length();
					line = line.substring(7, endIndex);
					line = line.trim();
				    packages.add(line);
				}
				else if(line.startsWith("load")) {
					flag = true;
					int endIndex = line.length();
					line = line.substring(4, endIndex);
					line = line.trim();
				    packages.add(line);
				}
				else if(line.startsWith("include")) {
					flag = true;
					int endIndex = line.length();
					line = line.substring(7, endIndex);
					line = line.trim();
				    packages.add(line);
				}
				else if(line.startsWith("extend")) {
					flag = true;
					int endIndex = line.length();
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