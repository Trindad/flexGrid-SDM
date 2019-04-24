package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Arrays;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.Slot;

/**
 * Dynamic Service Provisioning in Elastic Optical Networks With Hybrid Single-/Multi-Path Routing
 *	Zuqing Zhu, Senior Member, IEEE, Wei Lu, Liang Zhang, and Nirwan Ansari, Fellow, IEEE
 * 
 * @author trindade
 *
 */

public class MultipathOneRCSA extends MultipathRCSA {
		
		private ArrayList<Integer> cores = new  ArrayList<Integer> ();
		private int g = 2;//allocation granularity
		static int rate;
		
		public void flowArrival(Flow flow) {
			
			kPaths = 3;
			setkShortestPaths(flow);
			
			
			cores.addAll( new ArrayList<>(Arrays.asList(6,5,4,3,2,1,0)) );
			ArrayList< ArrayList<Slot> > fittedSlotList = new ArrayList< ArrayList<Slot> >();
			rate = flow.getRate();
			ArrayList<int []> p = new ArrayList<int []>(); 
			int i = 0;
			for(int []links : paths) {
					
				System.out.println("PATH INDEX: "+i+" "+rate);
					
					ArrayList<ArrayList<Slot>> slots = multiCoresAllocation(flow, links, rate);
					
					if(!slots.isEmpty()) {
						fittedSlotList.addAll(slots);
						p.add(links);
						
					}
					if(rate == 0) {
					
						
						if(!flow.isMultipath() && establishSinglePathConnection(links, fittedSlotList.get(0), flow.getModulationLevel(), flow))
						{
							System.out.println("Single: "+flow+" p(i): "+i);
							return;
						}

						flow.setMultiSlotList(fittedSlotList);
					
						if(flow.isMultipath() && multipathEstablishConnection(flow))
						{
							System.out.println("Multipath: "+flow);
							return;	
						}
						
					}
					
					i++;
			}
		
			flow.setMultipath(false);
			System.out.println("Blocked: "+flow);
			this.paths.clear();
			cp.blockFlow(flow.getID());
			
		}
		
		public boolean establishSinglePathConnection(int []links,ArrayList<Slot> fittedSlotList, int modulation, Flow flow) {
			
			
			if(establishConnection(links, fittedSlotList, flow.getModulationLevel(), flow)) {
					return true;
			}
			
			
			return false;
		}
		
		
		protected boolean multipathEstablishConnection(Flow flow) {

			ArrayList<int[]> lightpaths = new ArrayList<int[]>();
			ArrayList<ArrayList<Slot>> slotList = getSetOfSlotsAvailableInEachPath(getkShortestPaths(), flow, lightpaths);
			
			if(!slotList.isEmpty()) {

				if(this.establishConnection(lightpaths, slotList, flow.getModulationLevels(), flow))
				{
					return true;
				}
			}
			
			return false;
		}

		private ArrayList<ArrayList<Slot>> multiCoresAllocation(Flow flow, int[] links, int rate) {
			
			boolean[][] spectrum = bitMapAll(links);
			ArrayList<ArrayList<Slot>> fittedSlotList = multipathFit(flow, links, spectrum);
			
			return fittedSlotList;
		}
		
