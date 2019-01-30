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
import javafx.scene.chart.PieChart.Data;
import vne.VirtualNetworkEmbedding;

/**
 * 
 * @author trindade
 *
 */
public class VonControlPlane implements ControlPlaneForVon {
	
	 private Map<Integer, VirtualTopology> activeVons;//vons that are accepted
	 private Map<VirtualTopology, Map<Long, Flow>> mappedFlows;//flows accepted mapped in their vons
	 private RSA rsa;
	 private Mapper mapper;
	 private PhysicalTopology pt;
	 private VirtualNetworkEmbedding vne;
	 public boolean mape;
	 
	 private Map<Flow, Integer> flowCount;
	 
	 public double time = 0;
	 
	 Element xml;
	 EventScheduler eventScheduler;
	 private VonStatistics statistics = VonStatistics.getVonStatisticsObject();
	 
	public VonControlPlane(Element xml, EventScheduler eventScheduler, String rsaAlgorithm, String mapper, PhysicalTopology pt, TrafficGenerator traffic, boolean mape) {
		 @SuppressWarnings("rawtypes")
		 Class RSAClass;
		 @SuppressWarnings("rawtypes")
		 Class VonClass;
		 
		this.mape = mape;
		 
		Database.setup(pt);
		vne = new VirtualNetworkEmbedding();
		
		this.pt = pt;
		this.activeVons = new HashMap<Integer, VirtualTopology>();
		this.mappedFlows = new HashMap<VirtualTopology, Map<Long, Flow>>();
		this.xml = xml;
		this.eventScheduler = eventScheduler;
		
		this.flowCount = new HashMap<Flow, Integer>();
		
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
		    Database.getInstance().closenessCentrality = pt.getClosenessCentrality();
		} 
		catch (Throwable t) 
		{
		    t.printStackTrace();
		}
		 
	 }
	 
	 public void newEvent(Event event) {
		 
		 
		 if(event instanceof VonArrivalEvent) {
			 
			 double t = ((VonArrivalEvent) event).getVon().arrivalTime;
			 time = time <  t ? t : time;
			 
			 newVon(((VonArrivalEvent) event).getVon());
	         mapper.vonArrival(((VonArrivalEvent) event).getVon());
			 
		 }
		 else if(event instanceof VonDepartureEvent) {

			 if(mappedFlows.containsKey(((VonDepartureEvent) event).getVon())) {

				 mapper.vonDeparture(((VonDepartureEvent) event).getVon());
				 
				if(activeVons.containsKey(((VonDepartureEvent) event).getVon().getID())) {
					 deallocateVon(((VonDepartureEvent) event).getVon().getID());
			 	}
			 }
			 else
			 {
				 this.activeVons.remove(((VonDepartureEvent) event).getVon().getID());
			 }
		 }
		 else {
			 System.out.println("This type of event doen't exist!");
			 throw (new IllegalArgumentException());
		 }
	 }
	
	private void newVon(VirtualTopology von) {
		this.activeVons.put(von.getID(), von);
	}

	public boolean acceptVon(int id, Map<Long, Flow> flows) {
		
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
		
		if(this.mape == true) {
			
			System.out.println("ACCEPTED: "+activeVons.get(id).getID());
			vne.setLightpath(activeVons.get(id));
			updateDatabase(flows);
			Orchestrator.getInstance().run();
			Hooks.runPendingReconfiguration(pt, this, vne);
			Hooks.runPendingRedirectingLightpath(pt, this, vne);
		}
		
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
		
		if(this.mape == true) {
			
			Database.getInstance().bbr = statistics.getBandwidthBlockingRatio();
			Database.getInstance().acceptance = statistics.getAcceptance();
			Orchestrator.getInstance().run();
		}
	
		return true;
	}

	/**
	 * Update the database status
	 */
	private void updateDatabase(Map<Long, Flow> flows) {
			
		for(Long key: flows.keySet()) {
			Flow flow = flows.get(key);
			if(!flow.isAccepeted()) continue;
			
			Database.getInstance().totalTransponders += 2;
			Database.getInstance().flowCount += 1;
			
			Database.getInstance().usedBandwidth[flow.getSource()] += flow.getRate();
			Database.getInstance().usedBandwidth[flow.getDestination()] += flow.getRate();
			
			Database.getInstance().usedTransponders[flow.getSource()] += 1;
			Database.getInstance().usedTransponders[flow.getDestination()] += 1;
			
			Database.getInstance().computing[flow.getSource()] += flow.getComputingResourceSource();
			Database.getInstance().computing[flow.getDestination()] += flow.getComputingResourceDestination();
			Database.getInstance().totalComputeResource += flow.getComputingResource();
			
			flowCount.put(flow, flow.getComputingResource());
			
			getLinkInPath(flow); 
		}
		
		Database.getInstance().meanTransponders = (double)Database.getInstance().totalTransponders / (double) (pt.getNumNodes() * pt.transponders);
		Database.getInstance().availableTransponders = pt.getNumberOfAvailableTransponders();
	
		Database.getInstance().totalNumberOfTranspondersAvailable = 0;
		for(int i : Database.getInstance().availableTransponders) {
			Database.getInstance().totalNumberOfTranspondersAvailable += i;
		}
		
		double countTransponders = 0;
		for(int i = 0; i < pt.getNumNodes(); i++)
		{
			if(pt.getNode(i).getTransponders() <= 2) {
				countTransponders++;
			}
		}
		
		double countAvailableSlots = 0;
		for(int i = 0; i < pt.getNumLinks(); i++)
		{
			double y = pt.getNumSlots() * pt.getCores();
			if(((double)Database.getInstance().slotsAvailablePerLink[i]/y) < statistics.getAvailableSlotsRatio()) {
				countAvailableSlots++;
			}
		}
		
		System.out.println("COUNT: "+countAvailableSlots+" "+ statistics.getAvailableSlotsRatio());
		Database.getInstance().availableSlotsB = statistics.getAvailableSlotsRatio() >= 0.5 && countAvailableSlots >= 1  && countAvailableSlots < pt.getNumLinks() ? 1 : 0;
		Database.getInstance().fragmentationB = statistics.getFragmentationRatio() >= 0.5 ? 1 : 0;
		Database.getInstance().availableTranspondersB = (double)countTransponders <= ((double)pt.getNumNodes() * 0.4) && Database.getInstance().totalTransponders >= ((double)pt.getNumNodes() * pt.transponders * 0.5) && countTransponders >= 1 ? 1 : 0; 
		
		for (int i = 0; i < pt.getNumLinks(); i++) {
			int available = pt.getLink(i).getSlotsAvailable();
			
			Database.getInstance().slotsOccupied.replace((long) i, (pt.getNumSlots() * pt.getCores() - available) );
			Database.getInstance().slotsAvailable.replace((long) i, available);
			Database.getInstance().slotsAvailablePerLink[i] = available;
			Database.getInstance().bbrPerPair[i] = statistics.getBandwidthBlockingRatioPerLink(i);
			Database.getInstance().xtLinks[i] = pt.getLink(i).getXT();
		}
		
		Database.getInstance().meanCrosstalk = pt.getMeanCrosstalk();
		Database.getInstance().vne = vne;
		Database.getInstance().linkLoad = statistics.getLinkLoad();
		Database.getInstance().cost = statistics.getRevenueToCostRatio();
		Database.getInstance().bbr = statistics.getBandwidthBlockingRatio();
		
		Database.getInstance().nVons = this.activeVons.size();//number of active vons
		Database.getInstance().acceptance = statistics.getAcceptance();
		
		Database.dataWasUpdated();
	}

	private void getLinkInPath(Flow flow) {
		
		for(int i : flow.getLinks()) {
			
			Database.getInstance().numberOfLightpaths[i] += 1;
		}
	}

	public void updateControlPlane(PhysicalTopology newPT) {
		
		pt.updateEverything(newPT);
	}
	
	public boolean deallocateVon(int id) {
		
		
		if(activeVons.containsKey(id)) {
			
			
			for(Long key : mappedFlows.get(activeVons.get(id)).keySet() ) {
				
				Flow flow = mappedFlows.get(activeVons.get(id)).get(key);
				if(!flow.isAccepeted()) continue;
				
				if(this.mape == true) {
					int c = flowCount.get(flow) - flow.getComputingResource();
					flowCount.put(flow, c);
				}
				
				pt.getNode(flow.getSource()).updateTransponders(1);
				pt.getNode(flow.getDestination()).updateTransponders(1);
				pt.setComputeResourceUsed(activeVons.get(id).nodes, 1.0);
				
				RemoveFlowFromPhysicalTopology(flow, flow.getLinks());
				
				rsa.flowDeparture(flow);
				
				if(this.mape == true) {
				
					Database.getInstance().totalTransponders -= 2;
					Database.getInstance().usedTransponders[flow.getSource()] -= 1;
					Database.getInstance().usedTransponders[flow.getDestination()] -= 1;
					
					double countTransponders = 0;
					for(int i = 0; i < pt.getNumNodes(); i++)
					{
						if(pt.getNode(i).getTransponders() <= 2) {
							countTransponders++;
						}
					}
					
					Database.getInstance().flowCount -= 1;
					
					
					Database.getInstance().usedBandwidth[flow.getSource()] -= flow.getRate();
					Database.getInstance().usedBandwidth[flow.getDestination()] -= flow.getRate();
					Database.getInstance().computing[flow.getSource()] -= flow.getComputingResourceSource();
					Database.getInstance().computing[flow.getDestination()] -= flow.getComputingResourceDestination();
					Database.getInstance().totalComputeResource -= flow.getComputingResource();
					
					
					getLinkInPath(flow); 
					
					for (int i : flow.getLinks()) {
						int available = pt.getLink(i).getSlotsAvailable();
						
						Database.getInstance().slotsOccupied.replace((long) i, (pt.getNumSlots() * pt.getCores() - available) );
						Database.getInstance().slotsAvailable.replace((long) i, available);
						Database.getInstance().slotsAvailablePerLink[i] = available;
						Database.getInstance().bbrPerPair[i] = statistics.getBandwidthBlockingRatioPerLink(i);
						Database.getInstance().xtLinks[i] = pt.getLink(i).getXT();
						
						Database.getInstance().numberOfLightpaths[i] -= 1;
					}
					
					
					double countAvailableSlots = 0;
					for(int i = 0; i < pt.getNumLinks(); i++)
					{
						if(Database.getInstance().slotsAvailablePerLink[i] <= ( (pt.getCores() * pt.getNumSlots())/4 )) {
							countAvailableSlots++;
						}
					}
					
					Database.getInstance().availableSlotsB = statistics.getAvailableSlotsRatio() >= 0.5 && countAvailableSlots <= (pt.getNumLinks() * 0.3) && countAvailableSlots >= 1 ? 1 : 0;
					Database.getInstance().fragmentationB = statistics.getFragmentationRatio() >= 0.4 ? 1 : 0;
					Database.getInstance().availableTranspondersB = (double)countTransponders >= ((double)pt.getNumNodes() * 0.4) && Database.getInstance().totalTransponders >= ((double)pt.getNumNodes() * pt.transponders * 0.5) && countTransponders >= 1 ? 1 : 0; 
					
					
				}
			}
			
			statistics.updateStatisticsDeparture(activeVons.get(id));
			
			if(this.mape == true) 
			{
				vne.removeLightpaths(activeVons.get(id));
				
				Database.getInstance().meanTransponders = (double)Database.getInstance().totalTransponders / (double) (pt.getNumNodes() * pt.transponders);
				Database.getInstance().availableTransponders = pt.getNumberOfAvailableTransponders();
				
				Database.getInstance().totalNumberOfTranspondersAvailable = 0;
				for(int i : Database.getInstance().availableTransponders) {
					Database.getInstance().totalNumberOfTranspondersAvailable += i;
				}
				
				Hooks.runPendingReconfiguration(pt, this, vne);
				Hooks.checkDone(pt);
			
				Database.getInstance().meanCrosstalk = pt.getMeanCrosstalk();
				Database.getInstance().vne = vne;
				Database.getInstance().linkLoad = statistics.getLinkLoad();
				Database.getInstance().cost = statistics.getRevenueToCostRatio();
				
				Database.getInstance().nVons = this.activeVons.size()-1;//number of active vons
				
				
				Database.dataWasUpdated();
			}

			if(mappedFlows.containsKey(activeVons.get(id))) {
				
				mappedFlows.remove(activeVons.get(id));
			}
			else 
			{
				System.out.println("Something wrong occur in VON departure process... ");
				throw (new IllegalArgumentException());
			}
			
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
				
				for(Long key: mappedFlows.get(von).keySet()) {
					Flow f = mappedFlows.get(von).get(key);
					
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

	public Map<Long, Flow> getActiveFlows() {
		
		Map<Long, Flow> flows = new HashMap<Long, Flow>();
		
		for(VirtualTopology von : mappedFlows.keySet()) {
			flows.putAll(mappedFlows.get(von));
		}
		
		return flows;
	}

	public void removeFlowFromPT(Flow flow, PhysicalTopology ptTemp) {

    	int[] links;
        links = flow.getLinks();
        
    	for (int j : links) {
    		ptTemp.getLink(j).releaseSlots(flow.getSlotList());
    		ptTemp.getLink(j).updateNoise(flow.getSlotList(), flow.getModulationLevel());
    		ptTemp.getLink(j).resetCrosstalk(flow.getSlotList());
        }
    	
    }
}
