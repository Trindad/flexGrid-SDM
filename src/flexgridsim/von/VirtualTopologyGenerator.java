package flexgridsim.von;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import flexgridsim.Hooks;
import flexgridsim.PhysicalTopology;

/**
 * 
 * @author trindade
 *
 */
public class VirtualTopologyGenerator {
	
	public static ArrayList<Integer> bandwidths;
	
	public static VirtualTopology generate(PhysicalTopology physicalTopology, int minNodes, int maxNodes, int connectivityProbability, int minAlternativeNodes, int maxAlternativeNodes,
											int minComputingResources, int maxComputingResources, int minCapacity, int maxCapacity)
	{
		VirtualTopology topology = new VirtualTopology();
		
		int n = getRandomValue(minNodes, maxNodes);
		
		int []nodes = new int[physicalTopology.getNumNodes()];
		int physicalNodes = physicalTopology.getNumNodes();
		int []transponders = new int[physicalTopology.getNumNodes()];
//		System.out.println("------------");
		
		for	(int i = 0; i < physicalTopology.getNumNodes(); i++) {
			transponders[i] = physicalTopology.getNode(i).getTransponders();
//			System.out.println("t: " + transponders[i]);
		}
//		System.out.println("------------");
		
		for(int i = 0; i < n; i++) {
			VirtualNode node = new VirtualNode();
			
			//adding candidate physical nodes
			ArrayList<Integer> candidateNodes = new ArrayList<Integer>();
			do {
				candidateNodes = getNRandomNodes(minAlternativeNodes, maxAlternativeNodes, physicalNodes);
				
				node.setCandidatePhysicalNodes(candidateNodes);
			}
			while(isValid(candidateNodes, nodes, physicalTopology, transponders));
			
			//adding computing resource
			int computingResource = getRandomValue(minComputingResources, maxComputingResources);
			node.setComputeResource(computingResource);
			
			topology.nodes.add(node);
		}
		
		
		setBandwidthVector(minCapacity, maxCapacity);
		
		//Connecting virtual nodes
		for(int u = 0; u < (topology.nodes.size()-1); u++) {
			
			int maxBandwidth = 0;
			ArrayList<Integer> setBandwidth = new ArrayList<Integer>();
			boolean connected = false;
		
			do {
				for(int v = u+1; v < topology.nodes.size(); v++) {
					
					int var = new Random().nextInt(100);
					boolean connect = var >= connectivityProbability;

					if(connect == true) {
						
						VirtualLink link = new VirtualLink(topology.nodes.get(u), topology.nodes.get(v));
						int bandwidth = getRandomBandwidth();
						link.setBandwidth(bandwidth);
						connected = connect;
						if(bandwidth > maxBandwidth) {
							maxBandwidth = bandwidth;
						}
						
						setBandwidth.add(bandwidth);
						topology.links.add(link);
					}
				}
			}
			while(!connected || !topology.isNodeConnected(topology.nodes.get(u)));
			
			
			double sumRequiredBandwidth = 0;
			for(int bandwidth: setBandwidth) {
				sumRequiredBandwidth += ((double)bandwidth/(double)maxBandwidth);
			}
			
			double requestResource = (double)topology.nodes.get(u).getComputeResource() * sumRequiredBandwidth;
			topology.nodes.get(u).setRequestResource(requestResource);
		}
		
		topology.calculateTotalRequestResources();

		return topology;
	}
	
	private static void setBandwidthVector(int minCapacity, int maxCapacity) {
		
		int add = 25, temp = minCapacity;
		bandwidths = new ArrayList<Integer>();
		
		while(temp <= maxCapacity) {
			bandwidths.add(temp);
			temp += add;
		}
	}

	private static boolean isValid(ArrayList<Integer> candidateNodes, int[] nodes, PhysicalTopology physicalTopology, int[] transponders) {
		
		for(int node : candidateNodes)  {
			
			if(nodes[node] >= 2) {
				return false;
			}
			
			
			if (!Hooks.runBlockCostlyNodeFilter(node ,physicalTopology) || transponders[node] <= 0) {
				
				System.out.println("TERE IS NO TP "+physicalTopology.getNode(node).getTransponders());
				return false;
        	}
			
		}

		for(int node : candidateNodes)  {
			transponders[node] -= 1;
			nodes[node] += 1;
		}
		
		return true;
	}

	private static ArrayList<Integer> getNRandomNodes(int minAlternativeNodes, int maxAlternativeNodes,
			int nNodes) {
		
		ArrayList<Integer> nodes = new ArrayList<>();
		ArrayList<Integer> candidateNodes = new ArrayList<>();
		
		for(int i = 0; i < nNodes; i++) {
			nodes.add(i);
		}
		
		int n = getRandomValue(minAlternativeNodes, maxAlternativeNodes);
		
		Collections.shuffle(nodes);
		
		for(int i = 0; i < n; i++) {
			candidateNodes.add(nodes.get(i));
		}
		
		return candidateNodes;
	}

	public static int getRandomValue(int min, int max) {
		
		Random r = new Random();
		
		int low = min;
		int high = max;
		int result = r.nextInt(high-low) + low;
		
		return result;
	}
	
	public static int getRandomBandwidth() {
		
		Random r = new Random();
		
		int low = 0;
		int high = bandwidths.size();
		
		int result = r.nextInt(high-low) + low;
		
		return bandwidths.get(result);
	}
	
}
