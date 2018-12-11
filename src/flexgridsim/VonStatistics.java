package flexgridsim;

import java.util.ArrayList;
import java.util.Iterator;

import flexgridsim.von.VirtualLink;
import flexgridsim.von.VirtualTopology;

public class VonStatistics {

	private static VonStatistics singletonObject;
	private PhysicalTopology pt;
	private TrafficGenerator traffic;
	
	private int arrivals;
	private int departures;
	private int vonAcceptedRate = 0;
	private int vonBlockedRate = 0;
	private int [][]pairOfNodesBlocked;
	private int requiredBandwidth = 0;
	
	public static synchronized VonStatistics getVonStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new VonStatistics();
        }
        return singletonObject;
    }
	
	public void configuration(PhysicalTopology pt, TrafficGenerator traffic) {
	
		this.pt = pt;
		this.traffic = traffic;
		this.arrivals = 0;
	}
	
	public void calculatingStatistics() {
		
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
	 
	 public void blockVOn() {
		 vonBlockedRate++;
		 
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

	public void acceptVon(flexgridsim.von.VirtualTopology virtualTopology, ArrayList<Flow> flows) {
		vonAcceptedRate++;
	}
}
