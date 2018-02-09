package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;

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
	private ArrayList<Integer>identifyCluster(Flow flow, int h) {
		
		double  []distances = new double[this.clusters.size()];
		ArrayList<Integer> sortClusters = new ArrayList<Integer>();
		int i = 0;
		
		for(Cluster c: clusters) {
			
			if(c.getNumberOfFeatures() == 3) {
				
				distances[i] = euclidianDistance( c.getX(), c.getY(), c.getZ(), (double)(h * 100), (double)flow.getRate(), (flow.getDuration()*1000) );
			}
			else if(c.getNumberOfFeatures() == 2)
			{
				distances[i] = euclidianDistanceTwoFeatures(c.getX(), c.getY(),flow.getRate(), h * 100);
			}

			sortClusters.add(i);
			i++;
		}
		
		sortClusters.sort((a,b) -> (int)(distances[a] * 1000) - (int)(distances[b] * 1000) );
		
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
	private double euclidianDistance(double p1, double p2, double p3, double q1, double q2, double q3) {
		
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
	private double euclidianDistanceTwoFeatures(double p1, double p2, double q1, double q2) {
		
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
	private double euclidianDistance(double p1, double p2, double q1, double q2) {
		
		double t = Math.pow(p1 - q1, 2) + Math.pow(p2 - q2, 2) ;
		
		return Math.sqrt(t);
	}
	
	protected boolean runRCSA(Flow flow) {
		
		org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> kShortestPaths1 = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getGraph(), 3);
		List< GraphPath<Integer, DefaultWeightedEdge> > KPaths = kShortestPaths1.getPaths( flow.getSource(), flow.getDestination() );
			
		if(KPaths.size() >= 1)
		{
			boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
			
			for (int k = 0; k < KPaths.size(); k++) {
				
				spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
				List<Integer> listOfVertices = KPaths.get(k).getVertexList();
				int[] links = new int[listOfVertices.size()-1];
				
				for (int j = 0; j < listOfVertices.size()-1; j++) {
					
					links[j] = pt.getLink(listOfVertices.get(j), listOfVertices.get(j+1)).getID();
					bitMap(pt.getLink(listOfVertices.get(j), listOfVertices.get(j+1)).getSpectrum(), spectrum, spectrum);
					
				}
				
				
				if(cp.getClusters().isEmpty())
				{
					if( fitConnection(flow, spectrum, links) == true) {
//						 System.out.println("Connection accepted: "+flow);
						return true;
					}
					
				}
				else
				{
					if( fitConnectionUsingClustering(flow, spectrum, links) == true) 
					{
//						System.out.println("Connection accepted: "+flow);
						return true;
					}
				}
			}
		}
		
//		System.out.println("Connection blocked: "+flow);
		return false;
	}
	
	public boolean fitConnectionUsingClustering(Flow flow, boolean [][]spectrum, int[] links) {
		
		this.clusters = new ArrayList<Cluster>(cp.getClusters());
		ArrayList<Integer> sortClusters = identifyCluster(flow, links.length);
		
		for(int i = 0; i < sortClusters.size(); i++) {
			
			int []cores = this.clusters.get(sortClusters.get(i)).getCores();
		
			for(int j = 0; j < cores.length ; j++) {
//				System.out.println("core: "+cores[j]+" "+flow);
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

	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @param coreFull
	 * @return
	 */
	@SuppressWarnings("unused")
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
