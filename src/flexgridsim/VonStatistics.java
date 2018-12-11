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
	}
	
	public void calculatingStatistics() {
		System.out.println("Calculating a set of metrics...");
		System.out.println("arrivals: "+arrivals+" departures: "+departures);
		if(arrivals == departures) {
			
			plotter.addDotToGraph("acceptance", arrivals, ((float) this.vonAcceptedRate) / ((float) requiredBandwidth));
			System.out.println("Acceptance rate: "+((float) this.vonAcceptedRate) / ((float) requiredBandwidth));
			plotter.addDotToGraph("block", arrivals, ((float) this.vonBlockedRate) / ((float) requiredBandwidth));
			plotter.addDotToGraph("linkload", arrivals, getLinkLoad());
			
		}
		else {
			System.out.println("Something wrong occured...");
		}
	}
	
	
	
	 private double getLinkLoad() {
		
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
		
		double linkLoad = Math.sqrt( ( 1.0 / ( (double)pt.getNumLinks() - 1.0 ) ) * b);
				
		System.out.println("Link load: "+linkLoad);
		
		return linkLoad;
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
			
			if (this.arrivals % 1000 == 0){
			 	//TODO
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
			 vonBlockedRate += link.getBandwidth();
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
			 vonAcceptedRate += link.getBandwidth();
		 }
	}
}