		public ArrayList<ArrayList<Slot>> multipathFit(Flow flow, int[]links, boolean [][]spectrum) {
			
			ArrayList<ArrayList<Slot>> fittedSlotList = new ArrayList<ArrayList<Slot>>();
			int modulation = chooseModulationFormat(rate, links);
			
			double requestedBandwidthInGHz = Math.ceil( ((double)rate) / ((double)modulation + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
	
			if(rate == flow.getRate()) {
				fittedSlotList.add(FitPolicy(flow, spectrum, links, demandInSlots, modulation));
				
				if(rate == flow.getRate() && fittedSlotList.get(0).size() == demandInSlots && fittedSlotList.size() == 1) 
				{
					flow.setMultipath(false);
					flow.setModulationLevel(modulation);
					rate = 0;
					return fittedSlotList;
				}
			}

			if((demandInSlots-1) < g) return new ArrayList<>();
				
			fittedSlotList = new ArrayList<ArrayList<Slot>>();
			fittedSlotList = fitPolicyModified(flow, spectrum, links, demandInSlots, modulation);
	
			if(!fittedSlotList.isEmpty()) {
				
				flow.setMultipath(true);
				flow.addModulationLevel(modulation);
				System.out.println("SIZE: "+fittedSlotList.size()+" RATE: "+flow.getRate()+" SLOTS "+demandInSlots);
				
				for(ArrayList<Slot> slots : fittedSlotList) {
					System.out.println(slots+ " demand: "+demandInSlots+" rate: "+rate+" modulation: "+modulation);
					
					rate -= (ModulationsMuticore.subcarriersCapacity[0] * (slots.size()-1) * ( modulation + 1));
					
					flow.setMultipath(links);
				}
				
				return fittedSlotList;
				
				
			}
			
			return new ArrayList<ArrayList<Slot>>();
		}
		
		public ArrayList<ArrayList<Slot>> fitPolicyModified(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {
			
			ArrayList<ArrayList<Slot>> list = new ArrayList<ArrayList<Slot>>();
			
			int lim = 0;
			
			for(int c  : cores) 
			{
				int i = c;
			
				ArrayList<Slot> temp = new ArrayList<Slot>();
				for(int j = 0; j < pt.getNumSlots(); j++) {	
				
					if(spectrum[i][j]) temp.add( new Slot(i,j) );
					
					if( temp.isEmpty())continue;
					
					int a = (j+1) > (pt.getNumSlots()-1) ? j : j+1;
					if(temp.size() < g && spectrum[i][j] && a > j && g < demandInSlots) continue;
					if(temp.size() < g && spectrum[i][j] == false && a > j) 
					{
						temp.clear();
						continue;
					}
					
					if(temp.size() < demandInSlots && a > j && spectrum[i][a]) continue;
					int n = list.size();
					
					if((!spectrum[i][j] || !spectrum[i][a] ) && temp.size() >= g) {
						
						if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {
							if(list.isEmpty()) {
								System.out.println("HERE: "+demandInSlots+" "+temp+"  "+lim);
								list.add(new ArrayList<Slot>(temp));
								if(temp.size() == demandInSlots) {
									
									return list;	
								}
								lim = lim + (temp.size()-1);
//								System.out.println(" "+lim);
							}
							else {
								
								lim = lim + (temp.size()-1);
								if(lim == (demandInSlots) || temp.size() == demandInSlots) {
									System.out.println("HERE*: "+demandInSlots+" "+temp+" "+lim);
									
									if(temp.size() == demandInSlots) list.clear();
									list.clear();
									list.add(new ArrayList<Slot>(temp));
									return list;
								}
								else if(lim < (demandInSlots-1) ) {
									list.add(new ArrayList<Slot>(temp));
								}
							}
						}
						temp.clear();
					}	
					
					if(n < list.size() || !spectrum[i][j] || !spectrum[i][a] || !temp.isEmpty()) temp.clear();
				}
				
			}
			 return list;
		}
		
		public ArrayList<Slot> FitPolicy(Flow flow, boolean [][]spectrum, int[] links, int demandInSlots, int modulation) {

			for(int c = (cores.size()-1); c >= 0 ; c--) {
				
				int i = cores.get(c);
				ArrayList<Slot> temp = new ArrayList<Slot>();
				for(int j = 0; j < pt.getNumSlots(); j++) {	
					
					if(spectrum[i][j] == true) 
					{
						temp.add( new Slot(i,j) );
					}
					else {
						
						temp.clear();
						if(Math.abs(spectrum[i].length-j) < demandInSlots) break;
					}
					
					if(temp.size() == demandInSlots) {
						
						if(cp.CrosstalkIsAcceptable(flow, links, temp, ModulationsMuticore.inBandXT[modulation])) {

							return temp;
						}
						
						break;
					}
				}
			}
		    
			return new ArrayList<Slot>();
		}
	
}

