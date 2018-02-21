package flexgridsim.rsa;

import java.util.ArrayList;
import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.Slot;

public class ClusterRCSA extends SCVCRCSA {
	
	private ArrayList<Cluster> clusters;
	
	private ArrayList<Integer>identifyCluster(Flow flow, int h) {
		
		double  []distances = new double[this.clusters.size()];
		ArrayList<Integer> sortClusters = new ArrayList<Integer>();
		int i = 0;
		
		for(Cluster c: clusters) {
			
			if(c.getNumberOfFeatures() == 3) {
				
				distances[i] = euclidianDistance( c.getX(), c.getY(), c.getZ(), (double)(h * 100), (double)flow.getRate(), (flow.getDuration()*100) );
			}
			else if(c.getNumberOfFeatures() == 2)
			{
				distances[i] = euclidianDistanceTwoFeatures(c.getX(), c.getY(),flow.getRate(), (double)(h * 100));
			}

			sortClusters.add(i);
			i++;
		}
		
		sortClusters.sort((a,b) -> (int)(distances[a] * 1000) - (int)(distances[b] * 1000) );
		
		return sortClusters;
	}
	
	/**
	 * Three dimensions
	 * @return
	 */
	private double euclidianDistance(double p1, double p2, double p3, double q1, double q2, double q3) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2) + Math.pow(p3 - q3,2);
		
		return Math.sqrt(t);
	}
	
	/**
	 * Two dimensions
	 * @return
	 */
	private double euclidianDistanceTwoFeatures(double p1, double p2, double q1, double q2) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2);
		
		return Math.sqrt(t);
	}
	
	
	protected boolean runRCSA(Flow flow) {

		setkShortestPaths(flow);
		
		for(int i = 0; i < this.paths.size(); i++) {
			
			int []links = this.paths.get(i);
			boolean[][] spectrum = bitMapAll(links);
			
			if(cp.getClusters().isEmpty())
			{
				if(fitConnection(flow, spectrum, links)) {
//						 System.out.println("Connection accepted: "+flow);
						return true;
				}
			}
			else
			{
				if( fitConnectionUsingClustering(flow, spectrum, links)) 
				{
						System.out.println("Connection accepted: "+flow);
						return true;
				}
			}
		}
		
//		System.out.println("Connection blocked: "+flow);
		return false;
	}
	
	public boolean fitConnectionUsingClustering(Flow flow, boolean [][]spectrum, int[] links) {
		
		this.clusters = new ArrayList<Cluster>(cp.getClusters());
		ArrayList<Integer> sortClusters = identifyCluster(flow, links.length);
		
		for(int i = 0; i < 2; i++) {
			
			int []cores = this.clusters.get(sortClusters.get(i)).getCores();
		
			for(int j = 0; j < cores.length ; j++) {
				
				ArrayList<Slot> fittedSlotList = canBeFitConnection(flow, links, spectrum[cores[j]] , cores[j], flow.getRate());
				
				if(!fittedSlotList.isEmpty()) 
				{
					if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) 
					{
						
						return true;
					}
				}
			}
		}
	
		return false;
	}
}
