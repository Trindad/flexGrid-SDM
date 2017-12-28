/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import flexgridsim.rsa.EarliestDeadlineFirst;
import flexgridsim.rsa.ClusterDefragmentationRCSA;
import flexgridsim.rsa.ControlPlaneForRSA;
import flexgridsim.rsa.DefragmentationRCSA;
import flexgridsim.rsa.RSA;
import flexgridsim.rsa.TridimensionalClusterDefragmentationRCSA;

/**
 * The Control Plane is responsible for managing resources and
 * connection within the network.
 */
public class ControlPlane implements ControlPlaneForRSA {

    private RSA rsa;
    private DefragmentationRCSA defragmentation;
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
    SetOfBatches batches;
    
    /**
     * Defragmentation approaches
     */
//    private int nExceeds = 0;//Zhang Defragmentation Method
//    private int nArrival = 0;
    private static double TH = 0.85;
    private boolean DFR = false;
    private ArrayList<Cluster> clusters;
	private int nExceeds = 0;
	private int limited = 50;
	
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
    public ControlPlane(Element xml, EventScheduler eventScheduler, String rsaModule, String rsaAlgorithm, String defragmentation, String costMultipleKnapsackPloblem, PhysicalTopology pt, VirtualTopology vt, TrafficGenerator traffic) {
        @SuppressWarnings("rawtypes")
		Class RSAClass;
        mappedFlows = new HashMap<Flow, ArrayList<LightPath>>();
        activeFlows = new HashMap<Long, Flow>();
        this.eventScheduler = eventScheduler;
        
        batches = new SetOfBatches();
        
        this.pt = pt;
        this.vt = vt;
        
        this.setRsaAlgorithm(rsaAlgorithm);
        if(costMultipleKnapsackPloblem.equals("true") == true) this.setCostMKP(true);
      
        if(defragmentation.equals("true") == true) {
        	
        	this.setDefragmentation(true);
        	this.defragmentation = new ClusterDefragmentationRCSA();
        	this.defragmentation.simulationInterface(xml, pt, vt, this, traffic);
        }
  
        try {
            RSAClass = Class.forName(rsaModule);
            rsa = (RSA) RSAClass.newInstance();
            rsa.simulationInterface(xml, pt, vt, this, traffic);     
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    private void setDefragmentation(boolean b) {
		this.DFR = b;
	}

	/**
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
	            newFlow(((FlowArrivalEvent) event).getFlow());
	            rsa.flowArrival(((FlowArrivalEvent) event).getFlow());
	            
//	            nArrival++;
	        } 
	        else if (event instanceof FlowDepartureEvent) 
	        {
//	        	System.out.println("Flow departure"+((FlowDepartureEvent) event).time);
	        	removeFlow(((FlowDepartureEvent) event).getFlow().getID());
	            rsa.flowDeparture(((FlowDepartureEvent) event).getFlow());
	            
	            this.nExceeds++;          	
	            
            	if(this.activeFlows.size() >= 200 && this.DFR == true && nExceeds >= 150) {
            		
            		if(this.getFragmentationRatio() >= TH) {
	            		System.out.println("Defragmentation approach: "+this.getFragmentationRatio());
	            		DefragmentationArrivalEvent defragmentationEvent = new DefragmentationArrivalEvent(0);
	            		eventScheduler.addEvent(defragmentationEvent);
	            		this.nExceeds = 0;
	            		this.limited = (int) (this.limited + Math.log(this.limited) * 10);
            		}
            	}
	        }
	        else if (event instanceof DefragmentationArrivalEvent) 
	        {
	        	System.out.println("Start defragmentation");

	        	this.defragmentation.runDefragmentantion();
	        	
//	        	nExceeds = 0;
	        	eventScheduler.removeDefragmentationEvent((DefragmentationArrivalEvent)event);
	        	System.out.println("Defragmentation rate: "+this.getFragmentationRatio());
	        }
	    }
    }

    /**
     * Fragmentation ratio 
     * @return
     */
	private double getFragmentationRatio() {
    	
    	int E = pt.getNumLinks();
    	
    	double FRi = 0.0f;
    	
    	for(int i = 0; i < E; i++) {
    		double nSlots = (pt.getLink(i).getSlots()*pt.getLink(i).getCores());
    		FRi += (nSlots - pt.getLink(i).getNumFreeSlots()) / nSlots;
    	}
    	
    	return (FRi/E);
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
            for(int i = 0; i < lightpath.size(); i++) {
            	if (!canAddFlowToPT(flow, lightpath.get(i))) {
                    return false;
                } 
            	 addFlowToPT(flow, lightpath.get(i));
            	 tr.acceptFlow(flow, lightpath.get(i));
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
                return;
            } 
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
            if (mappedFlows.containsKey(flow)) {
                return false;
            }
            activeFlows.remove(id);
            tr.blockFlow(flow);
            st.blockFlow(flow);
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
                lightpaths = mappedFlows.get(flow).get(0);
                removeFlowFromPT(flow, lightpaths);
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
        	
        	int[] links;
        	for(int i = 0; i < flow.getMultipath().size(); i++) {	
        		
        		LightPath lp = mappedFlows.get(flow).get(i);
        		links = lp.getLinks();
                
        		for (int j = 0; j < links.length; j++) {
                    pt.getLink(links[j]).releaseSlots(lightpath.getSlotList());
                    pt.getLink(links[j]).updateNoise(lightpath.getSlotList(), flow.getModulationLevel(i));
                }
        		
                vt.removeLightPath(lightpath.getID());
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
    
    /**
     * Retrieves the complete set of Flow/Path pairs listed on the
     * mappedFlows HashMap.
     * 
     * @return the mappedFlows HashMap
     */
//    public Map<Flow, ArrayList<LightPath> > getMappedFlows() {
//        return mappedFlows;
//    }
    
    public boolean canGroom(Flow flow){
    	for (Entry<Flow, ArrayList<LightPath>> entry : mappedFlows.entrySet()){
    		ArrayList<LightPath> lp = entry.getValue();
    		if (flow.getSource()==lp.get(0).getSource() && flow.getDestination() == lp.get(0).getDestination()){
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
//    		if(batch.getSource() == batches.getEarliestDeadline().getSource() && batch.getDestination() == 
//    				batches.getEarliestDeadline().getDestination())
//    		{
//    			batches.resetEarliestDeadline();
//    		}
    		
    		eventScheduler.removeDeadlineEvent(batch.getEarliestDeadline());
//    		batches.remove(batch);
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
	public void setClusters(ArrayList<Cluster> clusters) {
		this.clusters = clusters;
	}

}