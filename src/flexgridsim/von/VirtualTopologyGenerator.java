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
	
	public static VirtualTopology generate(PhysicalTopology physicalTopology,int minNodes, int maxNodes, double connectivityProbability, int minAlternativeNodes, int maxAlternativeNodes,
											int minComputingResources, int maxComputingResources, int minCapacity, int maxCapacity)
	{
		VirtualTopology topology = new VirtualTopology();
		
		int n = getRandomValue(minNodes, maxNodes);
		
		for(int i = 0; i < n; n++) {
			VirtualNode node = new VirtualNode();
			
			//adding candidate physical nodes
			int physicalNodes = physicalTopology.getNumNodes();
			ArrayList<Integer> candidateNodes = getNRandomNodes(minAlternativeNodes, maxAlternativeNodes, physicalNodes);
			node.setCandidatePhysicalNodes(candidateNodes);
			
		
			
			topology.nodes.add(node);
		}
		
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
