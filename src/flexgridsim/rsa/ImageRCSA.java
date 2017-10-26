package flexgridsim.rsa;

import java.util.ArrayList;
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
		boolean established = false;

		for (Integer key : listOfRegions.keySet()) {

			//First-fit
			if (listOfRegions.get(key).size() >= demandInSlots)
			{
				int newCore = listOfRegions.get(key).get(0).c;
				
				for(int i = 0; i < listOfRegions.get(key).size(); i++) 
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
						if(establishConnection(links, fittedSlotList, 0, flow)) {
							System.out.println("Fist-fit");
//							System.out.print(fittedSlotList);
							return true;
						}
					}
					
					if(i < listOfRegions.get(key).size())
					{
						newCore = listOfRegions.get(key).get(i).c;
						
					}
					
					fittedSlotList.clear();
				}
		    }
			
			//construct a super-channel crossing different cores
			if(!established && listOfRegions.get(key).size() >= demandInSlots)
			{
				fittedSlotList.clear();
				for(int i = 0; i < demandInSlots; i++) {
					fittedSlotList.add(listOfRegions.get(key).get(i));
				}
				
				if(establishConnection(links, fittedSlotList, 0, flow)) {
					return true;
				}
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
//			System.out.println("*************SLOT**************");
			System.out.println(slotList);
//			System.out.println("*************FIN**************");
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
