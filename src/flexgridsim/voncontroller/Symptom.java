package flexgridsim.voncontroller;

import flexgridsim.PhysicalTopology;
import vne.VirtualNetworkEmbedding;

public class Symptom {
	
	private PhysicalTopology pt;
	private VirtualNetworkEmbedding vne;
	private double linkLoad;
	private double bbr;
	private double acceptance;
	private int transponders;
	private int availableTransponders;
	private double cost;
	
	public Symptom() {
		
	}
	
	
	public PhysicalTopology getPhysicalTopology() {
		return pt;
	}


	public VirtualNetworkEmbedding getVne() {
		return vne;
	}


	public double getBandwidthBlockingRatio() {
		return bbr;
	}


	public double getLinkLoad() {
		return linkLoad;
	}


	public double getCost() {
		return cost;
	}


	public double getAcceptance() {
		return acceptance;
	}


	public void setAcceptance(double acceptance) {
		this.acceptance = acceptance;
	}


	public int getnTranspondersActived() {
		return transponders;
	}


	public int getAvailableTransponders() {
		return availableTransponders;
	}
}
