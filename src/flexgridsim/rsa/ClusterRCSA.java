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
	private ArrayList<Integer>identifyCluster(Flow flow, int nHops) {
		
		int  []distances = new int[this.clusters.size()];
		ArrayList<Integer> sortClusters = new ArrayList<Integer>();
		
		for(int i = 0; i < this.clusters.size(); i++) {
			
			if(clusters.get(i).getNumberOfFeatures() == 3) {
				
				distances[i] = (int)( (this.euclidianDistance(clusters.get(i).getX() * 100, clusters.get(i).getY(), clusters.get(i).getZ(),
						nHops, flow.getRate(), flow.getDuration())*100 ) * 100);
			}
			else
			{
				distances[i] = (int)( this.euclidianDistanceTwoFeatures(clusters.get(i).getX(), clusters.get(i).getY(),flow.getRate(), nHops)*100 );
			}

			sortClusters.add(i);
		}
		
		sortClusters.sort((a,b) -> distances[a] - distances[b]);
		
		return sortClusters;
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
	private double euclidianDistanceTwoFeatures(int p1, int p2, int q1, int q2) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2);
		
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
					if( fitConnection(flow, spectrum, links) == true) {
						System.out.println("Connection accepted: "+flow);
						return true;
					}
				}
				else
				{
					if( fitConnectionUsingClustering(flow, spectrum, links) == true) 
					{
						System.out.println("Connection accepted using clusterRCSA: "+flow);
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean fitConnectionUsingClustering(Flow flow, boolean [][]spectrum, int[] links) {
		
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		this.clusters = new ArrayList<Cluster>(cp.getClusters());
		ArrayList<Integer> sortClusters = identifyCluster(flow, links.length);
			
		int []cores = this.clusters.get(sortClusters.get(0)).getCores();
		
		for(int i = 0; i < cores.length; i++) {
			
			fittedSlotList = canBeFitConnection(flow, links, spectrum[cores[i]] , cores[i], flow.getRate());
			if(!fittedSlotList.isEmpty()) 
			{
				return establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow);
			}
		}
		
		return this.lastChanceToAssign(flow, spectrum, links, cores);
	}

	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @param coreFull
	 * @return
	 */
	private boolean lastChanceToAssign(Flow flow, boolean[][] spectrum, int[] links, int []coreFull) {
		
		ArrayList<Integer> sortFreeCore = new ArrayList<Integer>();
		int  []coreSlots = new int[pt.getCores()];

		for(int i = 0; i < pt.getCores(); i++) {
			sortFreeCore.add(i);
			int n = 0;
			
			for(int j = 0; j < spectrum[i].length; j++) {
				if(spectrum[i][j]) n++;
			}
			
			coreSlots[i] = n;
		}
		
		sortFreeCore.sort((a,b) -> coreSlots[a] - coreSlots[b]);
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		for(int i = 0; i < pt.getCores(); i++) {

			if(!this.isFull(sortFreeCore.get(i), coreFull)) 
			{
				fittedSlotList = canBeFitConnection(flow, links, spectrum[sortFreeCore.get(i)] , sortFreeCore.get(i), flow.getRate());
				if(!fittedSlotList.isEmpty()) return establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow);
			}
		}
		
		return false;
	}

	private boolean isFull(int core, int[] coreFull) {
		
		for(int i = 0; i < coreFull.length; i++) {
			
			if(core == coreFull[i]) {
				return true;
			}
		}
		return false;
	}
}
