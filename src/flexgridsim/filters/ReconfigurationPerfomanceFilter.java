package flexgridsim.filters;

import flexgridsim.PhysicalTopology;
import flexgridsim.VonControlPlane;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;
import flexgridsim.rsa.VonReconfiguration;
import vne.VirtualNetworkEmbedding;


public class ReconfigurationPerfomanceFilter {
	
	private VonReconfiguration defragmentation;
	private double rate;
	
	public ReconfigurationPerfomanceFilter() {
		rate = 0;
	
	}
	
	public boolean filter() {
		
		return true;
	}

	public void run(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		this.defragmentation = new VonReconfiguration();
    	this.defragmentation.initialize(pt, vne, cp);
    	
    	updateFragmentationRatio(pt);
    	double before = rate;
    	System.out.println("before: "+rate);
    	defragmentation.runDefragmentantion();
    	updateFragmentationRatio(pt);
    	System.out.println("after: "+rate);
    	
    	//rewarding negatively
    	if(rate >= before) 
    	{
    		ShapedPlanRF.updateValue(new GridState(10,1), "right", -1);
    	}
    	else 
    	{
    		pt.updateEverything(defragmentation.pt);
    		ShapedPlanRF.updateValue(new GridState(10,1), "right", 1);
    	}
	}
	
	private void updateFragmentationRatio(PhysicalTopology pt) {
    	
    	int nLinks = pt.getNumLinks();
    	double []fi = new double[nLinks];
    	double nSlots = (pt.getNumSlots() * pt.getCores());
    	rate = 0;
    	
    	for(int i = 0; i < nLinks; i++) {
    		fi[i] =  (double)(nSlots - (double)pt.getLink(i).getSlotsAvailable()) / nSlots;
    		rate += fi[i];
    	}
    	
    	rate = (rate / (double)nLinks);
	}
	
}
