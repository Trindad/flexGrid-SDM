package flexgridsim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.rsa.ImageRCSA;

public class ResourceAssignment {
	
	private ImageRCSA rsa;
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

	@SuppressWarnings("unused")
	public boolean firstFit(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		
		
		Slot [][]channel = mergeRegions(listOfRegions);
		int nSlots = numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
		ArrayList<Integer> coreIndices = new ArrayList<Integer>();
		
		for(int i = 0; i < mergedRegions.size(); i++)
		{
			if(!mergedRegions.get(i).isEmpty() && mergedRegions.get(i).size() >= nSlots) coreIndices.add(i);
		}
		
		if(coreIndices.isEmpty()) return false;
		
		nSlots = numberOfSlotsPerCore(demandInSlots, coreIndices.size());
		ArrayList< ArrayList<Integer> > cont = getGroupContiguosCore(coreIndices);
		
//		for(int i = 0; i < mergedRegions.size(); i++)
//		{
//			System.out.print(mergedRegions.get(i));
//		}
//		System.out.println("nSlots: "+nSlots);
		
		for(int u = 0; u < cont.size(); u++ ) {
			
			for(int w = 0; w < cont.get(u).size(); w++)
			{
				int i = cont.get(u).get(w);
				
				if(mergedRegions.get(i).isEmpty())
				{
					continue;
				}
				else if( w >= 1)
				{
					if( (coreIndices.get(w) - coreIndices.get(w-1)) >= 2) numberOfSlotsPerCore(demandInSlots, rsa.getNumberOfCores());
				}
				
				
				int allocatable = 0, it = 0;
				int shift = 0;
				
				for(int j = shift; j < channel[i].length; j++) {
					
//					System.out.println(channel[i][j]);
					
					if(channel[i][j] != null && fittedSlotList.size() < demandInSlots)
					{
						fittedSlotList.add(channel[i][j]);
						it++;
					}
					
					if(channel[i][j] == null || fittedSlotList.size() == demandInSlots || it == nSlots)
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
//					System.out.println();
				}
			}
			
			fittedSlotList.clear();
		}
		
		return false;
	}
	
	@SuppressWarnings("unused")
	public boolean firstLastFit(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow, Integer key) {
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int n = listOfRegions.get(key).size();
		int numberOfSlots = 200;
		int numberOfCores = rsa.getNumberOfCores()-4;
		
		if(demandInSlots <= numberOfSlots && listOfRegions.get(key).get(0).c <= numberOfCores)
		{
			int newCore = listOfRegions.get(key).get(0).c;
			
			for(int i = 0; i < n; i++) 
			{
				while( (newCore == listOfRegions.get(key).get(i).c) && (fittedSlotList.size() < demandInSlots)) 
				{
					if( (i < 1 || (listOfRegions.get(key).get(i).s - listOfRegions.get(key).get(i-1).s) <= 1) ) 
					{
						fittedSlotList.add(listOfRegions.get(key).get(i));
		    			i++;
		    			if( listOfRegions.get(key).size() == i) break;
					}
					else break;
		    			
				}
				
				if(fittedSlotList.size() == demandInSlots)
				{
					if(rsa.establishConnection(links, fittedSlotList, 0, flow)) {
//						System.out.println("First-last-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
	//					System.out.print(fittedSlotList);
						return true;
					}
				}
				
				if(i < n)
				{
					newCore = listOfRegions.get(key).get(i).c;
					
				}
				
				fittedSlotList.clear();
			}
		}
		else if( (demandInSlots >= numberOfSlots+1) && listOfRegions.get(key).get(n-1).c >= 3)
		{
//			System.out.println("AQUI LAST: "+demandInSlots+" "+n);
//			System.out.print(listOfRegions.get(key));
			int newCore = listOfRegions.get(key).get(n-1).c;
			
			for(int i = n-1; i >= 0 && newCore >= numberOfCores; i--) 
			{
				while( (i >= newCore-1) && (Math.abs(newCore - listOfRegions.get(key).get(i).c) == 1) && 
						(fittedSlotList.size() < demandInSlots)) 
				{
					if( ( ( i >= ( n-1 ) || Math.abs( listOfRegions.get(key).get(i).s - listOfRegions.get(key).get(i-1).s ) == 1 ) ) )
					{
						fittedSlotList.add(listOfRegions.get(key).get(i));
			          
						i--;
					}
					else 
					{
			    	  	break;
					}  
				}
				
//				System.out.print(fittedSlotList);
//				System.out.println("\ntamanho fitted slot: "+fittedSlotList.size()+" demand: "+demandInSlots+"\n");
			  
				boolean established  = false;
				
				if(fittedSlotList.size() == demandInSlots)
				{
					established = rsa.establishConnection(links, fittedSlotList, 0, flow);
					if(established) 
					{
//			          System.out.println("Last-fit:"+demandInSlots+" tam: "+fittedSlotList.size());
//			          System.out.print(fittedSlotList);
			          return true;
					}
				}
			  
				if(i >= 0) 
				{ 
			
			      newCore = listOfRegions.get(key).get(i).c;
			      
			      if(!established && fittedSlotList.size() == demandInSlots) 
			      {
			    	  fittedSlotList.clear();
			    	  
			    	  if((n-demandInSlots)<= 0) return false;
			      }
			    	  
				}
			}			
//			System.out.println("\n*****************************************");
		}
		
		return false;
	}
	
	public Slot [][]mergeRegions(HashMap<Integer,ArrayList<Slot>> listOfRegions) {
		 
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
			}
		}
		
		return channel;
	}
	
	public int numberOfSlotsPerCore(int demand, int cores) {
		
		return (int) Math.ceil((double)demand/(double)cores);
	}
	
}
