package flexgridsim.rsa;

import java.util.ArrayList;
import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.Slot;

public class ClusterRCSA extends XTFFRCSA {
	
	private ArrayList<Cluster> clusters;
	
	private ArrayList<Integer>identifyCluster(Flow flow, int h) {
		
		double  []distances = new double[this.clusters.size()];
		ArrayList<Integer> sortClusters = new ArrayList<Integer>();
		int i = 0;
		
		for(Cluster c: clusters) {
			
			if(c.getNumberOfFeatures() == 3) {
				
				//nHops, bw, duration
				distances[i] = euclidianDistance( c.getX(), c.getY(), c.getZ(), (double)(h * 100), (double)flow.getRate(), (flow.getDuration()*100) );
			}
			else if(c.getNumberOfFeatures() == 2)
			{
				distances[i] = euclidianDistanceTwoFeatures(c.getX(), c.getY(), (double)(h * 100) ,  (flow.getRate() * 10));
			}

			sortClusters.add(i);
			i++;
		}
		
		sortClusters.sort((a,b) -> (int)(distances[a] * 1000) - (int)(distances[b] * 1000) );
		
		return sortClusters;
	}
	
	/**
	 * Two dimensions
	 * @return
	 */
	private double euclidianDistanceTwoFeatures(double p1, double p2, double q1, double q2) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2);
		
		return Math.sqrt(t);
	}
	
	/**
	 * Three dimensions
	 * @return
	 */
	private double euclidianDistance(double p1, double p2, double p3, double q1, double q2, double q3) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2) + Math.pow(p3 - q3,2);
		
		return Math.sqrt(t);
	}
	
	
	
	
	protected boolean runRCSA(Flow flow) {

		setkShortestPaths(flow);
		
		for(int []links : this.paths) {
			
			if(cp.getClusters().isEmpty())
			{
				if(fitConnection(flow, bitMapAll(links), links)) {
						return true;
				}
			}
			else
			{
				if( fitConnectionUsingClustering(flow, bitMapAll(links), links)) 
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean[][] initializeMatrix() {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		
		for(int i = 0; i < spectrum.length; i++) {	
			for(int j = 0; j < spectrum[i].length; j++) {
				spectrum[i][j] = false;
			}
		}
		
		
		return spectrum;
	}
	
	public boolean fitConnectionUsingClustering(Flow flow, boolean [][]spectrum, int[] links) {
		
		this.clusters = new ArrayList<Cluster>(cp.getClusters());
		ArrayList<Integer> sortClusters = identifyCluster(flow, links.length);
		boolean [][]setOfCores = initializeMatrix();
		int it = 0;
		for(int i : sortClusters) {
			
			int []cores = this.clusters.get(i).getCores();
			for(int j : cores) {
				setOfCores[j] = spectrum[j];
			}
			
			ArrayList<Slot> fittedSlotList = canBeFitConnection(flow, links, setOfCores, flow.getRate());
				
			if(!fittedSlotList.isEmpty()) 
			{
				if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) 
				{
					
					return true;
				}
			}
			
			it++;
			if(it >= 2) {
				break;
			}
		}
	
		return false;
	}

	
}
