package flexgridsim.util;

public class KMeansResult {

	private double [][]centroids;
	private String[] labels;
	private double silhouette;
	
	public KMeansResult(String output) {
		
		String []preparts = output.split("@");
//		System.out.println("output: "+output);
		this.silhouette = Double.parseDouble(preparts[0]);
		
		String []parts = preparts[1].split("&");
		this.labels = parts[0].split("-");
		
		String []temp = parts[1].split("/");
		int ydim = temp[0].split(",").length;
		
		this.centroids = new double[temp.length][ydim];
		
		for (int i = 0; i < temp.length; i++) {
			String []coords = temp[i].split(",");
			
			for (int j = 0; j < coords.length; j++) {
				this.centroids[i][j] = Double.parseDouble(coords[j]);
			}
		}
	}
	
	public double getSilhouette() {
		return silhouette;
	}

	public double[][] getCentroids() {
		
		return this.centroids;
	}

	public String[] getLabels() {
	
		return labels;
	}
}
