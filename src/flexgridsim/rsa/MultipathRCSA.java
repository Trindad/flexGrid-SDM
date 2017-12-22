package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

public class MultipathRCSA extends SCVCRCSA {
	
	private int TH = 30;
	
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
		
		System.out.println("Connection blocked:"+flow);
		cp.blockFlow(flow.getID());
	}
	
	/**
	 * 
	 * @param flow
	 * @return
	 */
	protected boolean multipathEstablishConnection(Flow flow) {
		
		
		this.setkShortestPaths(flow);
		ArrayList<int[]> paths = this.getkShortestPaths();

		if(paths.size() >= 2)
		{
			int sumOfAvailableSlots = 0;
			ArrayList<int[]> selectedPaths = new ArrayList<int[]>();
			
			for(int i = 0; i < this.paths.size(); i++) {
				
				int sum = pt.getNumSlots();
				
				for(int j = 0; j < this.paths.get(i).length; j++) {
					
					int n = pt.getLink(this.paths.get(i)[j]).getNumFreeSlots();
					
					if(n <= 0 && paths.size() <= 2) 
					{
						return false;
					}
					
					if(n == 0)
					{
						sum = 0;
						break;
					}
					
					if(sum > n) 
					{
						sum = n;
					}
				}
				
				sumOfAvailableSlots += sum;
				
				if(sum > 0) {
					selectedPaths.add(paths.get(i));
				}
			}
			
			//suppose the high level of modulation fitted
			int demandInSlots = (int) Math.ceil(flow.getRate() / ModulationsMuticore.subcarriersCapacity[ModulationsMuticore.numberOfModulations()-1]);
			
			if(sumOfAvailableSlots >= demandInSlots) 
			{
				boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
				
				for(int i = 2; i <=  selectedPaths.size(); i++) {
				
					ArrayList< ArrayList<Slot> > fittedSlotList = new ArrayList< ArrayList<Slot> >();
					int totalRate = flow.getRate();
					int rate = (int) Math.ceil(flow.getRate()/i);
					ArrayList<int[]> p = new ArrayList<int[]>();
					
					for(int j = 0; j < i; j++) {
						
						spectrum = initMatrix(spectrum, pt.getCores(),pt.getNumSlots());
			
						for (int v = 0; v < selectedPaths.get(j).length; v++) {
							
							int src = pt.getLink(selectedPaths.get(j)[v]).getSource();
							int dst = pt.getLink(selectedPaths.get(j)[v]).getDestination();
							bitMap(pt.getLink(src, dst).getSpectrum(), spectrum, spectrum);
						}
						
						
						ArrayList<Slot> temp = fitConnection(flow, spectrum, selectedPaths.get(j), rate);
						
						if(!temp.isEmpty()) {
							
							fittedSlotList.add(temp);
							p.add(selectedPaths.get(j));
							totalRate -= rate;
						}
						else
						{
							if(flow.getModulationLevels().size() >= 1) flow.removeModulationLevel();
						}
						
						if(totalRate < rate) 
						{
							rate = totalRate;
						}
					}
					
					if(fittedSlotList.size() == i) {
						
						if(this.establishConnection(selectedPaths, fittedSlotList, flow.getModulationLevels(), flow))
						{
							paths.clear();
							return true;
						}
					}
					
				}
				
			}
			
			paths.clear();
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
				
				System.out.println("Invalid ID");
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
		else {
			if(!flow.isAccepeted()) return;
			
			int []links = flow.getLinks();
			
			for(int i = 0; i < links.length; i++) {
				pt.getLink(links[i]).updateCrosstalk();
			}
		}
	}
}
