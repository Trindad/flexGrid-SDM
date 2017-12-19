package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathRCSA extends SCVCRCSA {
	
	private int TH = 70;
	
	/**
	 * Traditional algorithm RCSA using First-fit 
	 * @param Flow
	 */
	public void flowArrival(Flow flow) {

		//Try to assign in a normal way
		if(this.runRCSA(flow)) {
			return;
		}
		
		if(flow.getRate() >= TH) {
			
			if(this.multipathEstablishConnection(flow)) {
				System.out.println("Connection accepted using multipath:"+flow);
				return;
			}
		}
		
		paths.clear();
		System.out.println("Connection blocked:"+flow);
		cp.blockFlow(flow.getID());
	}
	

	/**
	 * 
	 * @param flow
	 * @return
	 */
	protected boolean multipathEstablishConnection(Flow flow) {
		
		int nSlotsAvailable = 0;
		this.setkShortestPaths(flow);
		ArrayList<int[]> paths = this.getkShortestPaths();

		if(paths.size() >= 2)
		{
			//Check if there are available slots to assign the request
			for(int i = 0; i < this.paths.size(); i++) {
				
				for(int j = 0; j < this.paths.get(i).length; j++) {
					
					nSlotsAvailable += pt.getLink(this.paths.get(i)[j]).getNumFreeSlots();
//					System.out.println(pt.getLink(this.kPaths[i][j]).getNumFreeSlots());
//					System.out.print(" "+this.paths.get(i)[j]);
				}
				
//				System.out.println("\n************");
			}
			
			
			//suppose the high level of modulation fitted
			int demandInSlots = (int) Math.ceil(flow.getRate() / ModulationsMuticore.subcarriersCapacity[ModulationsMuticore.numberOfModulations()-1]);
			
			if(nSlotsAvailable >= demandInSlots) 
			{
				
				boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
				
				for(int i = 2; i < this.paths.size(); i++) {
					
					ArrayList< ArrayList<Slot> > fittedSlotList = new ArrayList< ArrayList<Slot> >();
					int totalRate = flow.getRate();
					int rate = (int) Math.ceil(flow.getRate()/i);
					ArrayList<int[]> p = new ArrayList<int[]>();
					
					for(int j = 0; j < i; j++) {
						
						spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
						
						ArrayList<Slot> temp = fitConnection(flow, spectrum, paths.get(j), rate);
						
						if(!temp.isEmpty()) {
							fittedSlotList.add(temp);
							paths.add(paths.get(j));
						}
						else {
							break;
						}

						totalRate -= rate;
						
						if(totalRate < rate) 
						{
							rate = totalRate;
						}
					}
					
					if(fittedSlotList.size() == i) {
							
						if(establishConnection(paths, fittedSlotList, flow.getModulationLevel(), flow))
						{
							return true;
						}
					}
					
				}
				
			}
		}
		
		return false;
	}

	protected void updateData(Flow flow, ArrayList<int []> paths, ArrayList<Long> ids,  ArrayList< ArrayList<Slot> > fittedSlotList, int modulation) {
		
		flow.setLinks(paths);
		flow.setSlotListMultipath(fittedSlotList);
		flow.setModulationLevel(modulation);
		flow.setLightpathID(ids);
		
		for(int i = 0; i < paths.size(); i++) {
			
			LightPath lps = vt.getLightpath(ids.get(i));
			cp.acceptFlow(flow.getID(), lps);

			for (int j = 0; j < paths.get(i).length; j++) {
				
	            pt.getLink(paths.get(i)[j]).reserveSlots(fittedSlotList.get(i));
	        }
			
			//update cross-talk
			updateCrosstalk(paths.get(i));
		}
	}
	
	/**
	 * 
	 * @param flow
	 * @param links
	 * @param spectrum
	 * @param core
	 * @return
	 */
	public ArrayList<Slot> canBeFitConnection(Flow flow, int[]links, boolean []spectrum, int core) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		double xt = 0.0f;
		int modulation = chooseModulationFormat(flow, links);
		
		while(modulation >= 0)
		{
			double subcarrierCapacity = ModulationsMuticore.subcarriersCapacity[modulation];
			int demandInSlots = (int) Math.ceil(flow.getRate() / subcarrierCapacity);
			
			xt = pt.getSumOfMeanCrosstalk(links, core);//returns the sum of cross-talk	
			
			if(xt == 0 || (xt < ModulationsMuticore.inBandXT[modulation]) ) {

				fittedSlotList = this.FirstFitPolicy(spectrum, core, links, demandInSlots);
				
				if(fittedSlotList.size() == demandInSlots) {
					
					if(fittedSlotList.size() == demandInSlots) {
						
						return fittedSlotList;
					}
				}
				
				fittedSlotList.clear();
			}
			
			modulation--;
		}
		
		return fittedSlotList;
	}
	
	/**
	 * 
	 * @param flow
	 * @param spectrum
	 * @param links
	 * @return
	 */
	public ArrayList<Slot> fitConnection(Flow flow, boolean [][]spectrum, int[] links, int rate) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
				
		for (int i = 1; i < spectrum.length; i++) {
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i);
			
			if(!fittedSlotList.isEmpty()) return fittedSlotList;
			
		}
		
		fittedSlotList  = canBeFitConnection(flow, links, spectrum[0], 0);
		
		if(!fittedSlotList.isEmpty()) return fittedSlotList;
		
		return fittedSlotList;
	}
	
	
	/**
	 * 
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return
	 */
	public boolean establishConnection(ArrayList<int []> paths, ArrayList< ArrayList<Slot> > slotList, int modulation, Flow flow) {
		
		if(paths == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		ArrayList<Long> ids = new ArrayList<Long>();
		
		for(int i = 0; i < paths.size(); i++) {

			long id = vt.createLightpath(paths.get(i), slotList.get(i) ,flow.getModulationLevel());
			
			if(id < 0) return false;
			
			ids.add(id);
		}
			
		if (ids.size() == paths.size()) 
		{
			this.updateData(flow, paths, ids, slotList, modulation);
			
			System.out.println("Connection accepted:"+flow);
			return true;
		} 
		
		return false;
	}
}
