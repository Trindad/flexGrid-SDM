package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.Modulations;
import flexgridsim.ModulationsMuticore;
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
public class DynamicModulation implements RSA {
	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp,
			TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();
	}

	public void flowArrival(Flow flow) {
	KShortestPaths kShortestPaths = new KShortestPaths();
		int K = 5;
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), K);
		int demandInSlots[] = new int[ModulationsMuticore.numberOfModulations()];
		
		for (int k = 0; k < kPaths.length; k++) {
				for (int m = ModulationsMuticore.numberOfModulations()-1; m >= 0 ; m--)	{
					flow.setModulationLevel(m);
					demandInSlots[m] = (int) Math.ceil((double) flow.getRate() / (double)  Modulations.getBandwidth(m));
					boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
					if (checkDistanceAlocation(kPaths, k, m)){
						for (int i = 0; i < spectrum.length; i++) {
							for (int j = 0; j < spectrum[i].length; j++) {
								spectrum[i][j]=true;
							}
						}
						for (int i = 0; i < kPaths[k].length-1; i++) {
							imageAnd(pt.getLink(kPaths[k][i], kPaths[k][i+1]).getAllocableSpectrum(m,20), spectrum, spectrum);
						}
						
						ConnectedComponent cc = new ConnectedComponent();
						HashMap<Integer,ArrayList<Slot>> listOfRegions = cc.listOfRegions(spectrum);
						if (listOfRegions.isEmpty()){
							continue;
						}
						int[] links = new int[kPaths[k].length - 1];
						for (int j = 0; j < kPaths[k].length - 1; j++) {
							links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
						}
						if (fitConnection(listOfRegions, demandInSlots[m], links, flow, m))
							return;
					}
			}
		}
		cp.blockFlow(flow.getID());
		return;
	}
	
	/**
	 * @param kPaths
	 * @param k
	 * @param modulation
	 * @return physical distance allocation
	 */
	public boolean checkDistanceAlocation(int[][] kPaths, int k, int modulation){
		int dist =0;
		for (int i = 0; i < kPaths[k].length-1; i++) {
			dist+=pt.getLink(kPaths[k][i], kPaths[k][i+1]).getDistance();
		}
		if (dist<ModulationsMuticore.getMaxDistance(modulation))
			return true;
		else 
			return false;
	}
	
	/**
	 * @param listOfRegions
	 * @param demandInSlots
	 * @param links
	 * @param flow
	 * @param modulation 
	 * @return true if fitness successful
	 */
	public boolean fitConnection(HashMap<Integer,ArrayList<Slot>> listOfRegions, int demandInSlots, int[] links, Flow flow, int modulation){
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		for (Integer key : listOfRegions.keySet()) {
		    if (listOfRegions.get(key).size()>=demandInSlots){
		    	for (int i = 0; i < demandInSlots; i++) {
		    		fittedSlotList.add(listOfRegions.get(key).get(i));
				}
		    	if (establishConnection(links, fittedSlotList, flow, modulation)){
					return true;
				}
		    }
		}
		return false;
	}
	
	/**
	 * @param links
	 * @param slotList
	 * @param flow
	 * @param modulation 
	 * @return true if connection was successfully established
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, Flow flow, int modulation){
		long id = vt.createLightpath(links, slotList, modulation);
		if (id >= 0) {
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			flow.setSlotList(slotList);
			cp.acceptFlow(flow.getID(), lps);
			return true;
		} else {
			return false;
		}
	}
	
	private void imageAnd(boolean[][] img1, boolean[][] img2, boolean[][] res){
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[0].length; j++) {
				res[i][j] = img1[i][j] & img2[i][j];
			}
		}
	}
	@Override
	public void flowDeparture(Flow flow) {

	}
}
