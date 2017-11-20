package flexgridsim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.rsa.ImageRCSA;

/**
 * 
 * @author trindade
 * 
 * First-fit allocation
 * 
 * First-last-fit based on cores
 * First-last fit based on slots
 * 
 * Last-fit allocation
 *
 */
public class ResourceAssignment {
	
	private ImageRCSA rsa;
	private int numberOfAvailableSlots = 0;
	private ArrayList< ArrayList<Slot> > mergedRegions;

	public ResourceAssignment(ImageRCSA rsa) {
		super();
		this.rsa = rsa;
	}

	public ImageRCSA getRsa() {
		return rsa;
	}

	public void setRsa(ImageRCSA rsa) {
		this.rsa = rsa;
	}
	
	public boolean runFirstFit(Slot [][]channel, ArrayList< ArrayList<Slot> > regions, ArrayList<Integer> coreIndices, int demandInSlots, int[] links, Flow flow) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		
		if(coreIndices.isEmpty()) return false;
		
		nSlots = numberOfSlotsPerCore(demandInSlots, 1);
		ArrayList< ArrayList<Integer> > cont = getGroupContiguosCore(coreIndices);
		
//		for(int i = 0; i < mergedRegions.size(); i++)
//		{
//			System.out.print(mergedRegions.get(i));
//		}
//		System.out.println("nSlots: "+nSlots);
		
		regions.sort(Comparator.comparing(ArrayList::size));
		
		/**
		 * Try to assignment the request in just one core 
		 */
		for(int u = 0; u < regions.size() ; u++) {
				
			if(regions.get(u).size() < demandInSlots) {
				continue;
			}
			
			int i = regions.get(u).get(0).c;
			int shift = 0;
			
			for(int j = shift; j < channel[i].length; j++) {
				
				if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
				{
					fittedSlotList.add(channel[i][j]);
				}
				else if(channel[i][j] == null) break;
				
				if(fittedSlotList.size() == demandInSlots) 
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) {
//							System.out.println("First-fit: "+demandInSlots+" tam: "+fittedSlotList.size());
//							System.out.println(fittedSlotList);
						return true;
					}
			
					fittedSlotList.clear();
				}
			}
			
			fittedSlotList.clear();	
		}

		for(int n = 2; n <= regions.size(); n++) {
			
			for(int u = 0; u < cont.size() ; u++) {
				int nCores =  cont.get(u).size();
				nSlots = numberOfSlotsPerCore(demandInSlots, n);
				
				for(int i = 0; i < nCores; i++) {
					
					for(int j = 0; j < channel[i].length; j++) {
						
						if(channel[i][j] != null && fittedSlotList.size() < nSlots)
						{
							fittedSlotList.add(channel[i][j]);
						}
						else if(channel[i][j] == null) break;
		
						if(fittedSlotList.size() == nSlots) 
						{
							if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
							{
								return true;
							}
							
							fittedSlotList.clear();	
						}
					}
				}
			}
			
			fittedSlotList.clear();	
		}
		
		return false;
	}
	
	public ArrayList< ArrayList<Integer> > getGroupContiguosCore(ArrayList<Integer> coreIndices) {
		
		ArrayList< ArrayList<Integer> > contiguosCore = new ArrayList< ArrayList<Integer> >();
		
		int i = 0;
		contiguosCore.add(new ArrayList<Integer>());
		contiguosCore.get(i).add( coreIndices.get(0) );
		
		for(int w = 1; w < coreIndices.size(); w++)
		{
			if(Math.abs(coreIndices.get(w) - coreIndices.get(w-1)) == 1) 
			{
				contiguosCore.get(i).add( coreIndices.get(w) );
			}
			else
			{
				i++;
				contiguosCore.add(new ArrayList<Integer>());
				contiguosCore.get(i).add( coreIndices.get(w) );
			}
		}
		
		return contiguosCore;
	}
	
	
	public boolean runLastFit(Slot [][]channel, ArrayList< ArrayList<Slot> > regions, ArrayList<Integer> coreIndices, int demandInSlots, int[] links, Flow flow) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();

		int nSlots = numberOfSlotsPerCore(demandInSlots, 1);
		
		if(coreIndices.isEmpty()) return false;
		
		nSlots = numberOfSlotsPerCore(demandInSlots, 1);
		ArrayList< ArrayList<Integer> > cont = getGroupContiguosCore(coreIndices);
		
