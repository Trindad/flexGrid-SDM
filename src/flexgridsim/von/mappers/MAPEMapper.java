package flexgridsim.von.mappers;

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

	public void vonArrival(VirtualTopology von) {
		
		System.out.println("MAPE Mapper");
		
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
		
		nodeMapping(sortResourceContributionDegree(von, shortestPaths), von);
		
		Map<Integer, Double> weights = getLinkWeight(shortestPaths);
		pt.setVonGraph(weights);
		
		von.links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
		
		boolean accepted = false;
		
		Map<Long, Flow> flows = new HashMap<Long, Flow>();
		for(VirtualLink link : von.links) {
			
			int source = link.getSource().getPhysicalNode();
			int destination = link.getDestination().getPhysicalNode();
			Flow flow = new Flow(link.getID(), source, destination, von.arrivalTime, link.getBandwidth(), von.holdingTime, link.getSource().getComputeResource(), 0);
			
			if(ptCopy.getNode(source).getComputeResource() >= link.getSource().getComputeResource() || 
					ptCopy.getNode(destination).getComputeResource() >= link.getDestination().getComputeResource()) {
			
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
		
		this.pt = temp;
	}
	
}
