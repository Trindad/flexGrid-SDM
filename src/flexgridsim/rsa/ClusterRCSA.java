package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Cluster;
import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.util.KShortestPaths;

public class ClusterRCSA extends SCVCRCSA {
	
	private ArrayList<Cluster> clusters;
	
	/**
	 * 
	 * @param flow
	 * @return
	 */
	private int identifyCluster(Flow flow) {
		
		ArrayList<Double> distances = new ArrayList<Double>();
		
		int cluster = 0;
		
		for(int i = 0; i < clusters.size(); i++) {
			
			distances.add(this.euclidianDistance(clusters.get(i).getModulationLevel(), clusters.get(i).getRate(), clusters.get(i).getDuration(),
					flow.getModulationLevel(), flow.getRate(), flow.getDuration()));
		}
		
		double lastDistance = Double.MAX_VALUE;
		double newDistance = Double.MAX_VALUE;
		
		for(int i = 1; i < distances.size(); i++) {
			
			newDistance = distances.get(i);
			
			if(newDistance < lastDistance) {
				
				lastDistance = newDistance;
				cluster = i;
			}
		}
		
		return cluster;
	}
	
	/**
	 * Three dimensions
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param q1
	 * @param q2
	 * @param q3
	 * @return
	 */
	private double euclidianDistance(int p1, int p2, double p3, int q1, int q2, double q3) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2) + Math.pow(p3 - q3,2);
		
		return Math.sqrt(t);
	}
	
	/**
	 * Two dimensions
	 * @param p1
	 * @param p2
	 * @param q1
	 * @param q2
	 * @return
	 */
	@SuppressWarnings("unused")
	private double euclidianDistance(int p1, int p2, int q1, int q2) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2) ;
		
		return Math.sqrt(t);
	}
	
	protected boolean runRCSA(Flow flow) {
		
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 3);

		if(kPaths.length >= 1)
		{
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			for (int k = 0; k < kPaths.length; k++) {
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());

				int[] links = new int[kPaths[k].length - 1];
				for (int j = 0; j < kPaths[k].length - 1; j++) {
					
					links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
					bitMap(pt.getLink(kPaths[k][j], kPaths[k][j+1]).getSpectrum(), spectrum, spectrum);
				}
				
				if(cp.getClusters().isEmpty())
				{
					System.out.println("Normal way");
					if( fitConnection(flow, spectrum, links) == true) {
						return fitConnection(flow, spectrum, links);
					}
				}
				else
				{
					System.out.println("Clustering way");
					if( fitConnectionUsingClustering(flow, spectrum, links) == true) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean fitConnectionUsingClustering(Flow flow, boolean [][]spectrum, int[] links) {
		
		int idCluster = identifyCluster(flow);
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		this.clusters = cp.getClusters();
		
		if(idCluster >= 0) 
		{
			int []cores = this.clusters.get(idCluster).getCores();
			
			for(int i = 0; i < cores.length; i++) {
				
				fittedSlotList = canBeFitConnection(flow, links, spectrum[i] , i, flow.getRate());
				if(!fittedSlotList.isEmpty()) return establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow);
			}
		}
		
		return false;
	}
}