//		for(int i = 0; i < mergedRegions.size(); i++)
//		{
//			System.out.print(mergedRegions.get(i));
//		}
//		System.out.println("nSlots: "+nSlots);
		
		regions.sort(Comparator.comparing(ArrayList::size));
		
		/**
		 * Try to assignment the request in just one core 
		 */
		for(int u = 0; u < regions.size() ; u++) {
	
			if(regions.get(u).size() < nSlots) {
				continue;
			}
			
			int i = regions.get(u).get(0).c;
			for(int j = (channel[i].length-1); j >= 0; j--) {
				
				if(channel[i][j] != null && fittedSlotList.size() < nSlots)
				{
					fittedSlotList.add(channel[i][j]);
				}
				else if(channel[i][j] == null) break;

				if(fittedSlotList.size() == nSlots) 
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
					{
//							System.out.println("Last-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//							System.out.println(fittedSlotList);
						return true;
					}
					
					fittedSlotList.clear();	
				}
			}
			
			fittedSlotList.clear();
		}
		
		for(int n = 2; n <= regions.size() ; n++) {
			
			for(int u = cont.size()-1; u >= 0 ; u--) {
				
				int nCores =  cont.get(u).size();
				nSlots = numberOfSlotsPerCore(demandInSlots, n);
				
				for(int i = nCores-1; i >= 0; i--) {
					
					for(int j = (channel[i].length-1); j >= 0; j--) {
						
						if(channel[i][j] != null && fittedSlotList.size() < nSlots)
						{
							fittedSlotList.add(channel[i][j]);
						}
						else if(channel[i][j] == null) break;
		
						if(fittedSlotList.size() == nSlots) 
						{
							if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
							{
								return true;
							}
							
							fittedSlotList.clear();	
						}
					}
				}
			}
			fittedSlotList.clear();	
		}
		return false;
	}

	@SuppressWarnings("unused")
	public boolean lastFit(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		
		Slot [][]channel = mergeRegions(listOfRegions);
		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < mergedRegions.size(); i++)
		{
			if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= nSlots) coreIndices.add(i);
		}
		
		
		return this.runLastFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
	}

	@SuppressWarnings("unused")
	public boolean firstFit(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		
		for(int i = 0; i < mergedRegions.size(); i++)
		{
			if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= nSlots) coreIndices.add(i);
		}
		
		return this.runFirstFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
	}
	
	@SuppressWarnings("unused")
	public boolean firstLastFit(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {

		int numberOfSlots = 30;
		int numberOfCores = (int) Math.floor(rsa.getNumberOfCores()/3.0f);
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		boolean established = false;
		
		
		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >(mergedRegions);
		
		for(int i = 0; i < 3; i++)
		{
			if(!regions.get(i).isEmpty() && regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, (numberOfCores-1)))
			{
				regions.add(mergedRegions.get(i));
				coreIndices.add(i);
			}		
		}
		
		established = this.runFirstFit(channel, regions, coreIndices, demandInSlots, links, flow);
		
		if(established == false && demandInSlots >= numberOfSlots )
		{
			coreIndices.clear(); 
			regions = new ArrayList< ArrayList<Slot> >(mergedRegions);	
			for(int i = 3; i < 5; i++)
			{
				if(!regions.get(i).isEmpty() && regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, (rsa.getNumberOfCores()-(numberOfCores*2))))
				{
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}	
			}
			
			established = this.runLastFit(channel, regions, coreIndices, demandInSlots, links, flow);
			
		}
		
		
		if(demandInSlots >= (numberOfSlots*2) && established == false)
		{	
			coreIndices.clear(); 
			regions = new ArrayList< ArrayList<Slot> >(mergedRegions);
			for(int i = 5; i < 7; i++)
			{
				if(!regions.get(i).isEmpty() && regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores()-(numberOfCores*2)))
				{
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}		
			}
			
			return this.runFirstFit(channel, regions, coreIndices, demandInSlots, links, flow);
		}
		
		return established;
	}

	@SuppressWarnings("unused")
	public boolean firstLastFitSlots(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {

		int numberOfSlots = 30;
		int numberOfCores = rsa.getNumberOfCores();
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		int limitSlots = (int) Math.floor(rsa.getNumberOfSlots()/3.0f);
		boolean established = false;
		
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >(mergedRegions);

		for(int i = 0; i < regions.size(); i++)
		{
			if(!regions.get(i).isEmpty() && regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
			{
				for(int j = regions.get(i).size()-1; j >= 0; j--) {
					
					if( !regions.get(i).isEmpty() && regions.get(i).get(j).s > limitSlots) 
					{
						regions.get(i).remove(regions.get(i).get(j));
						channel[i][j] = null;
					}
				}
				if(!regions.get(i).isEmpty()) coreIndices.add(i);
			}	
				
		}
		
		established = this.runFirstFit(channel, regions, coreIndices, demandInSlots, links, flow);
		
//		System.out.println(established);
		
		if(established == false && demandInSlots >= numberOfSlots )
		{
			coreIndices.clear(); 
			regions = new ArrayList< ArrayList<Slot> >(mergedRegions);
			
			for(int i = 0; i < regions.size(); i++)
			{
				if(regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
				{
					for(int j = regions.get(i).size()-1; j >= 0; j--) {
						
						if( !regions.get(i).isEmpty() && regions.get(i).get(j).s <= limitSlots ) 
						{
							regions.get(i).remove(regions.get(i).get(j));
							channel[i][j] = null;
						}
					}
					if(!regions.get(i).isEmpty()) coreIndices.add(i);
				}	
			}
			
			established = this.runLastFit(channel, regions, coreIndices, demandInSlots, links, flow);
		}
		
		
//		if(established == false && demandInSlots >= (numberOfSlots*2))
//		{
//			coreIndices.clear(); 
//			regions = new ArrayList< ArrayList<Slot> >(mergedRegions);
//			
//			for(int i = 0; i < regions.size(); i++)
//			{
//				if(regions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
//				{
//					for(int j = regions.get(i).size()-1; j >= 0; j--) {
//						
//						if(!regions.get(i).isEmpty() && regions.get(i).get(j) != null && regions.get(i).get(j).s <= (limitSlots*2)) {
//							regions.get(i).remove(regions.get(i).get(j));
//							channel[i][j] = null;
//						}
//					}
//					if(!regions.get(i).isEmpty()) coreIndices.add(i);
//				}
//					
//			}
//			
//			established = this.runFirstFit(channel, regions, coreIndices, demandInSlots, links, flow);
//			System.out.println(established);
//		}
		
		return established;
	}
	
	public Slot [][]mergeRegions(HashMap<Integer,ArrayList<Slot>> listOfRegions) {
		 
		numberOfAvailableSlots = 0;
		Slot [][]channel = new Slot[rsa.getNumberOfCores()][rsa.getNumberOfSlots()];
		mergedRegions = new ArrayList< ArrayList<Slot> >();
		
		for	(int i = 0; i < rsa.getNumberOfCores(); i++) {
			for (int j = 0; j < rsa.getNumberOfSlots(); j++) {
				channel[i][j] = null;
			}
			
			mergedRegions.add( new ArrayList<Slot>() );
		}
		
		for (Integer key : listOfRegions.keySet()) {
			
			for (Slot s : listOfRegions.get(key)) {
				channel[s.c][s.s] = s;
				mergedRegions.get(s.c).add(s);
				numberOfAvailableSlots++;
			}
		}
		
		return channel;
	}
	
	public int numberOfSlotsPerCore(int demand, int cores) {
		
		return (int) Math.ceil((double)demand/(double)cores);
	}
	
	public int getNumberOfAvailableSlots() {
		return this.numberOfAvailableSlots;
	}
}
