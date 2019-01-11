package flexgridsim.von;

import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.von.VirtualTopology;

public interface ControlPlaneForVon {
	
	public boolean acceptVon(int id, Map<Long, Flow> flows);
	
	public boolean blockVon(int id);
	
	public VirtualTopology getVon(int id);
	
}
