package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.ResourceAssignment;
import flexgridsim.Slot;

/**
 * @author trindade
 *
 */

public class FirstFitRCSA extends ImageRCSA{

	public boolean fitConnection(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow){
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		boolean established = false;

		ResourceAssignment assigmnet = new ResourceAssignment(this);
		established = assigmnet.firstFit(listOfRegions,demandInSlots, links, flow);
		this.availableSlots = assigmnet.getNumberOfAvailableSlots();
		
		if(!established)
		{
			
			for (Integer key : listOfRegions.keySet()) 
			{
				if (listOfRegions.get(key).size() >= demandInSlots)
				{
					for(int i = 0; i < demandInSlots; i++) {
						fittedSlotList.add(listOfRegions.get(key).get(i));
					}
					
					if(establishConnection(links, fittedSlotList, 0, flow)) 
					{
						return true;
					}
				}
				
				fittedSlotList.clear();
			}
		}
		
		return established;
	}
}
