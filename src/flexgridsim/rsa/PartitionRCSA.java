package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class PartitionRCSA extends SCVCRCSA{

	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int start = 0, stop = spectrum.length-1;
		
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[flow.getModulationLevel()];
		int demandInSlots = (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
//		System.out.println(flow.getModulationLevel());
		if(demandInSlots <= 5) {
			
			start = stop = 0;
		}
		else if(demandInSlots > 10) {
			
			start = 3;
		}
		else if (demandInSlots < 10 && demandInSlots > 5) {
			
			start = 1;
			stop = 2;
		}
				
		for (int i = start; i <= stop; i++) {
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i, flow.getRate());
			
			if(!fittedSlotList.isEmpty()) {
				return establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow);
			}
			
		}
		
		return false;
	}
}
