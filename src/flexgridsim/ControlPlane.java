/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.w3c.dom.Element;

import flexgridsim.rsa.EarliestDeadlineFirst;
import flexgridsim.rsa.ClusterDefragmentationRCSA;
import flexgridsim.rsa.ControlPlaneForRSA;
import flexgridsim.rsa.DefragmentationRCSA;
import flexgridsim.rsa.RSA;
import flexgridsim.rsa.TridimensionalClusterDefragmentationRCSA;
import flexgridsim.rsa.ZhangDefragmentationRCSA;
import flexgridsim.rsa.BalanceDefragmentationRCSA;

/**
 * The Control Plane is responsible for managing resources and
 * connection within the network.
 */
public class ControlPlane implements ControlPlaneForRSA {

    private RSA rsa;
    private DefragmentationRCSA defragmentation;
    private DefragmentationRCSA rerouting;
    private String rsaAlgorithm;
    private boolean costMKP = false;//used in batch requests 
    private double time = 0.0f;
    private PhysicalTopology pt;
    private VirtualTopology vt;
    private Map<Flow, ArrayList<LightPath> > mappedFlows; // Flows that have been accepted into the network
    private Map<Long, Flow> activeFlows; // Flows that have been accepted or that are waiting for a decision 
    private Tracer tr = Tracer.getTracerObject();
    private MyStatistics st = MyStatistics.getMyStatisticsObject();
    
    private EventScheduler eventScheduler;
    private SetOfBatches batches;
    private long nConnections = 0;
    
    /**
     * Defragmentation approaches
     */
    private boolean DFR = false;
    private double dfIndex = 0;
    private double limited = 0.03;
    private ArrayList<Cluster> clusters;
    private String typeOfReroutingAlgorithm;
	private int nExceeds = 0;
	private boolean RR = false;
	private double fi[];
	
