package flexgridsim.von.mappers;

import flexgridsim.PhysicalTopology;
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

	public void vonArrival(VirtualTopology von, RSA rsa, PhysicalTopology pt) {
		this.rsa = rsa;
		
	}

	public void vonDeparture(VirtualTopology von) {
		// TODO Auto-generated method stub
		
	}

}
