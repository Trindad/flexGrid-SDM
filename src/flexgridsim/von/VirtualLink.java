package flexgridsim.von;

import java.util.ArrayList;

import flexgridsim.Flow;

public class VirtualLink {

	private ArrayList<VirtualNode> nodes;
	private int slotsRequired;
	private int id;
	
	public ArrayList<VirtualNode> getNodes() {
		return nodes;
	}
	
	public void setNodes(ArrayList<VirtualNode> nodes) {
		this.nodes = nodes;
	}
	
	public int getSlotsRequired() {
		return slotsRequired;
	}
	
	public void setSlotsRequired(int slotsRequired) {
		this.slotsRequired = slotsRequired;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	
}
