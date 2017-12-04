package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class RubyParser implements ILangParser {

	/*
	 *  ruby ���Ե����� ��ʽ Ϊ(load ����,)  ���� (require ����)
	 *  ��include ģ�� ����ʾ������һ��ģ�� ��extend ģ�飩��ʾ������һ��ģ��
	 *  ��������������Ϳ����ˡ� ͬʱ�ڴ��������Ҫע�� ����ע�Ͷ��ڷ�����Ӱ��
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