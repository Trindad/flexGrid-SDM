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
		
		if(flow.getRate() == 10) {
		
			start = stop = 0;
		}
		else if(flow.getRate() == 40) {
			
			start = stop = 1;
		}
		else if(flow.getRate() == 100) {
			
			start = 2;
			start = 3;
		}
		
		//400 Gb free cores
		
		
		for (int i = start; i <= stop; i++) {
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i, flow.getRate());
			
			if(!fittedSlotList.isEmpty()) {
				if (establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public ArrayList<Slot> canBeFitConnection(Flow flow, int[]links, boolean []spectrum,int i,  int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int modulation = chooseModulationFormat(rate, links);
	
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
		int demandInSlots = (int) Math.ceil((double)rate / subcarrierCapacity);
		fittedSlotList = this.FirstFitPolicy(flow, spectrum, i, links, demandInSlots, modulation);
		
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
		
		fittedSlotList.clear();
		
		return new ArrayList<Slot>();
	}

	

	public ArrayList<Slot> FirstFitPolicy(Flow flow, boolean []spectrum, int i, int[] links, int demandInSlots, int modulation) {
		
		ArrayList<Slot> temp = new ArrayList<Slot>();
		for(int j = 0; j < spectrum.length; j++) {	
			
			if(spectrum[j] == true) {
				temp.add( new Slot(i,j) );
			}
			else {
				temp.clear();
				if(Math.abs(spectrum.length-j) < demandInSlots) return new ArrayList<Slot>();
			}
			
			if(temp.size() == demandInSlots) {
				
				if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[flow.getModulationLevel()])) {

					return temp;
				}
				
				temp.remove(0);
			}
		}
		
		return new ArrayList<Slot>();
	}
	
}
