package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.VirtualTopology;

public class BestEffortTrafficMigration {

	private Map<Long, Flow> flows;
	public BestEffortTrafficMigration(ControlPlaneForRSA cp, PhysicalTopology pt, VirtualTopology vt,
			Map<Long, Flow> flowsIndex, Map<Flow, LightPath> flowsAccepted, ArrayList<Flow> connectionDisrruption) {
		// TODO Auto-generated constructor stub
		flows = flowsIndex;
	}

	public Map<Long, Flow> run() {
		// TODO Auto-generated method stub
		return flows;
	}

}
