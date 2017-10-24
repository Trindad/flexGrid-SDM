package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.ConnectedComponent;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;

/**
 * @author pedrom
 *
 */
public class ImageRCSA implements RSA {

	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	
	public void simulationInterface(Element xml, PhysicalTopology pt,
			VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
	}

	public void flowArrival(Flow flow) {
		
		int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 5);
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		
		for (int k = 0; k < kPaths.length; k++) {
			for (int i = 0; i < spectrum.length; i++) {
				for (int j = 0; j < spectrum[i].length; j++) {
					spectrum[i][j]=true;
				}
			}
			for (int i = 0; i < kPaths[k].length-1; i++) {
				imageAnd(pt.getLink(kPaths[k][i], kPaths[k][i+1]).getSpectrum(), spectrum, spectrum);
			}
			
			//printSpectrum(spectrum);
			ConnectedComponent cc = new ConnectedComponent();
			HashMap<Integer,ArrayList<Slot>> listOfRegions = cc.listOfRegions(spectrum);
			
			if (listOfRegions.isEmpty()){
				continue;
			}
			int[] links = new int[kPaths[k].length - 1];
			for (int j = 0; j < kPaths[k].length - 1; j++) {
				links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
			}
			if (fitConnection(listOfRegions, demandInSlots, links, flow))
				return;
			
		}
		cp.blockFlow(flow.getID());
		return;
	}

	
	/**
	 * @param listOfRegions
	 * @param demandInSlots
	 * @param links
	 * @param flow
	 * @return given a list of rectangles and a demand, the algorithm tries to fit the connector into the spectra
	 */
	public boolean fitConnection(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow){
		
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
//		System.out.println("demand: "+demandInSlots);
//		for (Integer key : listOfRegions.keySet()) {
//			System.out.println("tam: "+listOfRegions.get(key).size()+" key: "+key);
//		}
		
		
//		ArrayList<Integer> keys = new ArrayList<Integer>();
//		keys.addAll(listOfRegions.keySet());
//		Collections.shuffle(keys);
//		System.out.println(keys);
		for (Integer key : listOfRegions.keySet()) {
//			int i = 0;
			System.out.println("tami: "+listOfRegions.get(key).size()+" key: "+key+" d: "+demandInSlots);
		    
			if (listOfRegions.get(key).size() >= demandInSlots)
			{
//		    	while(i < listOfRegions.get(key).size())
//		    	{
//		    		int t = 0;
//			    	System.out.println("t: "+t+" iterator: "+i+" d: "+demandInSlots);
//		    		while( t < demandInSlots) 
//		    		{
			    	for(int i = 0; i < demandInSlots;i++) {
		    			fittedSlotList.add(listOfRegions.get(key).get(i));
		    			
//		    			i++;
//		    			if(i >=  listOfRegions.get(key).size()) break;
//			    		t++;
					}
		    		System.out.print(fittedSlotList);
//			    	if (fittedSlotList.size() == demandInSlots)
//			    	{
	//		    		System.out.println(" alloc key:"+key);
						if(establishConnection(links, fittedSlotList, 0, flow)) 
						{
							return true;
						}
//					}
//			    	else
//			    	{
//			    		fittedSlotList.clear();
//			    	}
//			    	
//		    	}	
		    }
		}
		
		return false;
	}
	
	/**
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return true if the connection was successfully established; false otherwise
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow){
		long id = vt.createLightpath(links, slotList ,0);
		if (id >= 0) {
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			System.out.println("*************SLOT**************");
			System.out.println(slotList);
			System.out.println("*************FIN**************");
			flow.setSlotList(slotList);
			cp.acceptFlow(flow.getID(), lps);
			return true;
		} else {
			return false;
		}
	}
		
	protected void imageAnd(boolean[][] img1, boolean[][] img2, boolean[][] res){
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[0].length; j++) {
				res[i][j] = img1[i][j] & img2[i][j];
			}
		}
	}

	
	public void flowDeparture(Flow flow) {
		
	}
	
}
