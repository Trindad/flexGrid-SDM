package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * First-Fit First-Core Algorithm
 * @author trindade
 *
 */
public class FFCFRCSA extends SCVCRCSA {

	private int preProcessSpectrumResources(boolean [][]spectrum) {
		
		int maxSlotIndex = 0;
		
		for(int core = (spectrum.length-1); core >= 0 ; core--) {
			int s = getMaximumIndexOfUsed(spectrum[core]);
			if(s > maxSlotIndex) {
				maxSlotIndex = s;
			}
		}
		
		
		return maxSlotIndex;
	}
	
	private int getMaximumIndexOfUsed(boolean[] core) {

		for(int i = (core.length-1); i >= 0; i--) {
			if(!core[i]) {
				return i;
			}
		}
		return 0;
	}

	public ArrayList<Slot> FirstFitPolicy(Flow flow, int []links, int demandInSlots, int modulation) {
		boolean [][]spectrum = bitMapAll(links);
		int maxSlotIndex = preProcessSpectrumResources(spectrum);
		
		for(int j = 0; j <= maxSlotIndex; j++) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
			int limit = j + (demandInSlots - 1);
			if(limit >= pt.getNumSlots()) {
				return new ArrayList<Slot>();
			}
			
			for(int k = (spectrum.length-1); k >= 0; k--) {
				int n = j;
				ArrayList<Slot> candidate = new ArrayList<Slot>();
				while(spectrum[k][n] == true && n <= limit) {
					candidate.add( new Slot(k,n) );
					n++;
				}
				if(candidate.size() == demandInSlots) {
					if(cp.CrosstalkIsAcceptable(flow, links, candidate, ModulationsMuticore.inBandXT[modulation])) {
						slots.addAll(new ArrayList<Slot>(candidate));
						break;
					}
				}
				slots.clear();
			}
			
			if(slots.size() == demandInSlots) {
				return slots;
			}
		}
		
		return new ArrayList<Slot>();
	}
	
	
	public ArrayList<Slot> canBeFitConnection(Flow flow, int[]links, boolean [][]spectrum, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
		
		while(modulation >= 0) {
			
			double requestedBandwidthInGHz = ( ((double)rate) / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			fittedSlotList = FirstFitPolicy(flow, links, demandInSlots, modulation);
			
			if(fittedSlotList.size() == demandInSlots) {
					
				if(!flow.isMultipath()) 
				{
					flow.setModulationLevel(modulation);
				}
				else 
				{
					flow.addModulationLevel(modulation);
				}
				
				return fittedSlotList;
				
			}
			
			modulation--;
		}
		
		
		return new ArrayList<Slot>();
	}
}
