package flexgridsim;

import java.util.ArrayList;
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
		
		
		/**
		 * Try to assignment the request in just one core 
		 */
		for(int u = 0; u < cont.size() ; u++) {
			
			int nCores =  cont.get(u).size();
			
			for(int w = 0; w < nCores; w++) {
				int i = cont.get(u).get(w);
				int shift = 0;
				
				for(int j = shift; j < channel[i].length; j++) {
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
					{
						fittedSlotList.add(channel[i][j]);
					}
//					else if(channel[i][j] == null) break;
					
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
		}

		for(int u = 0; u <= cont.size()-1 ; u++) {
			
			int nCores =  cont.get(u).size();
			ArrayList<Integer> temp = new ArrayList<Integer>();
			
			for(int w = 0; w < nCores; w++) {
				temp.add(cont.get(u).get(w));
			}
			
			int w = 0, n = 1;
			nSlots = numberOfSlotsPerCore(demandInSlots, n);
			
			while( n <  nCores )
			{
				int i = temp.get(w);			
				if(regions.get(i).isEmpty())
				{
					continue;
				}
				
				int it = 0, j = 0, lastShift = j;
				
				while( j < channel[i].length) 
				{	
					if(fittedSlotList.size() == demandInSlots)
					{
						break;
					}
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots && it < nSlots)
					{
						fittedSlotList.add(channel[i][j]);
						it++;
					}
					if(it == nSlots && w < (nCores-1) && fittedSlotList.size() < demandInSlots)
					{
						it = 0;
						j = lastShift;
						w++;
						i = temp.get(w);
					}
					else if(channel[i][j] == null && it < nSlots)
					{
						int v = j;
						j = lastShift;
						lastShift = v;
		
						it = 0;
						fittedSlotList.clear();
					}
					
					j++;
				}
				
				if(fittedSlotList.size() == demandInSlots)
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
					{
//						System.out.println("First-fit: "+demandInSlots+" tam: "+fittedSlotList.size());
//						System.out.println(fittedSlotList);
						return true;
					}
				}

				nSlots = numberOfSlotsPerCore(demandInSlots, n);
//				System.out.println("c = "+nCores+" s = "+nSlots+ " n = "+n+" d = "+demandInSlots);
				for(int t = nCores-1; t >= 0; t--) {
					
					if(regions.get(t).size() < (demandInSlots/(nCores-n)))
					{
						cont.get(u).remove(t);
					}
				}
				
				n++;
				w = 0;
				nCores = cont.get(u).size();
				fittedSlotList.clear();
				
			}
			
			fittedSlotList.clear();
		}
//		System.out.println("--------------");
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
		
		/**
		 * Try to assignment the request in just one core 
		 */
		for(int u = cont.size()-1; u >= 0 ; u--) {
			
			int nCores =  cont.get(u).size();
			
			for(int w = nCores-1; w >= 0; w--) {
				
				int i = cont.get(u).get(w);
				int shift = channel[cont.get(u).get(w)].length-1;
				
				for(int j = shift; j >= 0; j--) {
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
					{
						fittedSlotList.add(channel[i][j]);
					}
//					else if(channel[i][j] == null) break;

					if(fittedSlotList.size() == demandInSlots) 
					{
						if(rsa.establishConnection(links, fittedSlotList, 0, flow)) {
//							System.out.println("Last-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//							System.out.println(fittedSlotList);
							return true;
						}
						
						fittedSlotList.clear();	
					}
				}
				
				fittedSlotList.clear();
			}	
		}
		
		for(int u = cont.size()-1; u >= 0 ; u--) {
			
			int nCores =  cont.get(u).size();
			ArrayList<Integer> temp = new ArrayList<Integer>();
			
			for(int w = 0; w < nCores; w++) {
				temp.add(cont.get(u).get(w));
			}
			
			int shift = channel[0].length;
			int w = nCores-1;
			int n = 1;
			nSlots = numberOfSlotsPerCore(demandInSlots, n);
			
			while( n < nCores )
			{
				int i = temp.get(w);			
				if(regions.get(i).isEmpty())
				{
					continue;
				}
				
				int it = 0;
				int j = shift;
				int lastShift = j;
				
				while( j > 0 && fittedSlotList.size() < demandInSlots) 
				{
					j--;
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots && it < nSlots)
					{
						fittedSlotList.add(channel[i][j]);
						it++;
					}
					if(it == nSlots && w >= 1 && fittedSlotList.size() < demandInSlots)
					{
						it = 0;
						j = lastShift;
						w--;
						i = temp.get(w);
					}
					else if(channel[i][j] == null && it < nSlots)
					{
						int v = j;
						j = lastShift;
						lastShift = v;
						
						it = 0;
						fittedSlotList.clear();
						
						continue;
					}
				}
				
				if(fittedSlotList.size() == demandInSlots)
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
					{
//						System.out.println("Last-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//						System.out.println(fittedSlotList);
						return true;
					}
				}

				nSlots = numberOfSlotsPerCore(demandInSlots, n);
//				System.out.println("c = "+nCores+" s = "+nSlots+ " n = "+n+" d = "+demandInSlots);
				n++;
				for(int t = nCores-1; t >= 0; t--) {
					
					if(regions.get(t).size() < (demandInSlots/nCores))
					{
						cont.get(u).remove(t);
					}
				}
				
				nCores = cont.get(u).size();
				w = nCores-1;
				fittedSlotList.clear();
				
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

		int numberOfSlots = 50;
		int numberOfCores = rsa.getNumberOfCores()-3;
		
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >();
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		
		
		if(demandInSlots <= numberOfSlots)
		{
			int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
			
			for(int i = 0; i < (numberOfCores-1); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, (numberOfCores-1)))
				{
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}		
			}
			
			return this.runFirstFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
		else if( demandInSlots >= numberOfSlots+1)
		{
				
			for(int i = (numberOfCores-1); i < rsa.getNumberOfCores(); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
				{
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}	
			}
			
			return this.runLastFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
			
		}
		
		return false;
	}

	@SuppressWarnings("unused")
	public boolean firstLastFitSlots(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {

		int numberOfSlots = 50;
		int numberOfCores = rsa.getNumberOfCores();
		
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >();
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		int limitSlots = (rsa.getNumberOfSlots()/2)-1;

		
		if(demandInSlots <= numberOfSlots)
		{
				for(int i = 0; i < mergedRegions.size(); i++)
				{
					if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
					{
						for(int j = mergedRegions.get(i).size()-1; j >= 0; j--) {
							
							if(mergedRegions.get(i).get(j) != null && mergedRegions.get(i).get(j).s > limitSlots) {
								mergedRegions.get(i).remove(mergedRegions.get(i).get(j));
								channel[i][j] = null;
							}
						}
						if(!mergedRegions.get(i).isEmpty()) coreIndices.add(i);
					}
						
				}
				
				return this.runFirstFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
		else
		{
			
			for(int i = 0; i < mergedRegions.size(); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
				{
					for(int j = mergedRegions.get(i).size()-1; j >= 0; j--) {
						
						if(mergedRegions.get(i).get(j) != null && mergedRegions.get(i).get(j).s <= limitSlots) {
							mergedRegions.get(i).remove(mergedRegions.get(i).get(j));
							channel[i][j] = null;
						}
					}
					if(!mergedRegions.get(i).isEmpty()) coreIndices.add(i);
				}	
			}
			
			return this.runLastFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
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
