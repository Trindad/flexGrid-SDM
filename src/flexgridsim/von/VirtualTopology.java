package flexgridsim.von;

import java.util.ArrayList;import java.util.Comparator;

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
	
	public double totalResources;
	public double sigma = 0.5;
	
	public VirtualTopology() {
		setID(VirtualLink.ID++);
		
		this.links = new ArrayList<VirtualLink>();
		this.nodes = new ArrayList<VirtualNode>();
		this.requests = new ArrayList<Request>();
	}
	
	public void calculateTotalRequestResources() {
		
		double sumComputing = 0;
		double sumBandwidth = 0;
		
		links.sort(Comparator.comparing(VirtualLink::getBandwidth).reversed());
		double maxBandwith = (double)links.get(0).getBandwidth();
		for(VirtualLink link : links) {
			
			sumBandwidth += ((double)link.getBandwidth()/ maxBandwith);
		}
		
		nodes.sort(Comparator.comparing(VirtualNode::getComputeResource).reversed());
		double maxComputing = (double)nodes.get(0).getComputeResource();
		for(VirtualNode node: nodes) {
			
			sumComputing += ((double)node.getComputeResource()/ maxComputing);
		}
		
		totalResources = sigma * (sumComputing)/ ((double)nodes.size()) + (1.0 - sigma) * (sumBandwidth) / ((double)links.size());
	}


	public int getID() {
		return id;
	}


	public void setID(int id) {
		this.id = id;
	}
	
	public double getTotalResources() {
		return totalResources;
	}

	public boolean isNodeConnected(VirtualNode node) {
		
		for(VirtualLink link : links) {
			
			if(link.getSource().getId() == node.getId() || link.getDestination().getId() == node.getId()) {
				return true;
			}
		}
		
		return false;
	}
	 
}
