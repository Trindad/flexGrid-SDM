package flexgridsim.von;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


import flexgridsim.PhysicalTopology;

/**
 * 
 * @author trindade
 *
 */
public class VirtualTopologyGenerator {
	
	public static VirtualTopology generate(PhysicalTopology physicalTopology, int minNodes, int maxNodes, int connectivityProbability, int minAlternativeNodes, int maxAlternativeNodes,
											int minComputingResources, int maxComputingResources, int minCapacity, int maxCapacity)
	{
		VirtualTopology topology = new VirtualTopology();
		
		int n = getRandomValue(minNodes, maxNodes);
		System.out.println(n);
		for(int i = 0; i < n; i++) {
			VirtualNode node = new VirtualNode();
			
			//adding candidate physical nodes
			int physicalNodes = physicalTopology.getNumNodes();
			ArrayList<Integer> candidateNodes = getNRandomNodes(minAlternativeNodes, maxAlternativeNodes, physicalNodes);
			node.setCandidatePhysicalNodes(candidateNodes);
			
			
			//adding computing resource
			int computingResource = getRandomValue(minComputingResources, maxComputingResources);
			node.setComputeResource(computingResource);
			
			topology.nodes.add(node);
		}
		
		
		//Connecting virtual nodes
		for(int u = 0; u < (topology.nodes.size()-1); u++) {
			
			for(int v = u; v < topology.nodes.size(); v++) {
					
				boolean connect = new Random().nextInt(101) < connectivityProbability;
				
				if(connect == true) {
					
					VirtualLink link = new VirtualLink(topology.nodes.get(u), topology.nodes.get(v));
					link.setBandwidth(getRandomValue(minCapacity, maxCapacity));
					topology.links.add(link);
				}
				
			}
		}
		
		topology.calculateTotalRequestResources(maxComputingResources, maxCapacity);
	
		return topology;
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
	
}
