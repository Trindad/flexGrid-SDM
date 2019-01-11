package flexgridsim.filters;

import flexgridsim.PhysicalTopology;
import flexgridsim.VonControlPlane;
import flexgridsim.rsa.VonReconfiguration;
import vne.VirtualNetworkEmbedding;

public class ReconfigurationPerfomanceFilter {
	
	private VonReconfiguration defragmentation;
	
	public ReconfigurationPerfomanceFilter() {
		
	
	}
	
	public boolean filter() {
		
		return true;
	}

	public void run(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		this.defragmentation = new VonReconfiguration();
    	this.defragmentation.initialize(pt, vne, cp);
    	
    	defragmentation.runDefragmentantion();
	}
	
}
