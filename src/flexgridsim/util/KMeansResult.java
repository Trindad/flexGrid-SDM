package flexgridsim.util;

public class KMeansResult {

	private double [][]centroids;
	private String[] labels;
	
	
	public KMeansResult(String output) {
		
		String []parts = output.split("&");
		this.labels = parts[0].split("-");
		
		String []temp = parts[1].split("/");
		
		this.centroids = new double[temp.length][temp.length];
		
		for (int i = 0; i < temp.length; i++) {
			String []coords = temp[i].split(",");
			
			for (int j = 0; j < coords.length; j++) {
				this.centroids[i][j] = Double.parseDouble(coords[j]);
			}
		}
	}

	public double[][] getCentroids() {
		
		return this.centroids;
	}

	public String[] getLabels() {
	
		return labels;
	}
}
