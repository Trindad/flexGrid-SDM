package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Element;

import flexgridsim.rsa.RSA;
import flexgridsim.von.ControlPlaneForVon;
import flexgridsim.von.VirtualTopology;
import flexgridsim.von.mappers.Mapper;
//import flexgridsim.von.mappers.KeyLinkMapper;

public class VonControlPlane implements ControlPlaneForVon {
	
	 private Map<Integer, VirtualTopology> activeVons;//vons that are accepted
	 private Map<VirtualTopology, ArrayList<Flow>> mappedFlows;//flows accepted mapped in their vons
	 private RSA rsa;
	 private Mapper mapper;
	 private PhysicalTopology pt;
	 
	 Element xml;
	 EventScheduler eventScheduler;
	 private MyStatistics statistics = MyStatistics.getMyStatisticsObject();
	 
	 public VonControlPlane(Element xml, EventScheduler eventScheduler, String rsaAlgorithm, String mapper, PhysicalTopology pt, TrafficGenerator traffic) {
		 @SuppressWarnings("rawtypes")
		 Class RSAClass;
		 @SuppressWarnings("rawtypes")
		 Class VonClass;
		 
		 this.pt = pt;
		 this.activeVons = new HashMap<Integer, VirtualTopology>();
		 this.mappedFlows = new HashMap<VirtualTopology, ArrayList<Flow>>();
		 this.xml = xml;
		 this.eventScheduler = eventScheduler;
		 
		 this.pt.setGraph();
		 
		 try 
		 {
            RSAClass = Class.forName(rsaAlgorithm);
            rsa = (RSA) RSAClass.newInstance();
            rsa.simulationInterface(xml, pt, traffic);     
        } 
		catch (Throwable t) 
		{
            t.printStackTrace();
        }
		 
		 try 
		 {
            VonClass = Class.forName(mapper);
            this.mapper = (Mapper) VonClass.newInstance();
            this.mapper.simulationInterface(xml, pt, this, traffic, rsa);     
        } 
		catch (Throwable t) 
		{
            t.printStackTrace();
        }
		 
	 }
	 
	 public void newEvent(Event event) {
		 
		 System.out.println("New event "+event.getTime());
		 if(event instanceof VonArrivalEvent) {
			 
			 newVon(((VonArrivalEvent) event).getVon());
	         mapper.vonArrival(((VonArrivalEvent) event).getVon());
			 
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

	public void newEvents(EventScheduler events) {
		Event event;

		ArrayList<VirtualTopology> vons = new ArrayList<VirtualTopology>();
		Iterator<Event> it = events.getEvents();
		
		while(it.hasNext()) {
			 
			event = it.next();
			
			if(event instanceof VonArrivalEvent) {
				newVon(((VonArrivalEvent) event).getVon());
				vons.add(((VonArrivalEvent) event).getVon());
			}
		}
		
		mapper.vonArrival(vons);	
	}
	
	private double convertToDB(double p) {
		return 10.0f * Math.log10(p)/Math.log10(10);
	}
	
	private ArrayList<Slot> getMatchingSlots(ArrayList<Slot> s1, ArrayList<Slot> s2, LinkedList<Integer> adjacents) {
		
		boolean isAdjacent = false;
		for (int i : adjacents) {
			
			if(i == s2.get(0).c) {
				isAdjacent = true;	
			}
		}
		
		if (isAdjacent) {
			ArrayList<Slot> slots = new ArrayList<Slot>();
			for(Slot i: s1) 
			{
				for(Slot j: s2) 
				{
					if(i.s == j.s && !slots.contains(i)) {
						slots.add(i);
					}
				}
			}
			
			return slots;
		}
		
		return new ArrayList<Slot>();
	}
	
	private ArrayList<Integer>getMatchingLinks(int []l1, int []l2) {
		
		ArrayList<Integer> links = new ArrayList<Integer>();
		for(int i: l1) {
			
			for(int j: l2) {
				
				if(i == j) {
					links.add(i);
				}
				
			}
		}
		
		return links;
	}
	
	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> slotList, double db) {
			return true;
//			double xt = 0;
//			xt = xt + pt.canAcceptCrosstalk(links, slotList, db);
//			
//			double xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
//			
//			if(xti < 0 && xti >= db) {
//				return false;
//			}
//			
//			for(Long key: this.activeFlows.keySet()) {
//				
//				if(key == flow.getID()) {
//					continue;
//				}
//				else if(this.activeFlows.get(key).isMultipath()) 
//				{
//					int i = 0;
//					for(ArrayList<Slot> s: this.activeFlows.get(key).getMultiSlotList()) {
//						
//						ArrayList<Integer> matching = getMatchingLinks(links, this.activeFlows.get(key).getLinks(i));
//						
//						if(!matching.isEmpty()) {
//							ArrayList<Slot> t = getMatchingSlots(slotList, s, pt.getLink(0).getAdjacentCores(slotList.get(0).c));
//							
//							if(!t.isEmpty()) 
//							{
//								xt = xt + pt.canAcceptInterCrosstalk(this.activeFlows.get(key),  matching, this.activeFlows.get(key).getLinks(i), s, t);
//								
//								xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
//								
//								if(xti < 0 && xti >= db) {
//									return false;
//								}
//							}
//							else
//							{
//								xt = xt + pt.canAcceptInterCrosstalk(this.activeFlows.get(key),  matching, this.activeFlows.get(key).getLinks(i), s);
//								
//								xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
//								
//								if(xti < 0 && xti >= db) {
//									return false;
//								}
//							}
//						}
//						
//						i++;
//					}
//				}
//				else 
//				{
//					ArrayList<Integer> matching =  getMatchingLinks(links, this.activeFlows.get(key).getLinks());
//					
//					if(!matching.isEmpty()) {
//						int c = slotList.get(0).c;
//						LinkedList<Integer> adj = pt.getLink(0).getAdjacentCores(c);
//						ArrayList<Slot> t = getMatchingSlots(slotList, this.activeFlows.get(key).getSlotList(), adj);
//						
//						if(!t.isEmpty()) 
//						{
//							xt = xt + pt.canAcceptInterCrosstalk(this.activeFlows.get(key), matching, this.activeFlows.get(key).getSlotList(), t);
//							
//							xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
//							
//							if(xti < 0 && xti >= db) {
//								return false;
//							}
//						}
//						else
//						{
//							xt = xt + pt.canAcceptInterCrosstalk(this.activeFlows.get(key), matching, this.activeFlows.get(key).getSlotList());
//							
//							xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
//							
//							if(xti < 0 && xti >= db) {
//								return false;
//							}
//						}
//					}
//					
//				}
//			}
//			
//			xt = xt > 0 ? convertToDB(xt) : 0.0f;//db
//			return (xt == 0 || xt < db);
		}
}
