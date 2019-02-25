package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * Paper: Crosstalk-aware cross-core virtual concatenation in spatial division multiplexing elastic optical networks
 * Authors: Zhao and Zhang
 * Published: September 2016
 * 
 * @author trindade
 *
 */
public class CCVCRCSA extends XTFFRCSA {

	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {
		
		setkShortestPaths(flow);

		for(int []links: this.paths) {
			
			if(fitConnection(flow, bitMapAll(links), links)) {
//				System.out.println("accepted: "+flow);
					this.paths.clear();
					return;
			}
			else {
				
				flow.setMultipath(true);
				ArrayList<int[]> lightpathsAvailable = new ArrayList<int[]>();
				ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(links, flow, lightpathsAvailable);
				
				if(!slotList.isEmpty()) {
					
					if(this.establishMultipathConnection(lightpathsAvailable, slotList, flow.getModulationLevels(), flow))
					{
						System.out.println(flow);
						return;
					}
				}
				
				flow.setMultipath(false);
			}
		}
		
		cp.blockFlow(flow.getID());
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
	
	public boolean establishMultipathConnection(ArrayList<int []> mpaths, ArrayList< ArrayList<Slot> > slotList, ArrayList<Integer> modulation, Flow flow) {
		
		if(mpaths == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
//		System.out.println(mpaths.size());
		ArrayList<Long> ids = new ArrayList<Long>();
		int i = 0;
		int pathLength = 0;
		for(int []links : mpaths) {
			
			long id = vt.createLightpath(links, slotList.get(i),flow.getModulationLevel(i));
		
			if(id < 0) {
				System.out.println("Error: invalid ID: "+id+ " " + i);
				return false;
			}
			
			pathLength += getPathLength(links);
			i++;
			ids.add(id);
		}
			
		if (ids.size() == mpaths.size()) 
		{
			flow.setPathLength(pathLength);
			this.updateData(flow, mpaths, ids, slotList, modulation);
			return true;
		} 
		
		return false;
	}
	
	private ArrayList< ArrayList<Slot> > getSetOfSlotsAvailableInEachPath(int[] links, Flow flow, ArrayList<int[]> lightpathsAvailable) {

		ArrayList<ArrayList<Slot>> slotList = getListOfSlotBlockAvailable(links, flow);
		if(!slotList.isEmpty()) {
			
			for(int k = 0; k < slotList.size(); k++) {
//				System.out.println(slotList.get(k));
				lightpathsAvailable.add( links );
			}
//			System.out.println(lightpathsAvailable.size());
			return slotList;
		}
		
		flow.getModulationLevels().clear();

		return new ArrayList<ArrayList<Slot>>();
	}
	
	
	protected ArrayList<ArrayList<Slot>> getListOfSlotBlockAvailable(int[] links, Flow flow) {
		 
		int modulation = chooseModulationFormat(flow.getRate(), links);
		boolean [][]spectrum = bitMapAll(links);
		
		while(modulation >= 0) {
			
			int slots = 0;
			double requestedBandwidthInGHz = ( (double)flow.getRate() / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			ArrayList<ArrayList<Slot>> blockOfSlots = FirstFitBlockOfSlotsAvailable(flow, spectrum, links, demandInSlots, modulation);
			
			for(int i = 0; i < blockOfSlots.size(); i++) {
				slots += blockOfSlots.get(i).size();
			}
			if(slots == demandInSlots) {
					
				if(!flow.isMultipath()) 
				{
					flow.setModulationLevel(modulation);
				}
				else 
				{
					for(int i = 0 ; i < blockOfSlots.size(); i++) flow.addModulationLevel(modulation);
				}
				
				return blockOfSlots;
			}
			
			modulation--;
		}
		
		return new ArrayList<ArrayList<Slot>> ();
	}

	private ArrayList<ArrayList<Slot>> FirstFitBlockOfSlotsAvailable(Flow flow, boolean[][] spectrum, int[] links,
			int demandInSlots, int modulation) {
		
		ArrayList<Integer> priorityCores = new ArrayList<Integer>(Arrays.asList(6,5,4,3,2,1,0));
		ArrayList<ArrayList<Slot>> setOfSlots = new ArrayList<ArrayList<Slot>>();
		int n = 0;
		for(int c = 0; c < priorityCores.size() ; c++) {
			int i = priorityCores.get(c);
			
			ArrayList<Slot> temp = new ArrayList<Slot>();
			for(int j = 0; j < spectrum[i].length; j++) {	
				
				if(spectrum[i][j] == true) 
				{
					temp.add( new Slot(i,j) );
					if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
						n++;
						if(n == demandInSlots) {
							break;
						}
					}
					else temp.clear();
				}
				
				if(Math.abs(spectrum[i].length-j) < (demandInSlots-n)) break;
				
			}
			
			if(!temp.isEmpty()) setOfSlots.add(temp);
			
			if(n == demandInSlots) return setOfSlots;
			
		}
		
		return new ArrayList<ArrayList<Slot>>();
	}
}
