package flexgridsim.von.mappers;

import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.PhysicalTopology;
import flexgridsim.TrafficGenerator;
import flexgridsim.VonControlPlane;
import flexgridsim.rsa.RSA;
import flexgridsim.von.VirtualTopology;

/**
 * 
 * @author trindade
 *
 */
public class Mapper {
	
	public RSA rsa;
	public PhysicalTopology pt;
	public ArrayList<VirtualTopology> vons;
	public VonControlPlane cp;

	public void vonArrival(ArrayList<VirtualTopology> vons) {
		
	}
	
	public void vonArrival(VirtualTopology von) {
		
		this.vons.add(von);
	}

	public void vonDeparture(VirtualTopology von) {
//		TODO
	}

	public void simulationInterface(Element xml, PhysicalTopology pt, VonControlPlane cp, TrafficGenerator traffic, RSA rsa) {
		this.rsa = rsa;
		this.pt = pt;
		this.vons = new ArrayList<VirtualTopology>();
		this.cp = cp;
	}

}
