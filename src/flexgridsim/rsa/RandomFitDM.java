package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;

/**
 * @author pedrom
 *
 */
public class RandomFitDM extends DynamicModulation {
	
	private Random random;
	
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
		random = new Random(); 
	}

		
	public boolean fitConnection(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow, int modulation){
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		ArrayList<Integer> keys = new ArrayList<Integer>();
		for (Integer key : listOfRegions.keySet()) {
		    if (listOfRegions.get(key).size()-demandInSlots>=0){
		    	keys.add(key);
		    }
		}
		if (keys.isEmpty()){
			return false;
		}
		int key  = keys.get(random.nextInt(keys.size()));
		for (int i =  random.nextInt(listOfRegions.get(key).size()-demandInSlots+1); i < demandInSlots; i++) {
    		fittedSlotList.add(listOfRegions.get(key).get(i));
		}
    	if (establishConnection(links, fittedSlotList, flow, modulation)){
			return true;
		}
		return false;
	}
}
