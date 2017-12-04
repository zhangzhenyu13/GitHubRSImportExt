package UserAbilityModel.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import UserAbilityModel.ILangParser;

public class GoParser implements ILangParser 
{
	public List<String> parse(String code) {		

		int importcount=0;
		ArrayList<String> packages = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new StringReader(code));
		//System.out.print("请输入源代码（go语言)"); 
		try {
				String line = null;
				Boolean flag = false;
			
			while ((line = reader.readLine()) != null) 
			{
				
				line = line.trim();
				
				if(line.startsWith("//")) continue;      //单行注释处理
				if(line.startsWith("/*"))			     //多行注释处理
				{
					line = line.trim();
					while(line.contains("*/")==false)
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
			
				
				if (line.startsWith("import")) 
				{
					
					flag = true;
					if(line.indexOf(';')==-1)
					{
						line = line.substring(6, line.length());
						line = line.trim();
						if(line.charAt(0)=='(')   //import(......)
						{
							while((line=reader.readLine()).indexOf(')')==-1)
							{
							
								if(line.equals(""))
								{
									continue;
								}
							
								line=line.substring(line.indexOf('"')+1,line.lastIndexOf('"'));
							
								importcount++;
								//System.out.print("包名"+importcount+":"+line+'\n'); 
								packages.add(line);			
							
							
							}
				/*		
					int endIndex = line.indexOf(';');
					line = line.substring(6, endIndex);
					line = line.trim();
					
					if (line.indexOf('=') == -1)
						packages.add(line);
					else {
						int startIndex = line.indexOf('=') + 1;
						line = line.substring(startIndex);
						line = line.trim();
						System.out.print("baomingwei:"+line); 
						packages.add(line);
					}
					}
				}
				*/
						}
						else     //import "xxx"
						{
							line=line.substring(line.indexOf('"')+1,line.lastIndexOf('"')); 
							importcount++;
							//System.out.print("包名"+importcount+":"+line+'\n'); 
							packages.add(line);
						}
					}
					else         //import "xxx";impot "xxx";
					{
						String[] packagelist = line.split(";");
						int n=0;
						while(n<packagelist.length)
							{
								line=packagelist[n].substring(packagelist[n].indexOf('"')+1,packagelist[n].lastIndexOf('"'));
								importcount++;
								//System.out.print("包名"+importcount+":"+line+'\n'); 
								packages.add(line);
								n++;
							}
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
