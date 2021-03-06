/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.Map;

import flexgridsim.*;

/**
 * This is the interface that provides several methods for the
 * RSA Class within the Control Plane.
 * 
 * @author andred, pedrom
 */
public interface ControlPlaneForRSA {

    /**
     * Accept flow.
     *
     * @param id the id
     * @param lightpaths the lightpaths
     * @return true, if successful
     */
    public boolean acceptFlow(long id, LightPath lightpaths);

    /**
     * Block flow.
     *
     * @param id the id
     * @return true, if successful
     */
    public boolean blockFlow(long id);
    
    
    /**
     * Adds a given Flow object to the HashMap of active flows.
     * The HashMap also stores the object's unique identifier (ID). 
     * 
     * @param flow Flow object to be added
     */
    public void newFlow(Flow flow);
    
    /**
     * Removes a given Flow object from the list of active flows.
     * 
     * @param id the unique identifier of the Flow to be removed
     * 
     * @return the flow object
     */
    public Flow removeFlow(long id);

    /**
     * Reroute flow.
     *
     * @param id the id
     * @param lightpaths the lightpaths
     * @return true, if successful
     */
    public boolean rerouteFlow(long id, LightPath lightpaths);
    
    /**
     * Gets the flow.
     *
     * @param id the id
     * @return the flow
     */
    public Flow getFlow(long id);
    
    /**
     * Gets the path.
     *
     * @param flow the flow
     * @return the path
     */
    public LightPath getPath(Flow flow);
    
    /**
     * Gets the mapped flows.
     *
     * @return the mapped flows
     */
    public Map<Flow, LightPath> getMappedFlows();

	/**
	 * @param flow
	 * @param lp
	 * @param bestDemandInSlots
	 */
	public void groomFlow(Flow flow, LightPath lp);
	
	/**
	 * @param flow
	 * @return if the flow can be groomed
	 */
	public boolean canGroom(Flow flow);
	
	/**
	 * Remove a deadline event
	 * @param batch
	 */
	public void removeDeadlineEvent(BatchConnectionRequest batch);
	
	/**
	 * update the earliest and oldest deadline request
	 * 
	 * @param batch
	 */
	public void updateDeadlineEvent(BatchConnectionRequest batch);

	/**
	 * 
	 * @param time
	 * @param batch
	 */
	public void addScheduleDeadline(double time, BatchConnectionRequest batch);

	public void removeBatch(BatchConnectionRequest batch);

	public BatchConnectionRequest getEarliestDeadline();

	public String getRsaAlgorithm();

	public double getTime();

	public void setTime(double f);

	public boolean isCostMKP();

	public Map<Long, Flow> getActiveFlows();

	public void reacceptFlow(long id, LightPath lps);

	public boolean acceptFlow(long id, ArrayList<LightPath> lps);

	public  ArrayList<Cluster> getClusters();

	public void setClusters(ArrayList<Cluster> clustersStructure);

	public boolean blockFlow(long id, boolean b);

	public void updateControlPlane(PhysicalTopology pt, VirtualTopology vt, Map<Long, Flow> flows);

	public void removeFlowFromPT(Flow flow, LightPath lightpath, PhysicalTopology pt, VirtualTopology vt);

	public void updateControlPlane(PhysicalTopology pt, VirtualTopology vt, Flow flow);

	public boolean isRerouting();

	public boolean CrosstalkIsAcceptable(Flow flow, int[] links, ArrayList<Slot> fittedSlotList, double db);

	public double CrosstalkCost(Flow flow, int[] links, ArrayList<Slot> candidate, double d);

}
