package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.rsa.ImageRCSA;

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
		
		nSlots = numberOfSlotsPerCore(demandInSlots, coreIndices.size());
		ArrayList< ArrayList<Integer> > cont = getGroupContiguosCore(coreIndices);
		
//		for(int i = 0; i < mergedRegions.size(); i++)
//		{
//			System.out.print(mergedRegions.get(i));
//		}
//		System.out.println("nSlots: "+nSlots);
		
		for(int u = 0; u < cont.size(); u++ ) {
			
			int nCores =  cont.get(u).size();
			ArrayList<Integer> temp = new ArrayList<Integer>();
			
			for(int w = 0; w < nCores; w++) {
				temp.add(cont.get(u).get(w));
			}
			
			int shift = 0;
			for(int w = 0; w < nCores; w++)
			{
				int i = temp.get(w);
				
				if(regions.get(i).isEmpty())
				{
					continue;
				}
				else if( w >= 1)
				{
					if( (coreIndices.get(w) - coreIndices.get(w-1)) >= 2) numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
				}
				
				
				boolean allocatable = true;
				int it = 0;

				for(int j = shift; j < channel[i].length; j++) {
					
//					System.out.println(channel[i][j]);
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
					{
						fittedSlotList.add(channel[i][j]);
						it++;
					}
					
					if(channel[i][j] == null && (channel[i].length-j+1) >= nSlots)
					{
						fittedSlotList.clear();
						shift = j+1;
						it = 0;
					}
					else if(channel[i][j] == null && fittedSlotList.size() < demandInSlots && it < nSlots)
					{
						allocatable = false;
						break;
					}
					else if(channel[i][j] == null || fittedSlotList.size() == demandInSlots || it == nSlots)
					{
						break;
					}
					
				}
				
				if(fittedSlotList.size() == demandInSlots)
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
					{
	//					System.out.println("First-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//						System.out.print(fittedSlotList);
						return true;
					}
					else
					{
						//delete core without potential to assignment this demand
						int last = nCores;

						for(int v = nCores-1; v >= 0; v--){
							
							int t = cont.get(u).get(v);
							
							if(regions.get(t).size() <= nSlots)
							{
								cont.get(u).remove(v);
							}
						}
						
						if(last > cont.get(u).size()) 
						{
							nCores = cont.get(u).size();
							nSlots = numberOfSlotsPerCore(demandInSlots, nCores);
							
							fittedSlotList.clear();
						}
						
						allocatable = true;
					}
				}	
				
				if(!allocatable)
				{
					//delete core without potential to assignment this demand
					int last = nCores;

					for(int v = nCores-1; v >= 0; v--){
						
						int t = cont.get(u).get(v);
						
						if(regions.get(t).size() <= nSlots)
						{
							cont.get(u).remove(v);
						}
					}
					
					if(last > cont.get(u).size()) 
					{
						nCores = cont.get(u).size();
						nSlots = numberOfSlotsPerCore(demandInSlots, nCores);
						
						fittedSlotList.clear();
					}
					
					allocatable = true;
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

		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		
		if(coreIndices.isEmpty()) return false;
		
		nSlots = numberOfSlotsPerCore(demandInSlots, coreIndices.size());
		ArrayList< ArrayList<Integer> > cont = getGroupContiguosCore(coreIndices);
		
//		for(int i = 0; i < mergedRegions.size(); i++)
//		{
//			System.out.print(mergedRegions.get(i));
//		}
//		System.out.println("nSlots: "+nSlots);
		
		for(int u = cont.size()-1; u >= 0 ; u--) {
			
			int nCores =  cont.get(u).size();
			ArrayList<Integer> temp = new ArrayList<Integer>();
			
			for(int w = 0; w < nCores; w++) {
				temp.add(cont.get(u).get(w));
			}
			
			int shift = channel[0].length-1;
			
			for(int w = nCores-1; w >= 0; w--)
			{
				int i = temp.get(w);
				
				if(regions.get(i).isEmpty())
				{
					continue;
				}
				else if( w >= 1)
				{
					if( (coreIndices.get(w) - coreIndices.get(w-1)) >= 2) numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
				}
				
				
				boolean allocatable = true;
				int it = 0;
				
				
				for(int j = shift; j >= 0; j--) {
					
//					System.out.println(channel[i][j]);
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
					{
						fittedSlotList.add(channel[i][j]);
						it++;
					}
					
					if(channel[i][j] == null && (channel[i].length-j+1) >= nSlots)
					{
						fittedSlotList.clear();
						shift = j-1;
						it = 0;
					}
					else if(channel[i][j] == null && fittedSlotList.size() < demandInSlots && it < nSlots)
					{
						shift = j-1;
						allocatable = false;
						break;
					}
					else if(channel[i][j] == null || fittedSlotList.size() == demandInSlots || it == nSlots)
					{
						break;
					}
					
				}
				
				if(fittedSlotList.size() == demandInSlots)
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) 
					{
	//					System.out.println("First-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//						System.out.print(fittedSlotList);
						return true;
					}
					else
					{
						//delete core without potential to assignment this demand
						int last = nCores;

						for(int v = nCores-1; v >= 0; v--){
							
							int t = cont.get(u).get(v);
							
							if(regions.get(t).size() <= nSlots)
							{
								cont.get(u).remove(v);
							}
						}
						
						if(last > cont.get(u).size()) 
						{
							nCores = cont.get(u).size();
							nSlots = numberOfSlotsPerCore(demandInSlots, nCores);
							
							fittedSlotList.clear();
						}
						
						allocatable = true;
					}
				}
				
				if(!allocatable)
				{
					//delete core without potential to assignment this demand
					int last = nCores;

					for(int v = nCores-1; v >= 0; v--){
						
						int t = cont.get(u).get(v);
						
						if(regions.get(t).size() <= nSlots)
						{
							cont.get(u).remove(v);
						}
					}
					
					if(last > cont.get(u).size()) 
					{
						nCores = cont.get(u).size();
						nSlots = numberOfSlotsPerCore(demandInSlots, nCores);
						
						fittedSlotList.clear();
					}
					
					allocatable = true;
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

		int numberOfSlots = 200;
		int numberOfCores = rsa.getNumberOfCores()-4;
		
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >();
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		
		
		if(demandInSlots <= numberOfSlots)
		{
			int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
			
			for(int i = 0; i <= numberOfCores; i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
				{
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}
					
			}
			
			return this.runFirstFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
		else if( (demandInSlots >= numberOfSlots+1))
		{
			int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
			
			for(int i = numberOfCores; i < mergedRegions.size(); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores-mergedRegions.size()))
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

		int numberOfSlots = 200;
		int numberOfCores = rsa.getNumberOfCores()-4;
		
		ArrayList< ArrayList<Slot> > regions = new ArrayList< ArrayList<Slot> >();
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		Slot [][]channel = mergeRegions(listOfRegions);
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		int limitSlots = (rsa.getNumberOfSlots()/2)-1;
		
		
		if(demandInSlots <= numberOfSlots)
		{
			int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
			
			for(int i = 0; i < mergedRegions.size(); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores))
				{
					for(int j = 0; j < mergedRegions.get(i).size(); j ++) {
						
						if(mergedRegions.get(i).get(j) != null && mergedRegions.get(i).get(j).s > limitSlots) {
							mergedRegions.get(i).remove(mergedRegions.get(i).get(j));
						}
					}
					
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}
					
			}
			
			return this.runFirstFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
		else if( (demandInSlots >= numberOfSlots+1))
		{
			int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
			
			for(int i = 0; i < mergedRegions.size(); i++)
			{
				if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= numberOfSlotsPerCore(demandInSlots, numberOfCores-mergedRegions.size()))
				{
					for(int j = 0; j < mergedRegions.get(i).size(); j ++) {
						
						if(mergedRegions.get(i).get(j) != null && mergedRegions.get(i).get(j).s <= limitSlots) {
							mergedRegions.get(i).remove(mergedRegions.get(i).get(j));
						}
					}
					
					regions.add(mergedRegions.get(i));
					coreIndices.add(i);
				}	
			}
			
			return this.runLastFit(channel, mergedRegions, coreIndices, demandInSlots, links, flow);
		}
		
		return false;
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
