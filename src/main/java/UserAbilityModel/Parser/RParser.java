package UserAbilityModel.Parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class RParser implements ILangParser {

	/*
	 *  R�����ǽ���ִ�еģ����� ��ʽ Ϊ�� library(����)��  ���� ��require(����)��
	 *  ��������������Ϳ����ˡ� ͬʱ�ڴ��������Ҫע�� ����ע�Ͷ��ڷ�����Ӱ��
	 *  ��ΪR�����ǽ���ִ�еģ��ҵ�������Է����ڴ�����κ�λ������Ҫȫ��ɨ��
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