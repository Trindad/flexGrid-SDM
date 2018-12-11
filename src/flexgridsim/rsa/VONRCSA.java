package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.w3c.dom.Element;

import com.sun.javafx.fxml.expression.BinaryExpression;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VonControlPlane;
import flexgridsim.util.WeightedGraph;
import flexgridsim.von.VirtualTopology;


/**
 * 
 * RCSA based on "A load balancing algorithm based on Key-Link and resources contribution degree for virtual optical networks mapping"
 * 
 * Authors: G. Zhao, Z. Xu, Z. Ye, K. Wang and J. Wu
 * 
 * @author trindade
 *
 */

public class VONRCSA extends SCVCRCSA {
	private VonControlPlane cp;
	
	public void flowArrival(Flow flow) {
		kPaths = 3;
		System.out.println(flow);
		setkShortestPaths(flow);

		int []modulationFormats = new int[paths.size()];
		ArrayList<ArrayList<Slot>> blockOfSLots = getBlockOfSlots(flow, modulationFormats);
		System.out.println(blockOfSLots.size());
		int index = selectPath(blockOfSLots, flow);
		
		establishConnection(paths.get(index), blockOfSLots.get(index), modulationFormats[index], flow);

	}

	protected int preProcessSpectrumResources(boolean [][]spectrum) {
		
		int maxSlotIndex = 0;
		
		for(int core = (spectrum.length-1); core >= 0 ; core--) {
			int s = getMaximumIndexOfUsed(spectrum[core]);
			if(s > maxSlotIndex) {
				maxSlotIndex = s;
			}
		}
		
		
		return maxSlotIndex;
	}
	
	private int getMaximumIndexOfUsed(boolean[] core) {

		for(int i = (core.length-1); i >= 0; i--) {
			if(!core[i]) {
				return i;
			}
		}
		
		return 0;
	}

	public ArrayList<Slot> FirstFitPolicy(Flow flow, int []links, int demandInSlots, int modulation) {
		
		boolean [][]spectrum = bitMapAll(links);
		int maxSlotIndex = preProcessSpectrumResources(spectrum);
		
		for(int k = (spectrum.length-1); k >= 0; k--) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
			for(int j = 0; j <= maxSlotIndex; j++) {
				
				int limit = j + (demandInSlots - 1);
				
				if(limit >= pt.getNumSlots()) {
					break;
				}
				
				int n = j;
				ArrayList<Slot> candidate = new ArrayList<Slot>();
				while(n <= limit && spectrum[k][n] == true ) {
					candidate.add( new Slot(k,n) );
					n++;
				}
				
				if(candidate.size() == demandInSlots) {
					
					if(cp.CrosstalkIsAcceptable(flow, links, candidate, ModulationsMuticore.inBandXT[modulation])) {
						slots.addAll(new ArrayList<Slot>(candidate));
						break;
					}
				}
			}
			
			if(slots.size() == demandInSlots) {
//				System.out.println(slots.size());
				return slots;
			}	
		}
		
		return new ArrayList<Slot>();
	}
	
	
	private int selectPath(ArrayList<ArrayList<Slot>> blockOfSlots, Flow flow) {
		
		int selectedPath = 0;
		Slot last = null;
		
		for(int i = 0; i < blockOfSlots.size(); i++) {
		
			if(blockOfSlots.get(i).size() >= 1) {
				Slot temp = blockOfSlots.get(i).get(blockOfSlots.get(i).size()-1);
				if(last == null) 
				{
					selectedPath = i;
				}
				else if(last.c <= temp.c && last.s > temp.s) 
				{
					selectedPath = i;
				}
			}
		}
		
		return selectedPath;
	}

	private ArrayList<ArrayList<Slot>> getBlockOfSlots(Flow flow, int []modulationFormats) {
		
		ArrayList<ArrayList<Slot>> blockOfSlots = new ArrayList<ArrayList<Slot>>();
		modulationFormats = getModulationFormat(flow);
		
		for(int p = 0; p < paths.size(); p++) {
			
			double requestedBandwidthInGHz = ( ((double)flow.getRate()) / ((double)modulationFormats[p] + 1) );
			double requiredBandwidthInGHz = requestedBandwidthInGHz;
			double slotGranularityInGHz = ModulationsMuticore.subcarriersCapacity[0];
			int demandInSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
			
			demandInSlots = (demandInSlots % 2) >= 1 ? (demandInSlots + 1) : demandInSlots;
			demandInSlots++;//adding guardband
			
			blockOfSlots.add(FirstFitPolicy(flow, paths.get(p), demandInSlots, modulationFormats[p]));
		}
		
//		System.out.println(blockOfSlots.size());
		return blockOfSlots;
	}

	private int []getModulationFormat(Flow flow) {
		
		int []modulationFormats = new int[paths.size()];
		
		for(int i = 0; i < paths.size(); i++) {
			
			int totalLength = 0;
			
			for(int link : paths.get(i)) {
				
				totalLength += (pt.getLink(link).getDistance());
			}
			
			//invalid path
			if(totalLength > ModulationsMuticore.maxDistance[0]) {
				System.out.println("Error. Invalid path");
			}
			
			int modulationLevel =  ModulationsMuticore.getModulationByDistance(totalLength);
			
			modulationLevel = flow.getRate() < ModulationsMuticore.subcarriersCapacity[modulationLevel] &&  modulationLevel > 0 ? decreaseModulation(modulationLevel, flow.getRate()) : modulationLevel;

			modulationFormats[i] = modulationLevel;
		}
		
		return modulationFormats;
	}

	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow) {
		
		if(links == null || flow == null) 
		{
			System.out.println("Invalid variables");
			return false;
		}
		
		if (slotList.isEmpty()) {
			return false;
		}
		
		for (int i = 0; i < links.length; i++) {
            if (!pt.getLink(links[i]).areSlotsAvailable(slotList, modulation)) {
                return false;
            }
        }
		
		flow.setLinks(links);
		flow.setSlotList(slotList);
		flow.setCore(slotList.get(0).c);
		flow.setModulationLevel(modulation);
		flow.setAccepeted(true);

		flow.setPathLength(getPathLength(links));
		flow.setCore(slotList.get(0).c);
		
		cp.addFlowToPT(flow);
		
		return true;
		
	}

	public void flowDeparture(Flow flow) {
		super.flowDeparture(flow);
	}
	
	public void setkShortestPaths(Flow flow) {
		
		this.paths  = new ArrayList<int []>();
		
		if(pt == null) {
			System.out.println("Physical topology is NULL");
		}
		
		org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> kSP = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getVONGraph(), kPaths);
		List< GraphPath<Integer, DefaultWeightedEdge> > KPaths = kSP.getPaths( flow.getSource(), flow.getDestination() );
			
		if(KPaths.size() >= 1)
		{
			for (int k = 0; k < KPaths.size(); k++) {
				
				List<Integer> listOfVertices = KPaths.get(k).getVertexList();
				int[] links = new int[listOfVertices.size()-1];
				
				for (int j = 0; j < listOfVertices.size()-1; j++) {
					
					links[j] = pt.getLink(listOfVertices.get(j), listOfVertices.get(j+1)).getID();
				}

				this.paths.add(links);
			}
		}
	}

	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, TrafficGenerator traffic) {
		this.pt = pt;
	}
	
	public void setVonControlPlane(VonControlPlane cp) 
	{
		this.cp = cp;
	}

	public void setPhysicalTopology(PhysicalTopology ptCopy) {
		this.pt = ptCopy;
	}
}
