package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Slot;

public class MultipathRCSA extends SCVCRCSA {
	
	private int TH = 100;
	
	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {

		
		//Try to assign in a normal way
		if(this.runRCSA(flow)) 
		{
			return;
		}
		
		if(flow.getRate() > TH)
		{
//			System.out.println(flow);
			if(this.multipathEstablishConnection(flow)) 
			{
				this.paths.clear();
//				System.out.println("Connection accepted using multipath:"+flow);
				return;
			}
		}
		
//		System.out.println("Connection blocked:"+flow);
		this.paths.clear();
		cp.blockFlow(flow.getID());
		
	}

	protected boolean multipathEstablishConnection(Flow flow) {

		this.kPaths = 5;
		this.setkShortestPaths(flow);
		
		if(getkShortestPaths().size() <= 1)
		{
			return false;
		}
		
		//suppose the high level of modulation fitted, ignoring the distance between source and destination
		ArrayList<int []> nPaths = new ArrayList<int []>();
		ArrayList<ArrayList<Slot>> slotList = new ArrayList<ArrayList<Slot>>();
		int count = 0;
		int rate = flow.getRate()/2;
		for(int []links : getkShortestPaths()) {
			
			boolean [][]spectrum = bitMapAll(links);
			int i = 0;
			for(boolean []core : spectrum) {
				ArrayList<Slot> slots = fitConnection(flow, core, i, links, rate);
				if(!slots.isEmpty()) 
				{
//					System.out.println(slots.size());
					slotList.add(slots);
					nPaths.add(links);
					count++;
					break;
				}	
				
				i++;
			}
			
			if(count == 2) 
			{
//					System.out.println(fittedSlotList.size() + " "+ nPaths.size() + " " + flow.getModulationLevels().size());
				if(this.establishConnection(nPaths, slotList, flow.getModulationLevels(), flow))
				{
					return true;
				}
				
				slotList.remove(0);
				nPaths.remove(0);
				flow.getModulationLevels().remove(0);
				count--;
			}
		}
		
		
		return false;
	}

	protected void updateData(Flow flow, ArrayList<int []> mpaths, ArrayList<Long> ids,  ArrayList< ArrayList<Slot> > fittedSlotList, ArrayList<Integer> modulation) {
		
		flow.setMultipath(true);
		flow.setLinks(mpaths);
		flow.setSlotListMultipath(fittedSlotList);
		flow.setModulationLevels(modulation);
		flow.setLightpathsID(ids);
		
		ArrayList<LightPath> lps = new ArrayList<LightPath>();
		
		for(int i = 0; i < mpaths.size(); i++) {
			
			lps.add(vt.getLightpath(ids.get(i)));

			for (int j = 0; j < mpaths.get(i).length; j++) {
				
	            pt.getLink(mpaths.get(i)[j]).reserveSlots(fittedSlotList.get(i));
	        }
			
			//update cross-talk
			updateCrosstalk(mpaths.get(i), fittedSlotList.get(i), modulation.get(i));
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
	public ArrayList<Slot> fitConnection(Flow flow, boolean []spectrum, int core, int []links, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		flow.setMultipath(true);
		fittedSlotList  = canBeFitConnection(flow, links, spectrum, core, rate);
			
		if(fittedSlotList.size() >= 1) return fittedSlotList;
			
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
	

	@Override
	public void flowDeparture(Flow flow) {
		
		if(!flow.isAccepeted()) return;
	
		if(flow.isMultipath()) {
			ArrayList<int[]> mpaths = new ArrayList<int[]>();
			
			for(int j = 0; j < mpaths.size(); j++) {
				
				int []links = flow.getLinks(j);
				
				for(int i = 0; i < links.length; i++) {
					pt.getLink(links[i]).updateCrosstalk(flow.getSlotList());
				}
			}
		}
		else 
		{
			if(!flow.isAccepeted()) return;
			
			int []links = flow.getLinks();
			
			for(int i = 0; i < links.length; i++) {
				pt.getLink(links[i]).updateCrosstalk(flow.getSlotList());
			}
		}
	}
}
