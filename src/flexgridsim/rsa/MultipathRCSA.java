package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Collections;

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
		if(this.runRCSA(flow)) 
		{
			return;
		}
		
		//try to allocate using multipath
		if(flow.getRate() > TH)
		{
			if(this.multipathEstablishConnection(flow)) 
			{
//				System.out.println("Connection accepted using multipath:"+flow);
				return;
			}
		}
		
//		System.out.println("Connection blocked:"+flow);
		
		cp.blockFlow(flow.getID());
		
	}
	
	protected boolean[][]getAvaibleSlotsInLightpath(boolean[][] spectrum, int[] selectedPath) {
		
		spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
		
		for (int v = 0; v < selectedPath.length; v++) {
			
			int src = pt.getLink(selectedPath[v]).getSource();
			int dst = pt.getLink(selectedPath[v]).getDestination();
			bitMap(pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
		}
		
		return spectrum;
	}
	
	protected int getNewRate(Flow flow, int index, double percentage) {
		
		return (int) Math.abs(flow.getRate() - ( (double)flow.getRate() * percentage) );
	}
	
	protected boolean tryToSlitRateInMultipaths(Flow flow, int i, int rate, ArrayList<int[]> selectedPaths, ArrayList<Integer> indices) {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		int totalRate = flow.getRate();
		ArrayList< ArrayList<Slot> > fittedSlotList = new ArrayList< ArrayList<Slot> >();
		ArrayList<int[]> p = new ArrayList<int[]>();
		int j = 0;
		
		while( j < i) {
			
			int k = 0, n = p.size();
			
			while(k < selectedPaths.size() && p.size() < n) 
			{
				if(pt.getNumberOfAvailableSlots(selectedPaths.get(k)) >= rate) {
					
					spectrum = this.getAvaibleSlotsInLightpath(spectrum, selectedPaths.get(k));
					flow.setMultipath(true);
					ArrayList<Slot> temp = this.fitConnection(flow, spectrum, selectedPaths.get(k), rate);
					
					if(!temp.isEmpty() && !p.contains(selectedPaths.get(k))) {

						fittedSlotList.add(temp);
						p.add(selectedPaths.get(k));
						
						if(totalRate < rate) 
						{
							rate = totalRate;
						}
						else 
						{
							totalRate -= rate;
						}
						
						break;
						
					}
					else if(flow.getModulationLevels().size() >= 1) 
					{
						flow.removeModulationLevel();
						flow.setMultipath(false);
					}
				}
				
				k++;
			}
			
			j++;
		}
		
		if(fittedSlotList.size() == i) {
			
			if(this.establishConnection(p, fittedSlotList, flow.getModulationLevels(), flow))
			{
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean tryToAssign(Flow flow, ArrayList<int[]> selectedPaths, ArrayList<Integer> nSlotsAvailable ) {
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for(int i = 0; i < selectedPaths.size(); i++) {
			indices .add(i);
		}
				
		indices.sort((a,b) -> nSlotsAvailable.get(a) - nSlotsAvailable.get(b));
		Collections.reverse(indices);
		
		for(int i = 2; i <= indices.size(); i++) {

			double y = i;

			while(y == 2) {
				
				y--;
				double percentage = 1.0/y;
				int rate = this.getNewRate(flow, i, percentage);
				
				if(rate <= 0) continue;
				
				if( this.tryToSlitRateInMultipaths(flow, i, rate, selectedPaths, indices)) return true;
			}
		}
			
		return false;
	}
	

	/**
	 * 
	 * @param sumOfAvailableSlots
	 * @return
	 */
	private int getPathsCandidates(ArrayList<int[]> selectedPaths, int demandInSlots, ArrayList<Integer> nSlotsAvailable) {

		int sumOfAvailableSlots = 0;
		
		for(int i = 0; i < this.paths.size(); i++) {
			
			int sum = pt.getNumSlots();
			
			for(int j = 0; j < this.paths.get(i).length; j++) {
				
				int n = pt.getLink(this.paths.get(i)[j]).getNumFreeSlots();
				
				if(n <= 0)
				{
					sum = 0;
					break;
				}
				else if(sum > n) 
				{
					sum = n;
				}
			}
			
			sumOfAvailableSlots += sum;
			
			if(sum > 0 && sum >= demandInSlots) 
			{
				nSlotsAvailable.add(sum);
				selectedPaths.add(this.paths.get(i));
				
				if(Math.abs(this.paths.size()-i) <= 1 && selectedPaths.size() < 1) return 0;
			}
		}
		
		return sumOfAvailableSlots;
	}
	
	/**
	 * 
	 * @param flow
	 * @return
	 */
	protected boolean multipathEstablishConnection(Flow flow) {

		this.setkShortestPaths(flow);

		if(this.paths.size() >= 2)
		{
			//suppose the high level of modulation fitted, ignoring the distance between source and destination
			int demandInSlots = (int) Math.ceil( ( (double)flow.getRate() / (double)ModulationsMuticore.subcarriersCapacity[ModulationsMuticore.numberOfModulations()-1])/(double)paths.size());
			
			ArrayList<Integer> nSlotsAvailable = new ArrayList<Integer>();
			ArrayList<int[]> selectedPaths = new ArrayList<int[]>();
			int sumOfAvailableSlots = this.getPathsCandidates(selectedPaths, demandInSlots, nSlotsAvailable);
			
			if(sumOfAvailableSlots >= demandInSlots && selectedPaths.size() >= 2) 
			{
				return this.tryToAssign(flow, selectedPaths, nSlotsAvailable);
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
			updateCrosstalk(mpaths.get(i));
		}
		
		if(!cp.acceptFlow(flow.getID(), lps)) {
			throw (new IllegalArgumentException());
		}
	}
	
	/**
	 * Search to a core that has available slots and considering the cross-talk threshold
	 * @param links 
	 * @param spectrum 
	 * @return list of available slots
	 */
	protected ArrayList<Slot> FirstFitPolicy(boolean []spectrum, int core, int[] links, int demandInSlots) {
		
		ArrayList<Slot> setOfSlots = new ArrayList<Slot>();
		
		if (spectrum.length >= demandInSlots) {

			for(int i = 0; i < spectrum.length; i++) {
				
				if(spectrum[i] == true) {
					
					setOfSlots.add( new Slot(core,i) );
				}
			
				if(setOfSlots.size() == demandInSlots) return setOfSlots;
			}
	    }
		
		return setOfSlots;
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
			
			fittedSlotList  = canBeFitConnection(flow, links, spectrum[i], i, rate);
			
			if(!fittedSlotList.isEmpty()) return fittedSlotList;
			
		}
		
		fittedSlotList  = canBeFitConnection(flow, links, spectrum[0], 0, rate);
		
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
	public boolean establishConnection(ArrayList<int []> mpaths, ArrayList< ArrayList<Slot> > slotList, ArrayList<Integer> modulation, Flow flow) {
		
		if(mpaths == null || flow == null || slotList.isEmpty()) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		ArrayList<Long> ids = new ArrayList<Long>();
		
		for(int i = 0; i < mpaths.size(); i++) {

			long id = vt.createLightpath(mpaths.get(i), slotList.get(i) ,flow.getModulationLevel(i));
			
			if(id < 0) {
				
				System.out.println("Error: invalid ID: "+id);
				return false;
			}
			
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
					pt.getLink(links[i]).updateCrosstalk();
				}
			}
		}
		else 
		{
			if(!flow.isAccepeted()) return;
			
			int []links = flow.getLinks();
			
			for(int i = 0; i < links.length; i++) {
				pt.getLink(links[i]).updateCrosstalk();
			}
		}
	}
}
