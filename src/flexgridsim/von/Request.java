package flexgridsim.von;

import java.util.ArrayList;

public class Request {

	private ArrayList<Integer> bandwidths;//number of slots 
	private ArrayList<VirtualLink> links;
	private ArrayList<VirtualNode> nodes;
	private ArrayList<Double> computingResources;
	
	public ArrayList<Integer> getBandwidths() {
		return bandwidths;
	}
	
	public void setBandwidths(ArrayList<Integer> bandwidths) {
		this.bandwidths = bandwidths;
	}
	
	public ArrayList<VirtualLink> getLinks() {
		return links;
	}
	
	public void setLinks(ArrayList<VirtualLink> links) {
		this.links = links;
	}
	
	public ArrayList<VirtualNode> getNodes() {
		return nodes;
	}
	
	public void setNodes(ArrayList<VirtualNode> nodes) {
		this.nodes = nodes;
	}
	
	public ArrayList<Double> getComputingResources() {
		return computingResources;
	}
	
	public void setComputingResources(ArrayList<Double> computingResources) {
		this.computingResources = computingResources;
	}
	
}
