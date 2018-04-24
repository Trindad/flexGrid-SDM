package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class PartitionRCSA extends FCFFRCSA {

	public ArrayList<Slot> FirstFitPolicy(Flow flow, int []links, int demandInSlots, int modulation) {
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>();
		if(flow.getRate() == 50) {
			priorityCores.add(6);
			priorityCores.add(3);
		}
		else if(flow.getRate() == 400) {
			priorityCores.add(2);
			priorityCores.add(5);
		}
		else {
			priorityCores.add(1);
			priorityCores.add(4);
		}
		
		priorityCores.add(0);//the lowest priority 
		boolean [][]spectrum = bitMapAll(links);
		int maxSlotIndex = preProcessSpectrumResources(spectrum);
		
		for(int c = 0; c < priorityCores.size(); c++) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
			int k = priorityCores.get(c);
			for(int j = 0; j <= maxSlotIndex; j++) {
				
				int limit = j + (demandInSlots - 1);

				if(limit >= pt.getNumSlots()) {
					break;
				}
				
				int n = j;
				ArrayList<Slot> candidate = new ArrayList<Slot>();
				while(n <= limit && spectrum[k][n] == true ) {
					candidate.add( new Slot(k,n) );
					n++;
				}
				
				if(candidate.size() == demandInSlots) {
					if(cp.CrosstalkIsAcceptable(flow, links, candidate, ModulationsMuticore.inBandXT[modulation])) {
						slots.addAll(new ArrayList<Slot>(candidate));
						break;
					}
				}
			}
			
			if(slots.size() == demandInSlots) {
				return slots;
			}	
		}
			
		return new ArrayList<Slot>();
	}
	
}
