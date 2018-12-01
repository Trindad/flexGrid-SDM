package flexgridsim.von.mappers;

import org.w3c.dom.Element;

import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.VonControlPlane;
import flexgridsim.rsa.RSA;
import flexgridsim.von.VirtualTopology;

/**
 * A load balancing algorithm based on Key-Link and resources contribution degree for virtual optical networks mapping
 * 
 * Authors: G. Zhao, Z. Xu, Z. Ye, K. Wang and J. Wu
 * 
 * IEEE 2017
 * 
 * @author trindade
 *
 */
public class Mapper {
	
	private RSA rsa;
	private PhysicalTopology pt;
	private VirtualTopology von;

	public void vonArrival(VirtualTopology von, RSA rsa, PhysicalTopology pt) {
		this.rsa = rsa;
		this.pt = pt;
		this.von = von;
	}

	public void vonDeparture(VirtualTopology von) {
		// TODO Auto-generated method stub
		
	}

	public void simulationInterface(Element xml, PhysicalTopology pt, VonControlPlane vonControlPlane, TrafficGenerator traffic) {
		// TODO Auto-generated method stub
		
	}

}
