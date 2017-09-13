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
 * 
 * 
 * @author trindade
 */
class ConnectionRequest {

    Flow flow;
    int []links;
    HashMap<Integer,ArrayList<Slot>> listOfRegions;
    int demandInSlots;

    public ConnectionRequest() {}
    
	public Flow getFlow() {
		return flow;
	}

	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	public int[] getLinks() {
		return links;
	}

	public void setLinks(int[] links) {
		this.links = links;
	}

	public HashMap<Integer, ArrayList<Slot>> getListOfRegions() {
		return listOfRegions;
	}

	public void setListOfRegions(HashMap<Integer, ArrayList<Slot>> listOfRegions) {
		this.listOfRegions = listOfRegions;
	}

	public int getDemandInSlots() {
		return demandInSlots;
	}

	public void setDemandInSlots(int demandInSlots) {
		this.demandInSlots = demandInSlots;
	}
}


public class BatchGroomingSDM extends ImageRCSA{

    protected PhysicalTopology pt;
    protected VirtualTopology vt;
    protected ControlPlaneForRSA cp;
    protected WeightedGraph graph;
    protected ArrayList<ConnectionRequest> blockedRequests; 

    protected  ArrayList<ConnectionRequest> postponedRequests;
    protected  HashMap<ConnectionRequest,ArrayList<Slot>> batchRequests;
    
    public void simulationInterface(Element xml, PhysicalTopology pt,
            VirtualTopology vt, ControlPlaneForRSA cp, TrafficGenerator traffic) {
        this.pt = pt;
        this.vt = vt;
        this.cp = cp;
        this.graph = pt.getWeightedGraph();
    }

    public int[] RCSAProcessing(Flow flow, int demandInSlots) {
        
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
        }

    }

    public void flowArrival(Flow flow) 
    {

        int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());

        while() {

            RCSAProcessing(flow);
            
            if (links.length == 0) 
            {
                
            }
            else
            {
              fitConnection(listOfRegions, demandInSlots, links, flow);
            }
        }
            
        cp.blockFlow(flow.getID());
    }

    public void batchRequestsFound() 
    {

        cp.blockFlow(flow.getID());
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
        
        for (Integer key : listOfRegions.keySet()) {
            if (listOfRegions.get(key).size() >= demandInSlots){
                for (int i = 0; i < demandInSlots; i++) {
                    fittedSlotList.add(listOfRegions.get(key).get(i));
                }
                if (establishConnection(links, fittedSlotList, 0, flow)){
                    return true;
                }
            }
        }
        return false;
    }
}
