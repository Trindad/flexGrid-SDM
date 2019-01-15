package flexgridsim.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import flexgridsim.Database;
import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.VonControlPlane;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;
import vne.VirtualNetworkEmbedding;

public class RedirectingLightpathFilter {
	
	private int targetLink;

	public RedirectingLightpathFilter(int id) {
		targetLink = id;
	}

	public boolean filter() {
		
		return true;
	}
	
	public void run(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		
		double meanAvailableSlots = 0;
		for(int i = 0; i < pt.getNumLinks(); i++) {
			meanAvailableSlots += (double)pt.getLink(targetLink).getNumFreeSlots()/(pt.getCores() * pt.getNumSlots());
		}
		
		meanAvailableSlots = meanAvailableSlots / (double)pt.getNumLinks();
		
		for(Long key : flows.keySet()) {
			
			if(Arrays.asList( flows.get(key).getLinks() ).contains(targetLink))
			{		
				redictingLightpath(flows.get(key), targetLink, pt);
			}
			
			double current = (double)pt.getLink(targetLink).getNumFreeSlots()/(pt.getCores() * pt.getNumSlots());
			if(current >= meanAvailableSlots) 
			{
				ShapedPlanRF.updateValue(new GridState(4,1), "right", 1);
				
				return;
			}
		}
		
		ShapedPlanRF.updateValue(new GridState(4,1), "right", -1);
		
	}

	private void redictingLightpath(Flow flow, int link, PhysicalTopology pt) {
		// TODO Auto-generated method stub
		
	}

}
