package flexgridsim.util;

import java.io.BufferedReader;
//import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import flexgridsim.Simulator;

/**
 * KMeans 
 * @author trindade
 *
 */
public class PythonCaller {
	
	private String scriptsPath;
	
	public PythonCaller() {
		String path = Simulator.PYTHON_PATH;
//		System.out.println("python path " + path);
		if (path != null) {
			this.scriptsPath = path;
		}
	}

	public KMeansResult kmeans(double [][]features, int k) {
		String path = this.getScriptsPath() + "kmeans.py";
		
//		System.out.println(convertToJSON(features));
		
		StringBuilder st = new StringBuilder();
		st.append("python3 ");
		st.append(path);
		st.append(" ");
		st.append(String.valueOf(k));
		st.append(" ");
		st.append('"' + convertToJSON(features) + '"');
		
		
//		System.out.println(st.toString());
		String output = executeCommand(st.toString());
//		System.out.println(output);
		KMeansResult result = new KMeansResult(output);
		
		
		return result;
	}

	private String getScriptsPath() {
		if (this.scriptsPath != null) {
			return this.scriptsPath;
		}
		
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		String path = s + "/python/";
		
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