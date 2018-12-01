package flexgridsim.von;

import java.util.ArrayList;

/**
 * 
 * @author trindade
 *
 */
public class VirtualTopology {
	
	public static int ID = 1;

	public ArrayList<VirtualLink> links;
	public ArrayList<VirtualNode> nodes;
	public ArrayList<Request> requests;
	
	public double arrivalTime;
	public double holdingTime;
	
	private int id;
	
	public double totalResources = 0;
	public double sigma = 0.5;
	
	public VirtualTopology() {
		setID(VirtualLink.ID++);
		
		this.links = new ArrayList<VirtualLink>();
		this.nodes = new ArrayList<VirtualNode>();
		this.requests = new ArrayList<Request>();
	}
	
	public void calculateTotalRequestResources(int maxComputing, int maxBandwith) {
		
		double sumComputing = 0;
		double sumBandwidth = 0;
		
		for(VirtualLink link : links) {
			
			sumBandwidth += ((double)link.getBandwidth()/ (double)maxBandwith);
		}
		
		for(VirtualNode node: nodes) {
			
			sumComputing += ((double)node.getComputeResource()/ (double)maxComputing);
		}
		
		totalResources = sigma * (sumComputing)/ ((double)nodes.size()) + (1.0 - sigma) * (sumBandwidth) / ((double)links.size());
	}


	public int getID() {
		return id;
	}


	public void setID(int id) {
		this.id = id;
	}
	 
}
