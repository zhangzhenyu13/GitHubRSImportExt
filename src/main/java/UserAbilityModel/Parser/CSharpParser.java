package UserAbilityModel.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class CSharpParser implements ILangParser {

	/*
	 *  C# ���Ե����� ��ʽ Ϊ�� using + ������  ���� ��using ���� = ������
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
				if (line.startsWith("using")) {
					flag = true;
					int endIndex = line.indexOf(';');
					line = line.substring(5, endIndex);
					line = line.trim();
					
					if (line.indexOf('=') == -1)
						packages.add(line);
					else {
						int startIndex = line.indexOf('=') + 1;
						line = line.substring(startIndex);
						line = line.trim();
						packages.add(line);
					}
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

	public static void main(String[] args) throws Exception {
		String code = "";
		File file = new File("C:\\Users\\buaaxzl\\Desktop\\Program.cs");
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			code += line + "\n";
		}
		br.close();
		for (String each : new CSharpParser().parse(code))
			System.out.println(each);
	}
}
