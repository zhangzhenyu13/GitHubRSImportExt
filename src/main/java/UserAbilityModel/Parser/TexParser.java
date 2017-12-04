package UserAbilityModel.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;


public class TexParser implements ILangParser 
{
	public List<String> parse(String code) 
	{
		/*
		   Tex语言的引�?形式 为（ usepackage {包名}�?													
		 */	
		int importcount=0;
		ArrayList<String> packages = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(code));
	//	System.out.print("请输入源代码（Tex语言)"); 
		try {
				String line = null;
				Boolean flag = false;
			
			while ((line = reader.readLine()) != null) 
			{
				
				line = line.trim();
				
				if(line.startsWith("%")) continue;      //单行注释处理
				if(line.startsWith("\\iffalse"))	    //多行注释处理(\iffalse...\fi)
				{
					line = line.trim();
					while(line.contains("\\fi")==false)
					{
						line = reader.readLine();
						line = line.trim();
						while(line.equals(""))
						{
							line = reader.readLine();
						}
						
						
					}
					continue;
				}
				if(line.startsWith("\\begin{comment}"))	    //多行注释处理(\begin{comment}...\end{comment})
				{
					line = line.trim();
					while(line.contains("\\end{comment}")==false)
					{
						line = reader.readLine();
						line = line.trim();
						while(line.equals(""))
						{
							line = reader.readLine();
						}
						
						
					}
					continue;
				}
				
			
				
				if (line.startsWith("\\usepackage")) 
				{
					
					flag = true;
					
					line=line.substring(line.indexOf('{')+1, line.indexOf('}'));
					
					if(line.indexOf(',')!=-1)
					{
						String[] packagelist = line.split(",");
						int n=0;
						while(n<packagelist.length)
							{
								line=packagelist[n];
								importcount++;
								//System.out.print("包名"+importcount+":"+line+'\n'); 
								packages.add(line);
								n++;
							}
					}
					else
					{
						importcount++;
						//System.out.print("包名"+importcount+":"+line+'\n'); 
						packages.add(line);
					}
				}
				else {
					//if (!flag) continue;
					//else break;
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
