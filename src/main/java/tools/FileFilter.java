package tools;

public class FileFilter {
	
	public enum Language { 
		java
//		js,
//		bash,		// ����
//		sh,		//���Է��� һЩ shell �������ߵ�ʹ��
//		php,
//		c,
//		cpp,
//		rb,    //ruby
//		py,		//python
//		css,
//		html,
//		xml,
//		json,
//		awk,
//		cs,		//C#
//		go,		//go
//		m,		// objective-c
//		h,		// �������Զ�������� OC��C, C++, ruby ����
//		scala,
//		swift,
//		ts,		// TypeScript
//		vb,		// visual basic
//		pl,		// perl
//		pas,	//delphi,pascal
	}
	
	/*
	 * Only those languages which are in enum can be processed. 
	 * filter is to judge whether the file should be processed.
	 */
	public static Boolean filter(String fileName)
	{
		String[] tmp = fileName.split("\\.");
		if (tmp.length == 0) return false;
		
		String suffix = tmp[tmp.length - 1];
		for(int i=0;i<Language.values().length;i++)
		{
			Language lang = Language.values()[i];
			if (lang.toString().equalsIgnoreCase(suffix))
				return true;
		}
		return false;
	}
	
	/*
	 * return a Language enum to indicate the language of this fileName
	 */
	public static Language whichLanguage(String fileName)
	{
		String[] tmp = fileName.split("\\.");

		if (tmp.length == 0) return null;
		String suffix = tmp[tmp.length-1];
		Language check = Language.valueOf(suffix);
		
		return check;
	}
	
	public static void main(String[] args) {
		System.out.println(filter("adaf.java"));
	}

}
