package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import flexgridsim.rsa.RSA;
import flexgridsim.von.ControlPlaneForVon;
import flexgridsim.von.VirtualTopology;
import flexgridsim.von.mappers.Mapper;

public class VonControlPlane implements ControlPlaneForVon {
	
	 private Map<Integer, VirtualTopology> activeVons;//vons that are accepted
	 private Map<VirtualTopology, ArrayList<Flow>> mappedFlows;//flows accepted mapped in their vons
	 private RSA rsa;
	 private Mapper mapper;
	 private PhysicalTopology pt;
	 
	 Element xml;
	 EventScheduler eventScheduler;
	 private MyStatistics statistics = MyStatistics.getMyStatisticsObject();
	 
	 public VonControlPlane(Element xml, EventScheduler eventScheduler, String rsaAlgorithm, Mapper mapper, PhysicalTopology pt) {
		 
		 this.pt = pt;
		 this.activeVons = new HashMap<Integer, VirtualTopology>();
		 this.mappedFlows = new HashMap<VirtualTopology, ArrayList<Flow>>();
		 this.mapper = mapper;
		 this.xml = xml;
		 this.eventScheduler = eventScheduler;
	 }
	 
	 public void newEvent(Event event) {
		 
		 if(event instanceof VonArrivalEvent) {
			 
			 newVon(((VonArrivalEvent) event).getVon());
	         mapper.vonArrival(((VonArrivalEvent) event).getVon(), rsa, pt);
			 
		 }
		 else if(event instanceof VonDepartureEvent) {

			 if(mappedFlows.containsKey(((VonDepartureEvent) event).getVon())) {

				 mapper.vonDeparture(((VonDepartureEvent) event).getVon());
				 
				 if(deallocateVon(((VonDepartureEvent) event).getVon().getID())) {
					 throw (new IllegalArgumentException());
				 }
			 }
			 else
			 {
				 this.activeVons.remove(((VonDepartureEvent) event).getVon().getID());
			 }
		 }
		 else {
			 System.out.println("This type of event doen't exist!");
			 return;
		 }
	 }
	
	private void newVon(VirtualTopology von) {
		this.activeVons.put(von.getID(), von);
	}

	public boolean acceptVon(int id, ArrayList<Flow> flows) {
		
		if(id < 0) 
		{
			throw (new IllegalArgumentException());
		}
		else if(!activeVons.containsKey(id)) 
		{
			throw (new IllegalArgumentException());
		}
		else 
		{
			this.mappedFlows.put(activeVons.get(id), flows);
			this.statistics.acceptVon(activeVons.get(id), flows);
		}
		
		return true;
	}

	
	public boolean blockVon(int id) {
		
		if (id < 0) 
		{
			throw (new IllegalArgumentException());
	    } 
		else 
		{
			if(!this.activeVons.containsKey(id)) 
			{
				return true;
			}
			
			activeVons.remove(id);
			
		}
		return false;
	}

	
	public boolean deallocateVon(int id) {
		
		if(activeVons.containsKey(id)) {
			
			for(Flow flow : mappedFlows.get(activeVons.get(id)) ) {
				
				RemoveFlowFromPhysicalTopology(flow, flow.getLinks());
			}
			
			mappedFlows.remove(activeVons.get(id));
			activeVons.remove(id);
			
			return true;
		}
		
		return false;
	}

	
	private void RemoveFlowFromPhysicalTopology(Flow flow, int []links) {
		
    	for (int j = 0; j < links.length; j++) {
    		
            pt.getLink(links[j]).releaseSlots(flow.getSlotList());
            pt.getLink(links[j]).updateNoise(flow.getSlotList(), flow.getModulationLevel());
        }
	}

	public VirtualTopology getVon(int id) {
		
		return this.activeVons.get(id);
	}
}
