package flexgridsim.von;

import java.util.ArrayList;

import flexgridsim.Flow;

public class VirtualLink {

	public static int ID = 1;
	
	private VirtualNode source;
	private VirtualNode destination;
	private int bandwidth;
	private int id;
	
	public VirtualLink(VirtualNode source, VirtualNode destination) {
		id = VirtualLink.ID++;
		
		this.setSource(source);
		this.setDestination(destination);
	}
	
	public int getBandwidth() {
		return bandwidth;
	}
	
	public void setBandwidth(int slotsRequired) {
		this.bandwidth = slotsRequired;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public VirtualNode getDestination() {
		return destination;
	}

	public void setDestination(VirtualNode destination) {
		this.destination = destination;
	}

	public VirtualNode getSource() {
		return source;
	}

	public void setSource(VirtualNode source) {
		this.source = source;
	}
	
	
}
