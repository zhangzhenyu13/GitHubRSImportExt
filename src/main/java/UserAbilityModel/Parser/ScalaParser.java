package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class ScalaParser implements ILangParser {

	/*
	 *  Scala����java����  ���Ե����� ��ʽ Ϊimport + ����
	 *  ������һ��������Ϳ����ˡ� ͬʱ�ڴ��������Ҫע�� ����ע�Ͷ��ڷ�����Ӱ��
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