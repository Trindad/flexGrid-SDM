package flexgridsim.von.mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.Hooks;
import flexgridsim.PhysicalTopology;
import flexgridsim.rsa.VONRCSA;
import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualNode;
import flexgridsim.von.VirtualTopology;

public class MAPEMapper extends KeyLinkMapper {
	private PhysicalTopology temp;
	private int []transponders;

	public void vonArrival(VirtualTopology von) {
		
		int count = 0;
		for(int i = 0; i < pt.getNumNodes(); i++) {
			if (!Hooks.runBlockCostlyNodeFilter(i,pt) || !Hooks.runLimitCostlyNodeFilter(i, pt)) {
				
				count++;
        	}
		}
		if(count >= pt.getNumNodes()) 
		{
			System.out.println("VON Blocked: "+von.getID());
			cp.blockVon(von.getID());
			return;
		}
		
		this.temp = pt;
		PhysicalTopology ptCopy = new PhysicalTopology(pt);
		this.pt = ptCopy;
		
		vons.sort(Comparator.comparing(VirtualTopology::getTotalResources).reversed());
		
		if (rsa instanceof VONRCSA) {
			
			((VONRCSA) rsa).setVonControlPlane(cp);
		}
		
		Map<List<Integer>, List<Integer>> shortestPaths = shortestPaths();
		
		if(shortestPaths.isEmpty()) {
			cp.blockVon(von.getID());
			
			return;
		}
			
		von.nodes.sort(Comparator.comparing(VirtualNode::getRequestResource).reversed());
		

		transponders = new int[pt.getNumNodes()];
		for	(int i = 0; i < pt.getNumNodes(); i++) {
			transponders[i] = pt.getNode(i).getTransponders();
		}
		
		nodeMapping(sortResourceContributionDegree(von, shortestPaths), von);
		
		Map<Integer, Double> weights = getLinkWeight(shortestPaths);
		pt.setVonGraph(weights);
		
		von.links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
		
		boolean accepted = false;
		
		Map<Long, Flow> flows = new HashMap<Long, Flow>();
		for(VirtualLink link : von.links) {
			
			int source = link.getSource().getPhysicalNode();
			int destination = link.getDestination().getPhysicalNode();
			Flow flow = new Flow(link.getID(), source, destination, von.arrivalTime, link.getBandwidth(), von.holdingTime, 0, 0);
			
			if(ptCopy.getNode(source).getComputeResource() >= link.getSource().getComputeResource() || 
					ptCopy.getNode(destination).getComputeResource() >= link.getDestination().getComputeResource()) {
			
				if (rsa instanceof VONRCSA) {
					
					((VONRCSA) rsa).setPhysicalTopology(ptCopy);
					((VONRCSA) rsa).setVonControlPlane(cp);
				}
				
				flow.setComputingResource(link.getSource().getComputeResource(), link.getDestination().getComputeResource());
				flow.setVonID(von.getID());
				flow.setLightpathID(link.getID());
				rsa.flowArrival(flow);
				
			}
		
			if(!flow.isAccepeted()) {
				System.out.println("VON Blocked: "+von.getID());
				
				for(Long key : flows.keySet()) {
					Flow f = flows.get(key);
					if(f.isAccepeted()) {
						ptCopy.getNode(f.getSource()).updateTransponders(1);
						ptCopy.getNode(f.getDestination()).updateTransponders(1);
					}
				}
				
				accepted = false;
				cp.blockVon(von.getID());
				
				return;
			}
			
			ptCopy.getNode(flow.getSource()).updateTransponders(-1);
			ptCopy.getNode(flow.getDestination()).updateTransponders(-1);
			
			accepted = true;
			flows.put(flow.getID(),flow);
			link.setPhysicalPath(flow.getLinks());
		}
		
		if(accepted == true) {
			
			System.out.println("VON Accepted: "+von.getID());
			temp.updateEverything(ptCopy);
			cp.updateControlPlane(ptCopy);
			cp.acceptVon(von.getID(), flows);
		}
		
		pt = temp;
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
				transponders[selectedNode] -= 1;
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
			
			if(candidates.contains(node) && transponders[node] >= 1) {
				return node;
			}
		}
		
		return nodeIndex;
	}
}
