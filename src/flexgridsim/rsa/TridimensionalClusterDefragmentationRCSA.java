package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.util.KMeansResult;
import flexgridsim.util.PythonCaller;

/**
 * Tridimensional Cluster Defragmentation RCSA
 * @author trindade
 *
 */
public class TridimensionalClusterDefragmentationRCSA extends ClusterDefragmentationRCSA {
	
	//k = 4 for three dimensions 
	//k = 3 for two dimensions
	protected void runKMeans(int k , Map<Long, Flow> flows) {

		double[][] features = new double[flows.size()][3];
		ArrayList<Flow> listOfFlows = new ArrayList<Flow>();
		
		int i = 0;
		
		for(Long f: flows.keySet()) {
			
//			features[i][1] = (flows.get(f).getDuration() - (time - flows.get(f).getTime())) * 1000;
			features[i][0] = flows.get(f).getLinks().length * 100;
			features[i][1] = flows.get(f).getRate();
			features[i][2] = flows.get(f).getDuration() * 100;
			
			listOfFlows.add(flows.get(f));
			i++;
		}
		
		PythonCaller caller = new PythonCaller();
		KMeansResult result = caller.kmeans(features, k);
		String []labels = result.getLabels();
		double [][]centroids = result.getCentroids();
		
		this.clusters = new HashMap<Integer, ArrayList<Flow> >();
		
		for(i = 0; i < this.k; i++) {
			
			clusters.put(i, new ArrayList<Flow>());
			
		}
		
		for(i = 0; i < labels.length; i++) {
			clusters.get(Integer.parseInt(labels[i])).add(listOfFlows.get(i));
		}

		this.createClusters(centroids);
		
	}
	
	/**
	 * 
	 * @param centroids
	 */
	protected void createClusters(double [][]centroids) {
		
		distributeCores();
		ArrayList<Cluster> clustersStructure = new ArrayList<Cluster>();
		
		int index = this.pt.getCores();
		int next = index;

		for(int i = 0; i < centroids.length; i++) {
			
			Cluster c = new Cluster( centroids[i][0] , centroids[i][1], centroids[i][2], 3);
			
			int []temp = new int[cores[i]];
			index = nextLimit(index, i);
			next = (next - cores[i]);
			int k = 0;
			for (int j = index; j >= next && j >= 0; j--) {

				temp[k] = j;
				k++;
			}
			c.setCores(temp);
			clustersStructure.add(c);
		}
		
		cp.setClusters(clustersStructure);
		
	}
}
