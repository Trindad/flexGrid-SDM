package flexgridsim.von.mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.VonControlPlane;
import flexgridsim.rsa.RSA;
import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualNode;
import flexgridsim.von.VirtualTopology;

/**
 * 
 * A load balancing algorithm based on Key-Link and resources contribution degree for virtual optical networks mapping
 * 
 * Authors: G. Zhao, Z. Xu, Z. Ye, K. Wang and J. Wu
 * 
 * IEEE 2017
 * 
 * @author trindade
 *
 */
public class KeyLinkMapper extends Mapper {

	
	public void vonArrival(ArrayList<VirtualTopology> vons, RSA rsa, PhysicalTopology pt) {
	
		vons.sort(Comparator.comparing(VirtualTopology::getTotalResources).reversed());
		
		Map<Integer[], List<Integer>> shortestPaths = shortestPaths();
		
		for(VirtualTopology von: vons) {
			
			von.nodes.sort(Comparator.comparing(VirtualNode::getRequestResource).reversed());
			
			nodeMapping(sortResourceContributionDegree(von, shortestPaths), von);
			
			getLinkWeight(shortestPaths);
			
			von.links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
			for(VirtualLink link : von.links) {
				
				Flow flow = new Flow(link.getID(), link.getSource().getPhysicalNode(), link.getDestination().getPhysicalNode(), von.arrivalTime, link.getBandwidth(), von.holdingTime, link.getSource().getComputeResource(), 0);
				rsa.flowArrival(flow);
			}
		}
		
	}
	
	private Map<Integer[], List<Integer>> shortestPaths() {
		
		Map<Integer[], List<Integer>> shortestPaths = new HashMap<Integer[], List<Integer>>();
		
		for(int source = 0; source < pt.getNumNodes(); source++) {
			for(int destination = 0; destination < pt.getNumNodes(); destination++) {
				
				if(destination != source) {
					
					org.jgrapht.alg.shortestpath.DijkstraShortestPath<Integer, DefaultWeightedEdge> shortestPath = new org.jgrapht.alg.shortestpath.DijkstraShortestPath<Integer, DefaultWeightedEdge>(pt.getGraph(), 1);
					GraphPath<Integer, DefaultWeightedEdge> path = shortestPath.getPath( source, destination );
					
					List<Integer> listOfVertices = path.getVertexList();
					Integer[]nodes = new Integer[2];
					nodes[0] = source;
					nodes[1] = destination;
					
					shortestPaths.put(nodes, listOfVertices);
				}
			}
		}
		
		return shortestPaths;
	}

	private ArrayList<Integer> sortResourceContributionDegree(VirtualTopology von, Map<Integer[], List<Integer>> shortestPaths) {
		
		ArrayList<Integer> nodeIndices = new ArrayList<Integer>();
		double []rcd = new double[pt.getNumNodes()];
		
		for(int i = 0; i < rcd.length; i++) {
			rcd[i] = Double.MIN_VALUE;
			nodeIndices.add(i);
		}
		
		for(int source = 0; source < pt.getNumNodes(); source++) {
	
			for(int destination = 0; destination < pt.getNumNodes(); destination++) {
					
				if(source != destination) 
				{
					List<Integer> listOfVertices = shortestPaths.get(new Integer[] {source, destination});
					
					for(int i = 0; i < (listOfVertices.size()-1); i++) {
						
						double c = pt.getNode(listOfVertices.get(i)).getComputeResource() * pt.getNode(listOfVertices.get(i+1)).getComputeResource();
						double distance = pt.getLink(listOfVertices.get(i), listOfVertices.get(i+1)).getDistance();
						rcd[listOfVertices.get(i)] += (getMinimumBandwdith() * (c / Math.pow(distance, 2)));
					}
				}
			}
		}
		
		nodeIndices.sort((a, b) -> (int)((rcd[b] - rcd[a]) * 100.0f));
		
		return nodeIndices;
	}

	private double getMinimumBandwdith() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void nodeMapping(ArrayList<Integer> physicalNodes, VirtualTopology von) {
		
		ArrayList<Integer> temp = new ArrayList<Integer>(physicalNodes);
		
		for(VirtualNode node: von.nodes) {
			
			int selectedNode = getSelectedNode(temp, node.getCandidatePhysicalNodes());
			if(selectedNode >= 0) 
			{
				node.setPhysicalNode(selectedNode);
				temp.remove(selectedNode);
			}
		}
	}

	private int getSelectedNode(ArrayList<Integer> physicalNodes, ArrayList<Integer> candidates) {
		
		int nodeIndex = -1;
		
		for(int node : physicalNodes) {
			
			if(candidates.contains(node)) {
				return node;
			}
		}
		
		return nodeIndex;
	}
	
	public void bitMap(boolean[][] s1, boolean[][] s2, boolean[][] result) {

		for (int i = 0; i < result.length; i++) {
			
			for (int j = 0; j < result[i].length; j++) {
				result[i][j] = s1[i][j] && s2[i][j];
			}
		}
	}
	
	public int getNumberOfAvailableSLots(int id) {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		for (int i = 0; i < pt.getCores(); i++) {
			for (int j = 0; j < pt.getNumSlots(); j++) {
				spectrum[i][j] = true;
			}
		}
		
		
		bitMap(pt.getLink(id).getSpectrum(), spectrum, spectrum);
		
		
		int totalSlotsAvailable = 0;
		for(int i = 0; i < spectrum.length; i++) {
			for(int j = 0; j < spectrum[i].length; j++) {
				
				if(spectrum[i][j]) {
					totalSlotsAvailable++;
				}
				
			}
		}
		
		return totalSlotsAvailable;
	}
		

	public double getLinkWeight(Map<Integer[], List<Integer>> shortestPaths) {
	
		double linkWeight = 0;
		double lMax = getLongestLinkLength();
		
		Map<Integer, Double> weights = new HashMap<Integer, Double>();
	
		/**
		 * Number of all shortest paths
		 */
		double n = pt.getNumNodes();
		n = shortestPaths.size(); 
		
		double a = 0.2, b = 0.2, c = 0.6;
		
		for(int i = 0; i < pt.getNumLinks(); i++) {
			
			double nAppearances = getNumberOfAppearances(shortestPaths, i);
			double k = nAppearances / n;
			int availableSlots = getNumberOfAvailableSLots(i);
			int totalSlots = (pt.getCores() * pt.getNumSlots());
			int occupiedSlots = totalSlots - availableSlots;
			double weight = a * ( pt.getLink(i).getDistance() / lMax ) + b * ( occupiedSlots / totalSlots ) + c * k * ( occupiedSlots / totalSlots );
			weights.put(i, weight); 
		}
		
		return linkWeight;
	}
	
	private double getNumberOfAppearances(Map<Integer[], List<Integer>> shortestPaths, int id) {
		
		int count = 0;
		
		for(Integer []key : shortestPaths.keySet()) {
			
			if(shortestPaths.get(key).contains(id)) {
				count++;
			}
		}
		
		return count;
	}

	public double getLongestLinkLength() {
		
		int length = 0, temp = 0;
		
		for(int i = 0;i < pt.getNumLinks(); i++) {
			
			temp = pt.getLink(i).getDistance();
			if(temp > length) {
				length = temp;
			}
		}
		
		return length;
	}
	
	public void vonDeparture(VirtualTopology von) {
		// TODO Auto-generated method stub
		
	}

	public void simulationInterface(Element xml, PhysicalTopology pt, VonControlPlane vonControlPlane, TrafficGenerator traffic) {
			super.simulationInterface(xml, pt, vonControlPlane, traffic);
	}
}
