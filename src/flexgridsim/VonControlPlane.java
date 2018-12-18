package flexgridsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Element;

import flexgridsim.rsa.RSA;
import flexgridsim.von.ControlPlaneForVon;
import flexgridsim.von.VirtualNode;
import flexgridsim.von.VirtualTopology;
import flexgridsim.von.mappers.Mapper;
//import flexgridsim.von.mappers.KeyLinkMapper;
import vne.VirtualNetworkEmbedding;

public class VonControlPlane implements ControlPlaneForVon {
	
	 private Map<Integer, VirtualTopology> activeVons;//vons that are accepted
	 private Map<VirtualTopology, ArrayList<Flow>> mappedFlows;//flows accepted mapped in their vons
	 private RSA rsa;
	 private Mapper mapper;
	 private PhysicalTopology pt;
	 private VirtualNetworkEmbedding vne;
	 
	 Element xml;
	 EventScheduler eventScheduler;
	 private VonStatistics statistics = VonStatistics.getVonStatisticsObject();
	 
	public VonControlPlane(Element xml, EventScheduler eventScheduler, String rsaAlgorithm, String mapper, PhysicalTopology pt, TrafficGenerator traffic) {
		 @SuppressWarnings("rawtypes")
		 Class RSAClass;
		 @SuppressWarnings("rawtypes")
		 Class VonClass;
		 
		 Database.setup(pt);
		 vne = new VirtualNetworkEmbedding();
		 
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
		 
//		 System.out.println("New event "+event.getTime());
		 if(event instanceof VonArrivalEvent) {
			 
			 newVon(((VonArrivalEvent) event).getVon());
	         mapper.vonArrival(((VonArrivalEvent) event).getVon());
			 
		 }
		 else if(event instanceof VonDepartureEvent) {

			 if(mappedFlows.containsKey(((VonDepartureEvent) event).getVon())) {

				 mapper.vonDeparture(((VonDepartureEvent) event).getVon());
				 
				try{
					 deallocateVon(((VonDepartureEvent) event).getVon().getID());
				}
					 catch (Exception e) {
					 e.printStackTrace();
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
		
		if(id < 0 || !activeVons.containsKey(id)) 
		{
			throw (new IllegalArgumentException());
		}
		else 
		{
			this.mappedFlows.put(activeVons.get(id), flows);
			
			this.pt.setComputeResourceUsed(activeVons.get(id).nodes, -1.0);
			this.statistics.acceptVon(activeVons.get(id));
		}
		
		vne.setLightpath(activeVons.get(id));
		updateDatabase();
		
		return true;
	}
	
    public void addFlowToPT(Flow flow) {
        int[] links = flow.getLinks();
        // Implements it
        for (int j = 0; j < links.length; j++) {
            pt.getLink(links[j]).reserveSlots(flow.getSlotList());
            pt.getLink(links[j]).updateNoise(flow.getSlotList(), flow.getModulationLevel());
        }  
    }

	
	public boolean blockVon(int id) {
	
		if(id < 0 || !this.activeVons.containsKey(id)) 
		{
			throw (new IllegalArgumentException());
		}
		
		this.statistics.blockVon(activeVons.get(id));
		activeVons.remove(id);
	
		return true;
	}

	/**
	 * Update the database status
	 */
	private void updateDatabase() {
		
		Collection<ArrayList<Flow>> it = mappedFlows.values();
		
		
		for(ArrayList<Flow> flows : it) {
			
			for(Flow flow : flows) {
				
				Database.getInstance().totalTransponders += 2;
				Database.getInstance().usedTranspoders[flow.getSource()] += 1;
				Database.getInstance().usedTranspoders[flow.getDestination()] += 1;
				Database.getInstance().flowCount += 1;
			}
		}
		
		
		Database.getInstance().availableTransponders = pt.getNumberOfAvailableTransponders();
		
		for (int i = 0; i < pt.getNumLinks(); i++) {
			int available = pt.getLink(i).getSlotsAvailable();
			
			Database.getInstance().slotsAvailable.replace((long) i, available);
			Database.getInstance().slotsOccupied.replace((long) i, pt.getNumSlots() * pt.getCores() - available);
		}
		
		//TODO: do NOT forget
		Database.getInstance().meanCrosstalk = null;
		Database.getInstance().vne = vne;
		
		Database.getInstance().nVons = this.activeVons.size();//number of active vons
		
		
		Database.dataWasUpdated();
	}

	public void updateControlPlane(PhysicalTopology newPT) {
		
		pt.updateEverything(newPT);
	}
	
	public boolean deallocateVon(int id) {
		
		
		if(activeVons.containsKey(id)) {
			
			for(Flow flow : mappedFlows.get(activeVons.get(id)) ) {
				
				this.pt.setComputeResourceUsed(activeVons.get(id).nodes, 1.0);
				
				RemoveFlowFromPhysicalTopology(flow, flow.getLinks());
				
				rsa.flowDeparture(flow);
				
//				System.out.println("Flow departure: "+flow);
			}
			
			if(mappedFlows.containsKey(activeVons.get(id))) {
				
				mappedFlows.remove(activeVons.get(id));
//				System.out.println("VON departure complete... ");
			}
			else {
				System.out.println("Something wrong occur in VON departure process... ");
				throw (new IllegalArgumentException());
			}
			
			vne.removeLightpaths(activeVons.get(id));
			activeVons.remove(id);
			
//			System.out.println("VON Departure ID: "+id);
			updateDatabase();
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
	
	private ArrayList<Integer> getMatchingLinks(int []l1, int []l2) {
		
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
			
			double xt = 0;
			xt = xt + pt.canAcceptCrosstalk(links, slotList, db);
			
			double xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
			
			if(xti < 0 && xti >= db) {
				return false;
			}
			
			for(VirtualTopology von : mappedFlows.keySet()) {
				
				for(Flow f : mappedFlows.get(von)) {
					
				ArrayList<Integer> matching =  getMatchingLinks(links, f.getLinks());
				
				if(!matching.isEmpty()) {
					int c = slotList.get(0).c;
					LinkedList<Integer> adj = pt.getLink(0).getAdjacentCores(c);
					ArrayList<Slot> t = getMatchingSlots(slotList, f.getSlotList(), adj);
					
					if(!t.isEmpty()) 
					{
						xt = xt + pt.canAcceptInterCrosstalk(f, matching, f.getSlotList(), t);
						
						xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
						
						if(xti < 0 && xti >= db) {
							return false;
						}
					}
					else
					{
						xt = xt + pt.canAcceptInterCrosstalk(f, matching, f.getSlotList());
						
						xti = xt > 0 ? convertToDB(xt) : 0.0f;//db
						
						if(xti < 0 && xti >= db) {
							return false;
						}
					}
				}
				
			}
		}
	
		xt = xt > 0 ? convertToDB(xt) : 0.0f;//db
		return (xt == 0 || xt < db);
	}
}
