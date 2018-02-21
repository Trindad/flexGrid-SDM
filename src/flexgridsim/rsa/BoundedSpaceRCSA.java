package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class BoundedSpaceRCSA extends SCVCRCSA{

	private int k = 2;
	
	private boolean kBoundedSpacePolicy(Flow flow, boolean [][]spectrum, int []links, int demandInSlots) {
	
		ArrayList<ArrayList<Slot>> slotList = new ArrayList< ArrayList<Slot> >();
		
		for (int i = 0; i < spectrum.length; i++) {
			
			ArrayList<Slot> s = new ArrayList<Slot>();
			for (int j = 0; j < spectrum[i].length; j++) {
				
				if(spectrum[i][j] == true) 
				{
//					System.out.println(i+" "+j);
					if(s.size() <= 0) 
					{
//						System.out.println(i+" "+j);
						s.add(new Slot(i, j));
					}
					else if( Math.abs(j - s.get(s.size()-1).s) == 1 ) {
//						System.out.println(i+" "+j);
						s.add(new Slot(i, j));
					}
					else 
					{
						if(s.size() >= demandInSlots) {
							slotList.add(s);
						}
						
						s.clear();
						s.add(new Slot(i, j));
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
					double xt = pt.getSumOfMeanCrosstalk(links, slots.get(0).c);//returns the sum of cross-talk	
				
					if(xt == 0 || (xt < ModulationsMuticore.inBandXT[flow.getModulationLevel()]) ) {

					if(establishConnection(links, slots, flow.getModulationLevel(), flow)) 
					{
						return true;
					}
				}
					
					slots.clear();
				}
			}
		}
		
		return false;
	}
	
	private int getDemandInSlots(Flow flow, int []links) {
		
		int modulation = chooseModulationFormat(flow, links);
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
		return (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
	}
	
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		return kBoundedSpacePolicy(flow, spectrum, links, getDemandInSlots(flow, links));
	}
	
}
