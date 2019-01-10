package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	private int [][]pairOfNodesBandwidthRequiered;
	private double requiredBandwidth;
	private double linkLoad;
	private double bandwidthBlocked;
	
	private int nVons;
	
	private Map<VirtualTopology, Integer> bandwidths;
	private Map<VirtualTopology, Integer> computeResource;
	private Map<VirtualTopology, Integer> hops;
	
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
		this.departures = 0;
		this.vonAcceptedRate = 0;
		this.vonBlockedRate = 0;
		pairOfNodesBlocked = new int[pt.getNumNodes()][pt.getNumNodes()];
		pairOfNodesBandwidthRequiered = new int[pt.getNumNodes()][pt.getNumNodes()];
		this.linkLoad = 0;
		
		bandwidths = new HashMap<VirtualTopology, Integer> ();
		computeResource = new HashMap<VirtualTopology, Integer> ();
		hops = new HashMap<VirtualTopology, Integer> ();
	}
	
	public void calculatingStatistics() {
		System.out.println("Calculating a set of metrics...");
		if(arrivals == departures) {
			
			plotter.addDotToGraph("acceptance", arrivals, ((float) this.vonAcceptedRate) / ((float) arrivals));
			plotter.addDotToGraph("block", arrivals, ((float) this.vonBlockedRate) / ((float) arrivals));
			plotter.addDotToGraph("mbbr", arrivals, ((float) this.bandwidthBlocked) / ((float) requiredBandwidth));
			plotter.addDotToGraph("linkload", arrivals, getLinkLoad());
			
			System.out.println("Arrivals: " + arrivals+" Departures: "+departures);
			System.out.println("acceptance: "+((float) this.vonAcceptedRate) / ((float) arrivals)+" blocked: "+((float) this.vonBlockedRate) / ((float) arrivals)+" bbr: "+ ((float) this.bandwidthBlocked) / ((float) requiredBandwidth));
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
		
		if(bandwidths.isEmpty()) {
			return 0;
		}
		
		double revenue = 0;
		double cost = 0;
		
		for(VirtualTopology von : bandwidths.keySet()) {
			
			revenue += ( bandwidths.get(von) + computeResource.get(von) );
			cost += ( bandwidths.get(von) * hops.get(von) ) + computeResource.get(von);
		}

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
	
	public void addEvent(VirtualTopology von) {
		
		try {
			for(VirtualLink link : von.links) {
				this.requiredBandwidth += link.getBandwidth();
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
			 pairOfNodesBlocked[link.getSource().getPhysicalNode()][link.getDestination().getPhysicalNode()] += link.getBandwidth();
			 pairOfNodesBandwidthRequiered[link.getSource().getPhysicalNode()][link.getDestination().getPhysicalNode()] += link.getBandwidth();
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
		
		
		 int a = 0, b = 0, c = 0;
		 for(VirtualLink link : von.links) {

			 a += link.getBandwidth();
			 b += link.getPhysicalLinks().length;
			 c += link.getSource().getComputeResource();
			 
			 pairOfNodesBandwidthRequiered[link.getSource().getPhysicalNode()][link.getDestination().getPhysicalNode()] += link.getBandwidth();
		 }
		 
		 bandwidths.put(von, a);
		 hops.put(von, b);
		 computeResource.put(von, c);
		 
		 vonAcceptedRate ++;
		 
		if (this.nVons % 100 == 0 && traffic.dynamic == false) {
			
			plotter.addDotToGraph("revenue-cost", arrivals, getRevenueToCostRatio());
		}
	}

	public double getBandwidthBlockingRatio() {
		
		return ((double) this.bandwidthBlocked) / ((double) this.requiredBandwidth);
	}

	public double getBandwidthBlockingRatioPerLink(int i) {
		
		int source = pt.getLink(i).getSource(), destination = pt.getLink(i).getDestination();
		
		if(pairOfNodesBlocked[source][destination] == 0 && pairOfNodesBandwidthRequiered[source][destination] == 0) return 0;
		
		return ((double)pairOfNodesBlocked[source][destination] / (double)pairOfNodesBandwidthRequiered[source][destination]);
	}


	public double[] getAllocatedBandwidth() {
		// TODO Auto-generated method stub
		return null;
	}

	public double[] getComputing() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getAcceptance() {
		
		return ((double) this.vonAcceptedRate) / ((double) arrivals);
	}

	public void updateStatisticsDeparture(VirtualTopology von) {
		
		try {
			bandwidths.remove(von);
			hops.remove(von);
			computeResource.remove(von);
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
