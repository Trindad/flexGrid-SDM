package flexgridsim.von;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.von.VirtualTopology;

public interface ControlPlaneForVon {
	
	public boolean acceptVon(int id, ArrayList<Flow> flows);
	
	public boolean blockVon(int id);
	
	public VirtualTopology getVon(int id);
	
}
