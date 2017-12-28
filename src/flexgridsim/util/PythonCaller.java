package flexgridsim.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * KMeans 
 * @author trindade
 *
 */
public class PythonCaller {

	public KMeansResult kmeans(double [][]features, int k) {
		
		
		
		String path = this.getScriptsPath() + "kmeans.py";
		
		System.out.println(convertToJSON(features));
		
		StringBuilder st = new StringBuilder();
		st.append("python3 ");
		st.append(path);
		st.append(" ");
		st.append(String.valueOf(k));
		st.append(" ");
		st.append('"' + convertToJSON(features) + '"');
		
		
		String output = executeCommand(st.toString());
		KMeansResult result = new KMeansResult(output);
		
		return result;
	}
	
	private String getScriptsPath() {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		String path = s + "/src/python/";
		
		return path;
	}

	private String executeCommand(String command) {

		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();

	}
	
	private String convertToJSON(double [][]arr) {
		String json = "";
		
		for (int i = 0; i < arr.length; i++) {
			ArrayList<String> arr2 = new ArrayList<>();
			
			for (double e : arr[i]) {
				arr2.add(String.valueOf(e));
			}
			
			json += "[" + String.join(",", arr2) + "]";
		}
		
		json += "";
		
		return json;
	}
	
}