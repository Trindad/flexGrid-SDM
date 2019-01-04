package flexgridsim.von.mappers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.rsa.VONRCSA;
import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualNode;
import flexgridsim.von.VirtualTopology;

public class MAPEMapper extends KeyLinkMapper {

	public void vonArrival(VirtualTopology von) {
		
		System.out.println("MAPE Mapper");
		vons.sort(Comparator.comparing(VirtualTopology::getTotalResources).reversed());
		
		if (rsa instanceof VONRCSA) {
			
			((VONRCSA) rsa).setVonControlPlane(cp);
		}
		
		Map<List<Integer>, List<Integer>> shortestPaths = shortestPaths();
			
		von.nodes.sort(Comparator.comparing(VirtualNode::getRequestResource).reversed());
		
		nodeMapping(sortResourceContributionDegree(von, shortestPaths), von);
		
		Map<Integer, Double> weights = getLinkWeight(shortestPaths);
		pt.setVonGraph(weights);
		
		von.links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
		
		boolean accepted = false;
		PhysicalTopology ptCopy = new PhysicalTopology(pt);
		ArrayList<Flow> flows = new ArrayList<Flow>();
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
				System.out.println("VON Blocked: "+von.getID());
				accepted = false;
				cp.blockVon(von.getID());
				return;
			}
			
			accepted = true;
			flows.add(flow);
			link.setPhysicalPath(flow.getLinks());
		}
		
		if(accepted == true) {
			System.out.println("VON Accepted: "+von.getID());
			pt.updateEverything(ptCopy);
			cp.updateControlPlane(ptCopy);
			cp.acceptVon(von.getID(), flows);
		}
	}
	
}
