package flexgridsim.von.mappers;

import java.util.ArrayList;
import java.util.Arrays;
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
import flexgridsim.rsa.VONRCSA;
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

	
	public void vonArrival(ArrayList<VirtualTopology> vons) {
	
		System.out.println("Key-Link Mapper");
		vons.sort(Comparator.comparing(VirtualTopology::getTotalResources).reversed());
		
		if (rsa instanceof VONRCSA) {
			
			((VONRCSA) rsa).setVonControlPlane(cp);
		}
		
		Map<List<Integer>, List<Integer>> shortestPaths = shortestPaths();
		
		
		for(VirtualTopology von: vons) {
			
			if(shortestPaths.isEmpty()) {
				cp.blockVon(von.getID());
			}
			
			von.nodes.sort(Comparator.comparing(VirtualNode::getRequestResource).reversed());
			
			nodeMapping(sortResourceContributionDegree(von, shortestPaths), von);
			
			Map<Integer, Double> weights = getLinkWeight(shortestPaths);
			pt.setVonGraph(weights);
			
			von.links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
			
			boolean accepted = false;
			PhysicalTopology ptCopy = new PhysicalTopology(pt);
			Map<Long, Flow> flows = new HashMap<Long, Flow>();
			for(VirtualLink link : von.links) {
				
				int source = link.getSource().getPhysicalNode();
				int destination = link.getDestination().getPhysicalNode();
				Flow flow = new Flow(link.getID(), source, destination, von.arrivalTime, link.getBandwidth(), von.holdingTime, link.getSource().getComputeResource(), 0);
				
				if(pt.getNode(source).getComputeResource() >= link.getSource().getComputeResource() || 
						pt.getNode(destination).getComputeResource() >= link.getDestination().getComputeResource()) {
				
					if (rsa instanceof VONRCSA) {
						
						((VONRCSA) rsa).setPhysicalTopology(ptCopy);
						((VONRCSA) rsa).setVonControlPlane(cp);
					}
					
					
					flow.setVonID(von.getID());
					flow.setLightpathID(link.getID());
					rsa.flowArrival(flow);
					
				}
			
				if(!flow.isAccepeted()) {
//					System.out.println("VON Blocked: "+von.getID());
					for(Long key : flows.keySet()) {
						Flow f = flows.get(key);
						if(f.isAccepeted()) {
							ptCopy.getNode(f.getSource()).updateTransponders(1);
							ptCopy.getNode(f.getDestination()).updateTransponders(1);
						}
					}
					
					accepted = false;
					cp.blockVon(von.getID());
					break;
				}
				
				ptCopy.getNode(flow.getSource()).updateTransponders(-1);
				ptCopy.getNode(flow.getDestination()).updateTransponders(-1);
				
				accepted = true;
				flows.put(flow.getID(),flow);
				link.setPhysicalPath(flow.getLinks());
			}
			
			if(accepted == true) 
			{
				cp.updateControlPlane(ptCopy);
				cp.acceptVon(von.getID(), flows);
			}
		}
		
	}
	
	protected Map<List<Integer>, List<Integer>> shortestPaths() {
		
		Map<List<Integer>, List<Integer>> shortestPaths = new HashMap<List<Integer>, List<Integer>>();
		
		for(int source = 0; source < pt.getNumNodes(); source++) {
			for(int destination = 0; destination < pt.getNumNodes(); destination++) {
				
				if(destination != source) {
					org.jgrapht.alg.shortestpath.DijkstraShortestPath<Integer, DefaultWeightedEdge> shortestPath = new org.jgrapht.alg.shortestpath.DijkstraShortestPath<Integer, DefaultWeightedEdge>(pt.getGraph());
					GraphPath<Integer, DefaultWeightedEdge> path = shortestPath.getPath( source, destination );
					
					if(path != null) {
						
						try {
							
							List<Integer> listOfVertices = path.getVertexList();
							List<Integer> nodes = new ArrayList<Integer>();
							nodes.add(source);
							nodes.add(destination);
							
							shortestPaths.put(nodes, listOfVertices);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		return shortestPaths;
	}

	protected ArrayList<Integer> sortResourceContributionDegree(VirtualTopology von, Map<List<Integer>, List<Integer>> shortestPaths) {
		
		ArrayList<Integer> nodeIndices = new ArrayList<Integer>();
		double []rcd = new double[pt.getNumNodes()];
		
		for(int i = 0; i < rcd.length; i++) {
			rcd[i] = Double.MIN_VALUE;
			nodeIndices.add(i);
		}
//		System.out.println(shortestPaths.size());
		for(int source = 0; source < pt.getNumNodes(); source++) {
	
			for(int destination = 0; destination < pt.getNumNodes(); destination++) {
					
				if(source != destination) 
				{
					List<Integer> listOfVertices = shortestPaths.get(Arrays.asList(new Integer[] {source, destination}));
					
					if (listOfVertices != null)  {
						for(int i = 0; i < (listOfVertices.size()-1); i++) {
							
							int index = listOfVertices.get(i);
							double c = pt.getNode(index).getComputeResource() * pt.getNode(listOfVertices.get(i+1)).getComputeResource();
							double distance = pt.getLink(listOfVertices.get(i), listOfVertices.get(i+1)).getDistance();
							rcd[listOfVertices.get(i)] += (getMinimumBandwdith() * (c / Math.pow(distance, 2)));
						}
					}
				}
			}
		}
		
		nodeIndices.sort((a, b) -> (int)((rcd[b] - rcd[a]) * 100.0f));
		
		return nodeIndices;
	}

	protected double getMinimumBandwdith() {
		
		return 12.5;
	}

	public void nodeMapping(ArrayList<Integer> physicalNodes, VirtualTopology von) {
		
		ArrayList<Integer> temp = new ArrayList<Integer>();
		
		ArrayList<VirtualNode> nodes = new ArrayList<VirtualNode>();
		for(VirtualNode node: von.nodes) {
			int selectedNode;
			ArrayList<Integer> available = new ArrayList<>(physicalNodes);
			
			available.removeAll(temp);
			
			do {
				selectedNode = getSelectedNode(available, node.getCandidatePhysicalNodes());
			} while (temp.contains(selectedNode));
			
			if(selectedNode >= 0) 
			{
				node.setPhysicalNode(selectedNode);
				temp.add(selectedNode);
			}
			else {
				nodes.add(node);
			}
		}
		
		for(VirtualNode node: nodes) {
			int selectedNode;
			ArrayList<Integer> available = new ArrayList<>(physicalNodes);
			
			available.removeAll(temp);
			
			do {
				selectedNode = getSelectedNode(available, available);
			} while (temp.contains(selectedNode));
			
			if(selectedNode >= 0) 
			{
				node.setPhysicalNode(selectedNode);
				temp.add(selectedNode);
			}
			else {
				System.out.println("Node doesn't exist");
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
		

	public Map<Integer, Double> getLinkWeight(Map<List<Integer>, List<Integer>> shortestPaths) {
	
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
		
		return weights;
	}
	
	private double getNumberOfAppearances(Map<List<Integer>, List<Integer>> shortestPaths, int id) {
		
		int count = 0;
		
		for(List<Integer> key : shortestPaths.keySet()) {
			
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

	public void simulationInterface(Element xml, PhysicalTopology pt, VonControlPlane cp, TrafficGenerator traffic, RSA rsa) {
			super.simulationInterface(xml, pt, cp, traffic, rsa);
	}
}
