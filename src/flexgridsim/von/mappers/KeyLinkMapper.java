package flexgridsim.von.mappers;

import java.util.ArrayList;
import java.util.Comparator;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.VonControlPlane;
import flexgridsim.rsa.RSA;
import flexgridsim.von.VirtualLink;
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
		
		for(VirtualTopology von: vons) {
			
			
			for(VirtualLink link : von.links) {
				
				Flow flow = new Flow(link.getID(), link.getSource().getPhysicalNode(), link.getDestination().getPhysicalNode(), von.arrivalTime, link.getBandwidth(), von.holdingTime, link.getSource().getComputeResource(), 0);
				rsa.flowArrival(flow);
			}
		}
		
	}
	
	public void nodeMapping() {
		
	}

	public void vonDeparture(VirtualTopology von) {
		// TODO Auto-generated method stub
		
	}

	public void simulationInterface(Element xml, PhysicalTopology pt, VonControlPlane vonControlPlane, TrafficGenerator traffic) {
			super.simulationInterface(xml, pt, vonControlPlane, traffic);
	}
}
