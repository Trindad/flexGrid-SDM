package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class BoundedSpaceRCSA extends SCVCRCSA{

	private int k = 10;
	
	private boolean kBoundedSpacePolicy(Flow flow, boolean [][]spectrum, int []links, int demandInSlots) {
	
		ArrayList<ArrayList<Slot>> slotList = new ArrayList< ArrayList<Slot> >();
		
		for (int i = 0; i < spectrum.length; i++) {
			
			ArrayList<Slot> s = new ArrayList<Slot>();
			for (int j = 0; j < spectrum[i].length; j++) {
				
				if(spectrum[i][j] == true) 
				{
					if(s.size() <= 0) {
						s.add(new Slot(i, j));
					}
					else if( Math.abs(j - s.get(s.size()-1).s) == 1 ) {
						s.add(new Slot(i, j));
					}
					else {
						
						if(s.size() >= demandInSlots) {
							slotList.add(s);
							break;
						}
						
						s.clear();
						s.add(new Slot(i, j));
					}
				}
			}
				
			if(s.size() >= demandInSlots) {
				slotList.add(s);
			}
			
			if(slotList.size() >= k) 
			{
				break;
			}
			
		}
		
//		System.out.println(slotList.size());
		slotList.sort((a, b) -> a.size() - b.size());
		
		for(ArrayList<Slot> set: slotList) {
			
			if(set.size() < demandInSlots) continue;
			ArrayList<Slot> slots = new ArrayList<Slot>();
			
			for(Slot s: set) {
				
				slots.add(s);
				if(slots.size() == demandInSlots) 
				{
					if(cp.CrosstalkIsAcceptable(flow, links, slots, ModulationsMuticore.inBandXT[flow.getModulationLevel()])) {
						
						if(establishConnection(links, slots, flow.getModulationLevel(), flow)) 
						{
							return true;
						}
					}
					
					slots.remove(0);
				}
			}
		}
		
		return false;
	}
	
	private int getDemandInSlots(Flow flow, int []links) {
		
		int modulation = chooseModulationFormat(flow.getRate(), links);
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
		flow.setModulationLevel(modulation);
		return (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
	}
	
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		int demand = getDemandInSlots(flow, links);
		int modulation = flow.getModulationLevel();
		while(modulation >= 0) {
			
			demand = (int) Math.ceil((double)flow.getRate() / ModulationsMuticore.subcarriersCapacity[modulation]);
			flow.setModulationLevel(modulation);
			if(kBoundedSpacePolicy(flow, spectrum, links, demand)) return true;
			modulation--;
		}
		
		return false;
	}
	
}
