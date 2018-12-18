package vne;

import java.util.ArrayList;

import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualTopology;

public class VirtualNetworkEmbedding {
	
	public static int LightpathCounter = 1;
	
	private ArrayList<Integer> nodes;
	private ArrayList<Lightpath> links;
	
	public VirtualNetworkEmbedding() {
		
		nodes = new ArrayList<Integer>();
		links = new ArrayList<Lightpath>();
	}
	
	public void setLightpath(VirtualTopology von) {

		for(VirtualLink link : von.links) {
			
			int index = match( link.getPhysicalLinks() );
			
			if(index >= 0) {
				links.get(index).VonIDs.add(von.getID());
				links.get(index).bandwidth += link.getBandwidth();
			}
			else 
			{
				Lightpath e = new Lightpath();
				e.links =  link.getPhysicalLinks();
				e.bandwidth = link.getBandwidth();
				e.VonIDs.add(von.getID());
				nodes.add(link.getSource().getPhysicalNode());
				nodes.add(link.getDestination().getPhysicalNode());
				
				links.add(e);
			}
		}
	}
	
	public void removeLightpaths(VirtualTopology von) {
		
		int index = -1;
		for(VirtualLink link : von.links) {
			
			index = match( link.getPhysicalLinks() );
			
			if(index >= 0) 
			{
				links.get(index).VonIDs.remove(von.getID());
				links.get(index).bandwidth -= link.getBandwidth();
				
				if(links.get(index).bandwidth <= 0) {
					links.remove(index);
				}	
			}
		}
	}
	
	private int match(int []path) {
		
		int n = 0;
		for(Lightpath link : links) {
			
			if(path.toString().equals(link.links.toString())) {
					return n;
				
			}
			
			n++;
		}
		
		return -1;
	}
	
	private class Lightpath {
		 
		public int []links;
		public ArrayList<Integer> VonIDs;
		public int ID;
		public double bandwidth;
		
		public Lightpath() {
			
			ID = VirtualNetworkEmbedding.LightpathCounter++;
			VonIDs = new ArrayList<>();
		}
		
	}

}