    /**
	 * Creates a new ControlPlane object.
	 *
	 * @param xml the xml
	 * @param eventScheduler the event scheduler
	 * @param rsaModule the name of the RCSA class
	 * @param pt the network's physical topology
	 * @param vt the network's virtual topology
	 * @param traffic the traffic
	 */
    public ControlPlane(Element xml, EventScheduler eventScheduler, String rsaModule, String rsaAlgorithm, String defragmentation, String costMultipleKnapsackPloblem, String reroute, PhysicalTopology pt, VirtualTopology vt, TrafficGenerator traffic) {
        @SuppressWarnings("rawtypes")
		Class RSAClass;
        mappedFlows = new HashMap<Flow, ArrayList<LightPath>>();
        activeFlows = new HashMap<Long, Flow>();
        this.eventScheduler = eventScheduler;
        
        batches = new SetOfBatches();
        this.clusters = new ArrayList<Cluster>();
        
        this.pt = pt;
        this.vt = vt;
        this.pt.setGraph();
        
        this.setRsaAlgorithm(rsaAlgorithm);
        if(costMultipleKnapsackPloblem.equals("true") == true) 
        {
        	this.setCostMKP(true);
        }
        
        if(defragmentation.equals("TridimensionalClusterDefragmentationRCSA") == true) 
        {
        	this.DFR = true;
        	this.defragmentation = new TridimensionalClusterDefragmentationRCSA();
        	this.defragmentation.simulationInterface(xml, pt, vt, this, traffic);
        }
        else if(defragmentation.equals("ClusterDefragmentationRCSA") == true) 
        {
        	this.DFR = true;
        	this.defragmentation = new ClusterDefragmentationRCSA();
        	this.defragmentation.simulationInterface(xml, pt, vt, this, traffic);
        }
        else if(reroute.equals("BalanceDefragmentationRCSA") == true) 
        {
        	this.RR = true;
        	this.typeOfReroutingAlgorithm = reroute;
        	this.rerouting = new BalanceDefragmentationRCSA();
        	this.rerouting.simulationInterface(xml, pt, vt, this, traffic); 
        }
        else if(reroute.equals("ZhangDefragmentationRCSA") == true) 
        {
        	this.RR = true;
        	this.typeOfReroutingAlgorithm = reroute;
        	this.rerouting = new ZhangDefragmentationRCSA();
        	this.rerouting.simulationInterface(xml, pt, vt, this, traffic); 
        }
        fi = new double[pt.getNumLinks()];//for defragmentation methods
        
        try {
            RSAClass = Class.forName(rsaModule);
            rsa = (RSA) RSAClass.newInstance();
            rsa.simulationInterface(xml, pt, vt, this, traffic);     
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

	/**this.clusters = new ArrayList<Cluster>();
     * Deals with an Event from the event queue.
     * If it is of the FlowArrivalEvent kind, adds it to the list of active flows.
     * If it is from the FlowDepartureEvent, removes it from the list.
     * 
     * @param event the Event object taken from the queue 
     */
    public void newEvent(Event event) 
    {
    	if(event.getTime() > time) 
    	{
    		time = event.getTime();
    	}
    	
    	if (rsa instanceof EarliestDeadlineFirst && (event instanceof FlowArrivalEvent || event instanceof DeadlineEvent))
        {
        	if(event instanceof DeadlineEvent)
        	{		
        		try 
        		{	
            		( (EarliestDeadlineFirst) rsa).deadlineArrival( ((DeadlineEvent)event).getBatch() );	
				} 
        		catch (Exception e)
        		{
        			e.printStackTrace();
				}
        	}
        	else if(event instanceof FlowArrivalEvent )
        	{
        		try 
        		{
        			batches.addFlow( ((FlowArrivalEvent) event).getFlow() );
            		newFlow(((FlowArrivalEvent) event).getFlow());
                	( (EarliestDeadlineFirst) rsa).deadlineArrival( batches.getBatch(((FlowArrivalEvent) event).getFlow().getSource(), ((FlowArrivalEvent) event).getFlow().getDestination()));	
                	
				} 
        		catch (Exception e) 
        		{
					e.printStackTrace();
				}              
        	}
		    
	    } 
    	else 
    	{
    		
	    	if (event instanceof FlowArrivalEvent)
	        {
	    		nConnections++;
	    		
	            newFlow(((FlowArrivalEvent) event).getFlow());
	            rsa.flowArrival(((FlowArrivalEvent) event).getFlow());
	            
	            if(((FlowArrivalEvent) event).getFlow().isAccepeted()) {
	            	
	 	            updateCrosstalk();
	        	}
	            
	        } 
	        else if (event instanceof FlowDepartureEvent) 
	        {
	        	
	        	rsa.flowDeparture(((FlowDepartureEvent) event).getFlow());
	            removeFlow(((FlowDepartureEvent) event).getFlow().getID());
	        	
	        	if(((FlowDepartureEvent) event).getFlow().isAccepeted()) {
	        		st.updateInterCoreCrosstalk(((FlowDepartureEvent) event).getFlow());
	 	            updateCrosstalk();
	        	}
	           
	            this.nExceeds++;
	            //before 100
            	if(this.DFR == true && this.activeFlows.size() >= 100 && this.nExceeds >= 150 && nConnections < 10000) {
            		this.getFragmentationRatio();
//            		System.out.println("before df: "+dfIndex+ " n: "+this.activeFlows.size());
            		if(this.dfIndex >= limited && this.dfIndex <= 0.65) 
            		{
            			
            			DefragmentationArrivalEvent defragmentationEvent = new DefragmentationArrivalEvent(0);
    	            	eventScheduler.addEvent(defragmentationEvent);
    	            	this.nExceeds = 0; 
//    	            	limited = 0.06;
            		}
            	}
            	else if(RR == true && nExceeds >= 150 && this.activeFlows.size() >= 100 && nConnections < 10000) {
                    		
            		fi = this.getFragmentationRatio();
            		
            		if(this.dfIndex >= limited && this.dfIndex <= 0.65) 
            		{
//            			System.out.println("before df: "+dfIndex);
	        			ReroutingArrivalEvent reroutingnEvent = new ReroutingArrivalEvent(0);
	            		eventScheduler.addEvent(reroutingnEvent);
	            		this.nExceeds = 0;
            		}
            	}
	        }
	        else if (event instanceof DefragmentationArrivalEvent) 
	        {
	        	this.defragmentation.setTime(this.time);
	        	this.defragmentation.runDefragmentantion();
	        	this.getFragmentationRatio();
//	        	System.out.println("after df: "+ dfIndex);
	        	updateCrosstalk();
	        	eventScheduler.removeDefragmentationEvent((DefragmentationArrivalEvent)event);
	        }
	        else if(event instanceof ReroutingArrivalEvent) 
	        {
	        	ConnectionSelectionToReroute c = new ConnectionSelectionToReroute((int) Math.ceil(this.activeFlows.size() * 0.30),"ConnectionsInBottleneckLink", this, this.pt, this.vt);
	        	c.setFragmentationIndexForEachLink(fi);
	        	Map<Long, Flow> connections = c.getConnectionsToReroute();
//	        	System.out.println("connections selected: "+connections.size()+ " from n: "+this.activeFlows.size());
//	        	System.out.println("before df: "+dfIndex);
	        	if(typeOfReroutingAlgorithm.equals("ZhangDefragmentationRCSA") == true) {
		        	((ZhangDefragmentationRCSA) rerouting).copyStrutures(this.pt, this.vt);
		        	((ZhangDefragmentationRCSA) rerouting).runDefragmentantion(connections);
	        	}
	        	else if(typeOfReroutingAlgorithm.equals("BalanceDefragmentationRCSA") == true) {
		        	((BalanceDefragmentationRCSA) rerouting).copyStrutures(this.pt, this.vt);
		        	((BalanceDefragmentationRCSA) rerouting).setFragmentationIndexOfEachLink(fi);
		            ((BalanceDefragmentationRCSA) rerouting).runDefragmentantion(connections);
	        	}
	        	
	        	this.getFragmentationRatio();
//	        	System.out.println("after df: "+dfIndex);
	        	eventScheduler.removeReroutingEvent((ReroutingArrivalEvent)event);
	        }
	    }
    }

    private void updateCrosstalk() {
		
    	for(Long key: this.activeFlows.keySet()) {
    		
    		Flow f = this.activeFlows.get(key);
    		
    		if(f.isMultipath())
    		{
    			ArrayList<ArrayList<Slot>> slotList = f.getMultiSlotList();
    			double sumXt = 0;
    			for(int i = 0; i < slotList.size(); i++) {
    				int []links = f.getLinks(i);
	    			for(int l : links) {
	    				sumXt += pt.getLink(l).updateCrosstalk(slotList.get(i),  ModulationsMuticore.subcarriersCapacity[f.getModulationLevel(i)]);
	        		}
    			}
    			f.setXTGenerated(sumXt);
    		}
    		else
    		{
    			double sumXt = 0;
    			for(int l : f.getLinks()) {
        			sumXt += pt.getLink(l).updateCrosstalk(f.getSlotList(),  ModulationsMuticore.subcarriersCapacity[f.getModulationLevel()]);
        		}
    			f.setXTGenerated(sumXt);
    		}
    	}
	}

	/**
     * Fragmentation ratio 
     * @return
     */
	private double []getFragmentationRatio() {
    	
    	int nLinks = pt.getNumLinks();
    	double []fi = new double[nLinks];
    	double nSlots = (pt.getNumSlots() * pt.getCores());
    	dfIndex = 0;
    	
    	for(int i = 0; i < nLinks; i++) {
//    		System.out.println(pt.getLink(i).getSlotsAvailable() + " "+ pt.getLink(i).getNumFreeSlots());
    		fi[i] =  (double)(nSlots - (double)pt.getLink(i).getSlotsAvailable()) / nSlots;
    		dfIndex += fi[i];
    	}
    	
    	dfIndex = (dfIndex / (double)nLinks);
    	
    	return fi;
	}
	

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}
	
	public Map<Long, Flow> getActiveFlows() {
		
		return activeFlows;
	}

	/**
     * Retrieves a Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow object
     * @return the required Flow object
     */
    public Flow getFlow(long id) {
        return activeFlows.get(id);
    }
    
    /**
     * Adds a given active Flow object to a determined Physical Topology.
     * 
     * @param id unique identifier of the Flow object
     * @param lightpath the Path, or list of LighPath objects
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean acceptFlow(long id, LightPath lightpath) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
            	throw (new IllegalArgumentException());
            }
            flow = activeFlows.get(id);
            if (!canAddFlowToPT(flow, lightpath)) {
                return false;
            } 
            
            addFlowToPT(flow, lightpath);
            
            ArrayList<LightPath> lp = new ArrayList<LightPath>();
            lp.add(lightpath);
            mappedFlows.put(flow, lp);
            tr.acceptFlow(flow, lightpath);
            st.acceptFlow(flow, lightpath);
            flow.setAccepeted(true);
            return true;
        }
    }
    
    /**
     * Adds a given active Flow object to a determined Physical Topology.
     * 
     * @param id unique identifier of the Flow object
     * @param lightpath the Path, or list of LighPath objects
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean acceptFlow(long id, ArrayList<LightPath> lightpath) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
            	throw (new IllegalArgumentException());
            }
            
            flow = activeFlows.get(id);
            for(LightPath lp : lightpath) {
            	if (!canAddFlowToPT(flow, lp)) {
                    return false;
                } 
            	
            	 addFlowToPT(flow, lp);
            	 tr.acceptFlow(flow, lp);
            }
            
            st.acceptFlow(flow,lightpath);
            mappedFlows.put(flow, lightpath);
            
            flow.setAccepeted(true);
            return true;
        }
    }
    
    public void reacceptFlow(long id, LightPath lightpath) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
            	throw (new IllegalArgumentException());
            }
            flow = activeFlows.get(id);
            if (!canAddFlowToPT(flow, lightpath)) {
            	System.out.println("Error while reaccepting");
                return;
            } 
            
           this.mappedFlows.get(flow).remove(0);
           this.mappedFlows.get(flow).add(lightpath);
           addFlowToPT(flow, lightpath);
      
            return;
        }
    }

    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id unique identifier of the Flow object
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean blockFlow(long id) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            
          
            flow = activeFlows.get(id);

            if (mappedFlows.containsKey(flow) && !flow.isConnectionDisruption()) {
                return false;
            }
            
            activeFlows.remove(id);
            tr.blockFlow(flow);
            st.blockFlow(flow);
            return true;
        }
    }
    
    
    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id unique identifier of the Flow object
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean blockFlow(long id, boolean disrupted) {
        Flow flow;

        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (mappedFlows.containsKey(flow) && !disrupted) {
                return false;
            }
            else if(mappedFlows.containsKey(flow) && disrupted) {
            	removeFlowFromPT(flow, mappedFlows.get(flow).get(0));
            }
            
            activeFlows.remove(id);
            tr.blockFlow(flow);
            if(!disrupted) st.blockFlow(flow);  
            
            return true;
        }
    }
    
    /**
     * Removes a given Flow object from the Physical Topology and then
     * puts it back, but with a new route (set of LightPath objects). 
     * 
     * @param id unique identifier of the Flow object
     * @param lightpath list of LightPath objects, which form a Path
     * @return true if operation was successful, or false if a problem occurred
     */
    public boolean rerouteFlow(long id, LightPath lightpath) {
        Flow flow;
        LightPath oldPath;
        if (id < 0) {
            throw (new IllegalArgumentException());
        } else {
            if (!activeFlows.containsKey(id)) {
                return false;
            }
            flow = activeFlows.get(id);
            if (!mappedFlows.containsKey(flow)) {
                return false;
            }
            oldPath = mappedFlows.get(flow).get(0);
            removeFlowFromPT(flow, lightpath);
            if (!canAddFlowToPT(flow, lightpath)) {
                addFlowToPT(flow, oldPath);
                return false;
            }
            addFlowToPT(flow, lightpath);
            ArrayList<LightPath> lp = new ArrayList<>();
            lp.add(lightpath);
            mappedFlows.put(flow, lp);
            //tr.flowRequest(id, true);
            return true;
        }
    }
    
    /**
     * Adds a given Flow object to the HashMap of active flows.
     * The HashMap also stores the object's unique identifier (ID). 
     * 
     * @param flow Flow object to be added
     */
    public void newFlow(Flow flow) {
    	
        activeFlows.put(flow.getID(), flow);
    }
    
    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow to be removed
     * 
     * @return the flow object
     */
    public Flow removeFlow(long id) {
        Flow flow;
        LightPath lightpaths;
 
        if (activeFlows.containsKey(id)) {
            flow = activeFlows.get(id);
            if (mappedFlows.containsKey(flow)) {
            	
            	if(flow.isMultipath())
            	{
            		removeFlowFromPT(flow, null);  
            	}
            	else 
            	{
            		lightpaths = mappedFlows.get(flow).get(0);
            		removeFlowFromPT(flow, lightpaths);   
            	}
                
            	mappedFlows.remove(flow);
            }
            
            activeFlows.remove(id);
            return flow;
        }
       
        return null;
    }
    
    /**
     * Removes a given Flow object from a Physical Topology. 
     * 
     * @param flow the Flow object that will be removed from the PT
     * @param lightpaths a list of LighPath objects
     */
    private void removeFlowFromPT(Flow flow, LightPath lightpath) {
    	
        if(flow.isMultipath()) {
        	
        	ArrayList<Long> ids =  flow.getLightpathsID();
        	int n = flow.getMultipath().size();
        	for(int i = 0; i < n; i++) {	
        		
//        		LightPath lp = vt.getLightpath(ids.get(i));
        		int []links = flow.getLinks(i);
                
        		for (int j : links) {
                    pt.getLink(j).releaseSlots(flow.getMultiSlotList().get(i));
                    pt.getLink(j).updateNoise(flow.getMultiSlotList().get(i), flow.getModulationLevel(i));
                }
        		
//        		try {
//        			
//        			throw new Exception("Oh hai "+lp.getID()+" "+flow.getID());
//        		} catch (Exception e) {
//        			System.out.println(e.getMessage());
//        			e.printStackTrace();
//        		}
//        		
                vt.removeLightPath(ids.get(i));
        	}	 
        }
        else
        {
        	int[] links;
            links = lightpath.getLinks();
            
        	for (int j = 0; j < links.length; j++) {
                pt.getLink(links[j]).releaseSlots(lightpath.getSlotList());
                pt.getLink(links[j]).updateNoise(lightpath.getSlotList(), flow.getModulationLevel());
            }
            vt.removeLightPath(lightpath.getID());
        }
        
//       updateCrosstalk();
    }
    
    
    /**
     * Removes a given Flow object from a Physical Topology. 
     * 
     * @param flow the Flow object that will be removed from the PT
     * @param lightpaths a list of LighPath objects
     */
    public void removeFlowFromPT(Flow flow, LightPath lightpath, PhysicalTopology ptTemp, VirtualTopology vtTemp) {

    	int[] links;
        links = flow.getLinks();
        
    	for (int j : links) {
    		ptTemp.getLink(j).releaseSlots(flow.getSlotList());
    		ptTemp.getLink(j).updateNoise(flow.getSlotList(), flow.getModulationLevel());
    		ptTemp.getLink(j).resetCrosstalk(flow.getSlotList());
        }
    	
    	vtTemp.removeLightPath(lightpath.getID());
    }
    
    
    /**
     * Says whether or not a given Flow object can be added to a 
     * determined Physical Topology, based on the amount of bandwidth the
     * flow requires opposed to the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     * @return true if Flow object can be added to the PT, or false if it can't
     */
    private boolean canAddFlowToPT(Flow flow, LightPath lightpath) {
        int[] links;
        // Test the availability of resources
        
        links = lightpath.getLinks();
        for (int j = 0; j < links.length; j++) {
            if (pt.getLink(links[j]).areSlotsAvailable(lightpath.getSlotList(), flow.getModulationLevel())) {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Adds a Flow object to a Physical Topology.
     * This means adding the flow to the network's traffic,
     * which simply decreases the available bandwidth.
     * 
     * @param flow the Flow object to be added 
     * @param lightpaths list of LightPath objects the flow uses
     */
    private void addFlowToPT(Flow flow, LightPath lightpath) {
        int[] links = lightpath.getLinks();
        // Implements it
        for (int j = 0; j < links.length; j++) {
            pt.getLink(links[j]).reserveSlots(lightpath.getSlotList());
            pt.getLink(links[j]).updateNoise(lightpath.getSlotList(), flow.getModulationLevel());
        }
        
    }
    
    /**
     * Retrieves a Path object, based on a given Flow object.
     * That's possible thanks to the HashMap mappedFlows, which
     * maps a Flow to a Path.
     * 
     * @param flow Flow object that will be used to find the Path object
     * @return Path object mapped to the given flow 
     */
    public LightPath getPath(Flow flow) {
        return mappedFlows.get(flow).get(0);
    }
   
    
    public boolean canGroom(Flow flow){
    	
    	for (Entry<Flow, ArrayList<LightPath>> entry : mappedFlows.entrySet())
    	{
    		ArrayList<LightPath> lp = entry.getValue();
    		if (flow.getSource()==lp.get(0).getSource() && flow.getDestination() == lp.get(0).getDestination())
    		{
    			int demandInSlots = (int) Math.ceil(flow.getRate() / (double) Modulations.getBandwidth(lp.get(0).getModulationLevel()));
    			int slotCount = lp.get(0).getSlotList().get(lp.get(0).getSlotList().size()-1).s;
    			ArrayList<Slot> slotList = new ArrayList<Slot>();
    			
    			for (int i = slotCount; i < slotCount+demandInSlots; i++) {
    				Slot p = new Slot(0,i);
    				slotList.add(p);
				}
    			
    			boolean contiguity = true;
    			for (int j = 0; j < lp.get(0).getLinks().length; j++) {
    				if (pt.getLink(lp.get(0).getLink(j)).areSlotsAvailable(slotList, flow.getModulationLevel())){
    					contiguity = false;
    					break;
    				}
				}
    			
    			if (contiguity == true){
    				for (int linkID : lp.get(0).getLinks()) {
    		            pt.getLink(linkID).reserveSlots(lp.get(0).getSlotList());
    		        }
    				flow.setSlotList(slotList);
    				st.groomedFlow(flow);
    				return true;
    			}
    		}
    	    
    	}
    	
    	return false;
    }
    
    /**
     * Groom flow.
     * @param flow 
     *
     * @param lp the lp
     * @param demandInSlots 
     */
    public void groomFlow(Flow flow, LightPath lp){
		for (int linkID : lp.getLinks()) {
            pt.getLink(linkID).reserveSlots(lp.getSlotList());
        }
		st.groomedFlow(flow);
    }
	  /**
     * Update deadline event.
     *
     * @param batch the batch
     */
    public void updateDeadlineEvent(BatchConnectionRequest batch) {
    	
    	eventScheduler.updateDeadlineEvent( batch.getOldestDeadline(), batch.getNewDeadline(time) );
    }
    
    /**
     * Remove the deadline event.
     *
     * @param batch the batch
     */
    public void removeDeadlineEvent(BatchConnectionRequest batch) {
    	
    	if(batch.isEmpty()) return;
    	
    	try 
    	{	
    		eventScheduler.removeDeadlineEvent(batch.getEarliestDeadline());
    		batch.clear();
		} 
    	catch (Exception e) 
    	{
    		e.printStackTrace();
		}
    	
    }
    
	/**
	 * New deadline to schedule
	 * @param time
	 */
	public void addScheduleDeadline(double time, BatchConnectionRequest batch) {
		
		DeadlineEvent deadlineEvent = new DeadlineEvent(time, batch);
		
		try 
		{
			eventScheduler.addEvent(deadlineEvent);
		} 
		catch (Exception e) 
		{
			System.out.println(e);
		}
	}

	public void removeBatch(BatchConnectionRequest batch) {
		this.batches.remove(batch);
	}
	
	public BatchConnectionRequest getEarliestDeadline() {
		
		return batches.getEarliestDeadline();
	}

	public String getRsaAlgorithm() {
		return rsaAlgorithm;
	}

	public void setRsaAlgorithm(String rsaAlgorithm) {
		this.rsaAlgorithm = rsaAlgorithm;
	}

	/**
	 * @return the costMKP
	 */
	public boolean isCostMKP() {
		return costMKP;
	}

	/**
	 * @param costMKP the costMKP to set
	 */
	public void setCostMKP(boolean costMKP) {
		this.costMKP = costMKP;
	}

	@Override
	public Map<Flow, LightPath> getMappedFlows() {
		Map<Flow, LightPath> map = new HashMap<Flow, LightPath>();
		
		for(Flow flow: mappedFlows.keySet()) {
			
			map.put(flow, mappedFlows.get(flow).get(0));
		}
		return map;
	}

	/**
	 * 
	 */
	public ArrayList<Cluster> getClusters() {
		return clusters;
	}

	/**
	 * 
	 * @param clusters
	 */
	public void setClusters(ArrayList<Cluster> c) {
		
		if(c.isEmpty()) this.clusters.clear();
		this.clusters = c;
	}
	
	public void updateControlPlane(PhysicalTopology newPT, VirtualTopology newVT, Flow flow) {

			if(!flow.isConnectionDisruption())
			{
				ArrayList<LightPath> t = new ArrayList<LightPath>();
				t.add(newVT.getLightpath(flow.getLightpathID()));
				
				activeFlows.replace(flow.getID(), flow);
				
				mappedFlows.remove(flow);
				mappedFlows.put(flow, t);
			}
			else
			{
				activeFlows.remove(flow.getID());
				mappedFlows.remove(flow);
			}
		
	}
	

	public void updateControlPlane(PhysicalTopology newPT, VirtualTopology newVT, Map<Long, Flow> flows) {
		
		for(Long key: flows.keySet()) {
			
			if(activeFlows.get(key).getID() == flows.get(key).getID()) {
//				System.out.println(key + ", " + activeFlows.get(key).getID() + ", " +  activeFlows.get(key).getLightpathID() + ", " + flows.get(key).getLightpathID());
				
				if(!flows.get(key).isConnectionDisruption())
				{
					ArrayList<LightPath> t = new ArrayList<LightPath>();
					t.add(newVT.getLightpath(flows.get(key).getLightpathID()));
					
					activeFlows.replace(flows.get(key).getID(), flows.get(key));
					
					mappedFlows.remove(flows.get(key));
					mappedFlows.put(flows.get(key), t);
				}
				else
				{
//					activeFlows.remove(key);
//					mappedFlows.remove(flows.get(key));
					blockFlow(flows.get(key).getID());
				}
			}
			
		}
		
		this.pt.updateEverything(newPT);
		this.vt.updateEverything(newVT);
		
		for(Long key: activeFlows.keySet()) {
			int []links = activeFlows.get(key).getLinks();
			for(int i: links) {
				this.pt.getLink(i).updateCrosstalk(activeFlows.get(key).getSlotList());
			}	
		}
	}
	
	public boolean isRerouting() {
		
		return this.RR;
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
		
		if(!pt.canAcceptCrosstalk(links, slotList, db)) {
			return false;
		}
		
		for(Long key: this.activeFlows.keySet()) {
			
			if(key == flow.getID()) {
				continue;
			}
			else if(this.activeFlows.get(key).isMultipath()) 
			{
				int i = 0;
				for(ArrayList<Slot> s: this.activeFlows.get(key).getMultiSlotList()) {
					
					ArrayList<Integer> matching = getMatchingLinks(links, this.activeFlows.get(key).getLinks(i));
					
					if(!matching.isEmpty()) {
						ArrayList<Slot> t = getMatchingSlots(slotList, s, pt.getLink(0).getAdjacentCores(slotList.get(0).c));
						
						if(!t.isEmpty()) 
						{
							if(!pt.canAcceptInterCrosstalk(this.activeFlows.get(key),  matching, this.activeFlows.get(key).getLinks(i), s, t)) {
								return false;
							}
						}
					}
					i++;
				}
			}
			else 
			{
//				System.out.println(Arrays.toString(slotList.toArray()) + "\n" +this.activeFlows.get(key).getSlotList());
				ArrayList<Integer> matching =  getMatchingLinks(links, this.activeFlows.get(key).getLinks());
				
				if(!matching.isEmpty()) {
					int c = slotList.get(0).c;
					LinkedList<Integer> adj = pt.getLink(0).getAdjacentCores(c);
					ArrayList<Slot> t = getMatchingSlots(slotList, this.activeFlows.get(key).getSlotList(), adj);
					
					if(!t.isEmpty()) 
					{
//						System.out.println(Arrays.toString(matching.toArray()));
						if(!pt.canAcceptInterCrosstalk(this.activeFlows.get(key), matching, this.activeFlows.get(key).getSlotList(), t)) {
							return false;
						}
					}
				}
				
			}
		}
		
		return true;
	}
}