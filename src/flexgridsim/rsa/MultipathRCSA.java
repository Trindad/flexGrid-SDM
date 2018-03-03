package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathRCSA extends SCVCRCSA {
	
	private int TH = 1250;
	
	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {

		//Try to assign in a normal way
		if(this.runRCSA(flow)) 
		{
//			System.out.println("Connection accepted: "+flow);
			return;
		}
		
		if(flow.getRate() >= TH)
		{
			kPaths = 3;
			flow.setMultipath(true);
			if(multipathEstablishConnection(flow)) 
			{
				this.paths.clear();
				System.out.println("Connection accepted using multipath: "+flow);
				return;
			}
		}
		
//		System.out.println("Connection blocked: "+flow);
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}
	
	
	

	protected boolean multipathEstablishConnection(Flow flow) {

		this.setkShortestPaths(flow);
		
		if(getkShortestPaths().size() <= 1)
		{
			return false;
		}
		
		ArrayList<int[]> lightpaths = new ArrayList<int[]>();
		ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(getkShortestPaths(), flow, lightpaths);
		
		
		if(!slotList.isEmpty()) {
//			System.out.println(lightpaths.size());
			if(this.establishConnection(lightpaths, slotList, flow.getModulationLevels(), flow))
			{
				return true;
			}
		}
		
		
		return false;
	}
	
	private ArrayList<Slot> getSetOfSlotsAvailable(int[] lightpath, Flow flow, int rate ) {
		
		ArrayList<Slot> setOfSlots = getSlotsAvailable(lightpath, flow, rate);
		
		if(!setOfSlots.isEmpty()) 
		{
			return new ArrayList<Slot>(setOfSlots);
		}
		
		
		return new ArrayList<Slot>();
		
	}
	

	private ArrayList<ArrayList<Slot>> getSetOfSlotsAvailableInEachPath(ArrayList<int[]> lightpaths, Flow flow, ArrayList<int[]> lightpathsAvailable) {
		
		int totalRate = flow.getRate();
		
		for(int i = 1; i < kPaths; i++) {
			
			int rate = (int) Math.ceil((double)flow.getRate()/(double)(i+1));//rate for each lightpath
			
			ArrayList<int[]> lightpathsAvailableTemp = new ArrayList<int[]>();
			ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>>();
			
			for(int j = 0; j <= i; j++) {
				
				ArrayList<Slot> slots = getSetOfSlotsAvailable(lightpaths.get(j), flow, rate);
				
				if(!slots.isEmpty()) {
					
					slotList.add(new ArrayList<Slot>(slots));
					lightpathsAvailableTemp.add( lightpaths.get(j) );
					totalRate -= rate;
					
					if(totalRate < rate) 
					{
						rate = totalRate;
					}
					
				}
			}
			
			if(slotList.size() == (i + 1) ) {
				
				lightpathsAvailable.addAll( new ArrayList<int[]>(lightpathsAvailableTemp) );
				
				return slotList;
			}
			
			flow.getModulationLevels().clear();
			
		}
		
		
		return new ArrayList<ArrayList<Slot>>();
	}
	
	private ArrayList<Slot> getSlotsAvailable(int []links, Flow flow, int rate) {
		
		return canBeFitConnection(flow, links, bitMapAll(links),  rate);
	}




	@SuppressWarnings("unused")
	private double getNewRate(int modulation, int n) {
		
		return (ModulationsMuticore.subcarriersCapacity[modulation] * n);
	}




	protected void updateData(Flow flow, ArrayList<int []> mpaths, ArrayList<Long> ids,  ArrayList< ArrayList<Slot> > fittedSlotList, ArrayList<Integer> modulation) {
		
		
		flow.setLinks(mpaths);
		flow.setSlotListMultipath(fittedSlotList);
		flow.setModulationLevels(modulation);
		flow.setLightpathsID(ids);
		
		ArrayList<LightPath> lps = new ArrayList<LightPath>();
		
		for(long id: ids) {
			lps.add(vt.getLightpath(id));
		}
		
		if(!cp.acceptFlow(flow.getID(), lps)) {
			throw (new IllegalArgumentException());
		}
	}
	
	
	
	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public ArrayList<Slot>fitConnection(Flow flow, boolean [][]spectrum, int []links, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		flow.setMultipath(true);
		fittedSlotList  = canBeFitConnection(flow, links, spectrum, rate);
			
		if(fittedSlotList.size() >= 1) {
			return fittedSlotList;
		}
			
		return new ArrayList<Slot>();
	}
	
	
	/**
	 * 
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return
	 */
	public boolean establishConnection(ArrayList<int []> mpaths, ArrayList< ArrayList<Slot> > slotList, ArrayList<Integer> modulation, Flow flow) {
		
		if(mpaths == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		ArrayList<Long> ids = new ArrayList<Long>();
		int i = 0;
		
		for(int []links : mpaths) {
			
			long id = vt.createLightpath(links, slotList.get(i),flow.getModulationLevel(i));
		
			if(id < 0) {
				//TODO
//				for(int []l : mpaths) System.out.println(Arrays.toString(l));
//				System.out.println("Error: invalid ID: "+id);
				return false;
			}
			
//			System.out.println(id);
			
			i++;
			ids.add(id);
		}
			
		if (ids.size() == mpaths.size()) 
		{
			this.updateData(flow, mpaths, ids, slotList, modulation);
			return true;
		} 
		
		return false;
	}
	
	protected void removeCrosstalkInMultipaths(int[] links, ArrayList<Slot> slotList) {
		
		for(int l : links) {
			this.pt.getLink(l).resetCrosstalk(slotList);
        }
	}
	

	@Override
	public void flowDeparture(Flow flow) {
		
		if(!flow.isAccepeted()) {
			return;
		}
	
		if(flow.isMultipath()) {
			
			int n = flow.getMultiSlotList().size();
			
			for(int j = 0; j < n; j++) {
				removeCrosstalkInMultipaths(flow.getLinks(j), flow.getMultiSlotList().get(j));
			}
		}
		else 
		{
			if(!flow.isAccepeted()) return;
			
			removeCrosstalk(flow.getLinks(), flow);
		}
	}
}
