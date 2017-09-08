package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.Slot;

/**
 * @author pedrom
 *
 */
public class BestFitRCSA extends ImageRCSA {
	public boolean fitConnection(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow){
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int minDiff = Integer.MAX_VALUE;
		for (Integer key : listOfRegions.keySet()) {
		    if (listOfRegions.get(key).size()-demandInSlots<minDiff && listOfRegions.get(key).size()-demandInSlots>=0){
		    	minDiff=key;
		    }
		}
		if (minDiff != Integer.MAX_VALUE){
		    for (int i = 0; i < demandInSlots; i++) {
	    		fittedSlotList.add(listOfRegions.get(minDiff).get(i));
			}
	    }
    	if (establishConnection(links, fittedSlotList, 0, flow)){
			return true;
		}
		return false;
	}
}
