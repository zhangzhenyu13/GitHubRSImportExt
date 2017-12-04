package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

public class RepairJsonData {

	public static void main(String[] args) {
		String filePath = "C:\\Users\\buaaxzl\\Desktop\\ExpData\\puppetlabs-puppetdb\\prs.json";
		String text = "", line = null;
		try (BufferedReader bReader = new BufferedReader(new FileReader(filePath))) {
			while ((line = bReader.readLine()) != null)
				text += line;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (text == null || text.length() == 0) {
			System.out.println("read failed");
			return;
		}
		
		JSONObject cpy = new JSONObject();
		JSONObject jsonOb = JSONObject.parseObject(text,Feature.IgnoreNotMatch);
		for (Map.Entry<String, Object> each : jsonOb.entrySet()) {
			Object val = each.getValue();
			JSONObject valOb = (JSONObject)val;
			Integer newKey = (Integer)valOb.get("number");
			cpy.put(newKey+"", valOb);
		}
		try (BufferedWriter bWriter = new BufferedWriter(new FileWriter(
				"C:\\Users\\buaaxzl\\Desktop\\ExpData\\puppetlabs-puppetdb\\prs_new.json"))) {
			bWriter.write(cpy.toJSONString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
