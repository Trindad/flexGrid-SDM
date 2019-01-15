package flexgridsim.filters;

import flexgridsim.PhysicalTopology;
import flexgridsim.VonControlPlane;
import vne.VirtualNetworkEmbedding;

public class RedirectingLightpathFilter {
	
	private int targetLink;

	public RedirectingLightpathFilter(int id) {
		targetLink = id;
	}

	public void run(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		
	}

}
