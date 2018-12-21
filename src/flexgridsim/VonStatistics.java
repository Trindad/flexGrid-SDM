package flexgridsim;

import java.util.ArrayList;
import java.util.Iterator;

import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualTopology;

/**
 * Statistics for VON topologies using SDM technology
 * 
 * @author trindade
 *
 */

public class VonStatistics {

	private static VonStatistics singletonObject;
	private PhysicalTopology pt;
	private TrafficGenerator traffic;
	
	private int arrivals;
	private int departures;
	private int vonAcceptedRate;
	private int vonBlockedRate;
	private int [][]pairOfNodesBlocked;
	private int requiredBandwidth;
	private double linkLoad;
	private double bandwidthBlocked;
	
	private ArrayList<Integer> bandwidths;
	private ArrayList<Integer> computeResource;
	private ArrayList<Integer> hops;
	private int nVons;
	
	private OutputManager plotter;
	
	public static synchronized VonStatistics getVonStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new VonStatistics();
        }
        return singletonObject;
    }
	
	public void configuration(OutputManager plotter, PhysicalTopology pt, TrafficGenerator traffic) {
	
		this.pt = pt;
		this.traffic = traffic;
		this.arrivals = 0;
		this.plotter = plotter;
		this.arrivals = 0;
		this.departures = 0;
		this.vonAcceptedRate = 0;
		this.vonBlockedRate = 0;
		pairOfNodesBlocked = new int[pt.getNumNodes()][pt.getNumNodes()];
		this.linkLoad = 0;
		
		bandwidths = new ArrayList<Integer> ();
		computeResource = new ArrayList<Integer> ();
		hops = new ArrayList<Integer> ();
	}
	
	public void calculatingStatistics() {
		System.out.println("Calculating a set of metrics...");
		if(arrivals == departures) {
			
			plotter.addDotToGraph("acceptance", arrivals, ((float) this.vonAcceptedRate) / ((float) arrivals));
			plotter.addDotToGraph("block", arrivals, ((float) this.vonBlockedRate) / ((float) arrivals));
			plotter.addDotToGraph("mbbr", arrivals, ((float) this.bandwidthBlocked) / ((float) requiredBandwidth));
			plotter.addDotToGraph("linkload", arrivals, getLinkLoad());
		}
		else {
			System.out.println("Something wrong occured...");
			throw (new IllegalArgumentException());
		}
	}
	
	
	
	 protected double getLinkLoad() {
		
		double a = 0;
		for(int i = 0; i < pt.getNumLinks(); i++) {
			
			a += ( (pt.getNumSlots() * pt.getCores()) - pt.getLink(i).getNumFreeSlots());
		}

		a = (double)a / (double)pt.getNumLinks();
		
		double b = 0;
		
		for(int i = 0; i < pt.getNumLinks(); i++) {
			
			double temp = (double)( (pt.getNumSlots() * pt.getCores()) - pt.getLink(i).getNumFreeSlots());
			b += Math.pow(temp - a, 2);
		}
		
		linkLoad = Math.sqrt( ( 1.0 / ( (double)pt.getNumLinks() - 1.0 ) ) * b);
				
//		System.out.println("Link load: "+linkLoad);
		
		return linkLoad;
	}
	 
	/**
	 * Long-term revenue to cost ratio
	 * @return
	 */
	protected double getRevenueToCostRatio() {
		
		if(bandwidths.size() <= 0) {
			
			return 0;
		}
		
		double revenue = 0;
		
		for(int i = 0; i < bandwidths.size(); i++) {
			
			revenue += ( bandwidths.get(i) + computeResource.get(i) );
		}
		
		double cost = 0;
		
		for(int i = 0; i < bandwidths.size(); i++) {
			
			cost += ( bandwidths.get(i) * hops.get(i) ) + computeResource.get(i);
		}

		
		hops.clear();
		bandwidths.clear();
		computeResource.clear();
//		System.out.println("Long-term revenue to cost ratio: "+revenue/cost);
		
		return (revenue/cost);
	}

	public void addEvent(Event event) {
		
		try {
			if(event instanceof VonArrivalEvent) {
				
				VirtualTopology von = ((VonArrivalEvent) event).getVon();
				for(VirtualLink link : von.links) {
					this.requiredBandwidth += link.getBandwidth();
				}
				
				this.arrivals++;
			}
			else if(event instanceof VonDepartureEvent) {
				this.departures++;
			}
			
			if (this.arrivals % 10 == 0 && traffic.dynamic == true) {
				
				plotter.addDotToGraph("revenue-cost", arrivals, getRevenueToCostRatio());
			}
		}
		catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
	 }
	 
	 public void blockVon(VirtualTopology von) {
		 
		 for(VirtualLink link : von.links) {
			 bandwidthBlocked += link.getBandwidth();
			 pairOfNodesBlocked[link.getSource().getPhysicalNode()][link.getDestination().getPhysicalNode()] += 1;
		 }
		 
		 vonBlockedRate++;
		 nVons++;

		if (this.nVons % 10 == 0 && traffic.dynamic == false) {
			
			plotter.addDotToGraph("revenue-cost", arrivals, getRevenueToCostRatio());
		}
	 }
	 
	public void finish()
    {
        singletonObject = null;
    }

	public void addEvents(EventScheduler events) {
		
		Event event;

		Iterator<Event> it = events.getEvents();
		
		while(it.hasNext()) {
			 
			event = it.next();
			
			addEvent(event);
			
		}
		
	}

	public void acceptVon(VirtualTopology von) {
		
		 for(VirtualLink link : von.links) {

			 
			 bandwidths.add(link.getBandwidth());
			 hops.add(link.getPhysicalLinks().length);
			 computeResource.add( link.getSource().getComputeResource() );
		 }
		 
		 vonAcceptedRate ++;
		 
		if (this.nVons % 100 == 0 && traffic.dynamic == false) {
			
			plotter.addDotToGraph("revenue-cost", arrivals, getRevenueToCostRatio());
		}
	}

	public double getBandwidthBlockingRatio() {
		
		return ((double) this.bandwidthBlocked) / ((double) requiredBandwidth);
	}
}
