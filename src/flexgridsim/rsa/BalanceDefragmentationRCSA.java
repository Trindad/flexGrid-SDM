package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;
import flexgridsim.rsa.ZhangDefragmentationRCSA;

public class BalanceDefragmentationRCSA extends ZhangDefragmentationRCSA{

	protected double []fi;

	public void setFragmentationIndexOfEachLink(double []fi) {
		
		this.fi = fi;
	}
	
	public void flowArrival(Flow flow) {

//		kPaths = 4;
		ArrayList<int[]> kPaths = findKPaths(flow);//find K-Shortest paths using Dijkstra
		ArrayList<Integer> indices = orderKPaths(kPaths);//sort paths by the fragmentation index from each link
//		System.out.println(flow);
		for(int i: indices)
		{
			if(fitConnection(flow, bitMapAll(kPaths.get(i)), kPaths.get(i))) 
			{
				return;
			}
		}
	
		this.connectionDisruption.add(flow);
		flow.setConnectionDisruption(true);
		this.nConnectionDisruption++;
	}
	
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
				
				if(slotList.size() >= 1) 
				{
					break;
				}
			}
		}
		
		slotList.sort((a, b) -> a.size() - b.size());
		
		for(ArrayList<Slot> set: slotList) {
			
			if(set.size() < demandInSlots) continue;
			ArrayList<Slot> slots = new ArrayList<Slot>();
			
			for(Slot s: set) {
				
				slots.add(s);
				if(slots.size() == demandInSlots) 
				{
					if(establishConnection(links, slots, flow.getModulationLevel(), flow)) 
					{
						return true;
					}
					
					slots.clear();
				}
			}
		}
		
		return false;
	}
	
	private int getDemandInSlots(Flow flow, int []links) {
		
		int modulation = chooseModulationFormat(flow.getRate(), links);
		double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
		return (int) Math.ceil((double)flow.getRate() / subcarrierCapacity);
	}
	
	public boolean fitConnection(Flow flow, boolean [][]spectrum, int[] links) {
		
		int d =  getDemandInSlots(flow, links);
//		System.out.println(flow.getModulationLevel() + " "+flow.getRate()+" "+d);
		return kBoundedSpacePolicy(flow, spectrum, links, d);
	}

	private ArrayList<Integer>orderKPaths(ArrayList<int[]> kPaths) {
		
		ArrayList<Integer> sumLightpath = new ArrayList<Integer>();
		ArrayList<Integer> indices = new ArrayList<Integer>(); 
		int index = 0;

		for(int[] links: kPaths) {

			double s = Double.NEGATIVE_INFINITY;
			for(int i = 0; i < links.length; i++) {
				s = s < fi[ links[i] ] ? fi[ links[i] ] : s;
			}
			
			sumLightpath.add( (int)(s * 100) );
			indices.add(index);
			index++;
		}
		
		indices.sort( (a , b) -> sumLightpath.get(a) - sumLightpath.get(b));
		
		return indices;
	}
	
}
