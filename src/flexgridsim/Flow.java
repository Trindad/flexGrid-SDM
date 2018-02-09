/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.ArrayList;

/**
 * The Flow class defines an object that can be thought of as a flow
 * of data, going from a source node to a destination node. 
 * 
 * @author andred, pedrom, trindade
 */
public class Flow {

    private long id;
    private long lightpathID;
    private int src;
    private int dst;
    private int bw;
    private double duration;
    private int cos;
    private int[] links;
    private int core;
    private ArrayList<Slot> slotList;
    private double deadline;
    private boolean accepeted;
    private boolean groomed;
    private double time;
    private int modulationLevel;
    
    //Batch requests
    boolean isBatchRequest;
    boolean isPostponeRequest;
    private int numberOfFlowsGroomed;
    
    //Multipath requests
    private ArrayList<int[]> multipath; 
    private ArrayList<Integer> modulationLevels;
    private ArrayList<Long> lightpathsID;
    private boolean isMultipath = false;
    private ArrayList<ArrayList<Slot>> multiSlotList;
    
    //Rerouting
    private boolean connectionDisruption = false;

	/**
	 * Creates a new Flow object.
	 *
	 * @param id            unique identifier
	 * @param src           source node
	 * @param dst           destination node
	 * @param time the time
	 * @param bw            bandwidth required (Mbps)
	 * @param duration      duration time (seconds)
	 * @param cos           class of service
	 * @param deadline 		the deadline
	 */
    public Flow(long id, int src, int dst, double time, int bw, double duration, int cos, double deadline) {
        if (id < 0 || src < 0 || dst < 0 || bw < 1 || duration < 0 || cos < 0) {
            throw (new IllegalArgumentException());
        } else {
            this.id = id;
            this.src = src;
            this.dst = dst;
            this.bw = bw;
            this.duration = duration;
            this.cos = cos;
            this.deadline = deadline;
            this.accepeted = false;
            this.time = time;
            this.modulationLevel = 0;
            this.groomed = false;
            this.isBatchRequest = false; //initialize with false a batch request
            this.numberOfFlowsGroomed = 0;
            this.isPostponeRequest = false;
            
            this.multipath = new  ArrayList<int[]>();
            this.setLightpathsID(new ArrayList<Long>());
            this.modulationLevels = new ArrayList<Integer>();
            
        }
    }
    
    /**
     * Gets the time.
     *
     * @return the time
     */
    public double getTime() {
		return time;
	}


	/**
     * Retrieves the unique identifier for a given Flow.
     * 
     * @return the value of the Flow's id attribute
     */
    public long getID() {
        return id;
    }
    
    /**
     * Retrieves the source node for a given Flow.
     * 
     * @return the value of the Flow's src attribute
     */
    public int getSource() {
        return src;
    }
    
    /**
     * Retrieves the destination node for a given Flow.
     * 
     * @return the value of the Flow's dst attribute
     */
    public int getDestination() {
        return dst;
    }
    
    /**
     * Retrieves the required bandwidth for a given Flow.
     * 
     * @return the value of the Flow's bw attribute.
     */
    public int getRate() {
        return bw;
    }
    
    /**
     * Assigns a new value to the required bandwidth of a given Flow.
     * 
     * @param bw new required bandwidth 
     */
    public void setRate(int bw) {
        this.bw = bw;
    }
    
    /**
     * Retrieves the duration time, in seconds, of a given Flow.
     * 
     * @return the value of the Flow's duration attribute
     */
    public double getDuration() {
        return duration;
    }
    
    /**
     * Retrieves a given Flow's "class of service".
     * A "class of service" groups together similar types of traffic
     * (for example, email, streaming video, voice,...) and treats
     * each type with its own level of service priority.
     * 
     * @return the value of the Flow's cos attribute
     */
    public int getCOS() {
        return cos;
    }
    
    
    public String getLinksInStringFormat() {
//    	for(int i = 0; i < links.length; i++) System.out.println(links[i]);
    	
    	return  links.toString();
    }

    
    /**
     * @return the list of channels allocated for all cores
     */
    public ArrayList<Slot> getSlotList() {
		return slotList;
	}
    
	/**
	 * @param slotList list of slots
	 */
	public void setSlotList(ArrayList<Slot> slotList) {
		this.slotList = slotList;
	}

	/**
     * Gets the links used by the flow.
     *
     * @return the links
     */
    public int[] getLinks() {
		return links;
	}
    
    /**
     * Gets the link used by the flow.
     *
     * @param i the i
     * @return the link
     */
    public int getLink(int i) {
		return links[i];
	}

	/**
	 * Sets the links used by the flow.
	 *
	 * @param links the new links
	 */
	public void setLinks(int[] links) {
		this.links = links;
	}
    /**
     * Prints all information related to a given Flow.
     * 
     * @return string containing all the values of the flow's parameters
     */
    public String toString() {
        String flow = Long.toString(id) + ": " + Integer.toString(src) + "->" + Integer.toString(dst) + " rate: " + Integer.toString(bw) + " duration: " + Double.toString(duration) + " cos: " + Integer.toString(cos);
        return flow;
    }
  
	/**
	 * Gets the deadline.
	 *
	 * @return the deadline
	 */
	public double getDeadline() {
		return deadline;
	}

	/**
	 * Sets the deadline.
	 *
	 * @param deadline the new deadline
	 */
	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}
	
	  
    /**
     * Creates a string with relevant information about the flow, to be
     * printed on the Trace file.
     * 
     * @return string with values of the flow's parameters
     */
    
    public String toTrace()
    {
    	String trace = Long.toString(id) + " " + Integer.toString(src) + " " + Integer.toString(dst) + " " + Integer.toString(bw) + " " + Double.toString(duration) + " " + Integer.toString(cos);
    	return trace;
    }

	/**
	 * Checks if is accepted.
	 *
	 * @return true, if is accepted
	 */
	public boolean isAccepeted() {
		return accepeted;
	}

	/**
	 * Sets the accepeted.
	 *
	 * @param accepeted the new accepeted
	 */
	public void setAccepeted(boolean accepeted) {
		this.accepeted = accepeted;
	}

	/**
	 * Gets the modulation level.
	 *
	 * @return the modulation level
	 */
	public int getModulationLevel() {
		return modulationLevel;
	}

	/**
	 * Sets the modulation level.
	 *
	 * @param modulationLevel the new modulation level
	 */
	public void setModulationLevel(int modulationLevel) {
		this.modulationLevel = modulationLevel;
	}

	/**
	 * @return true if is a groomed flow
	 */
	public boolean isGroomed() {
		return groomed;
	}

	/**
	 * @param groomed
	 */
	public void setGroomed(boolean groomed) {
		this.groomed = groomed;
	}
    
	/**
     * Sets the batch.
     *
     * @param isBatch the new batch
     */
    public void setBatchRequest(boolean isBatchRequest) {
		this.isBatchRequest = isBatchRequest;
	}
    
    /**
     * Is this a batch request?
     */
    public boolean isBatchRequest() {
    	return this.isBatchRequest;
    }
    
    public boolean isPostponeRequest() {
    	return this.isPostponeRequest;
    }
    
    public void setPostponeRequest(boolean post) {
    	
    	this.isPostponeRequest = post;
    }

	public void setNumberOfFlowsGroomed(int size) {
		this.numberOfFlowsGroomed = size;
	}

	public int getNumberOfFlowsGroomed() {
		return this.numberOfFlowsGroomed;
	}

	public long getLightpathID() {
		return lightpathID;
	}

	public void setLightpathID(long lightpathID) {
		this.lightpathID = lightpathID;
	}
	
	

	public ArrayList<int[]> getMultipath() {
		return multipath;
	}

	public void setMultipath(ArrayList<int[]> multipath) {
		this.multipath = multipath;
	}

	public void setLinks(ArrayList<int[]> paths) {
		
		this.multipath = paths;
	}

	public void setSlotListMultipath(ArrayList<ArrayList<Slot>> m) {
		this.setMultiSlotList(m);
	}

	public ArrayList<Integer> getModulationLevels() {
		return modulationLevels;
	}
	
	public int getModulationLevel(int index) {
		return modulationLevels.get(index);
	}

	public void setModulationLevels(ArrayList<Integer> modulationLevels) {
		this.modulationLevels = modulationLevels;
	}

	public void addModulationLevel(int modulation) {
		this.modulationLevels.add(modulation);
	}
	
	public void removeModulationLevel() {
		this.modulationLevels.remove(modulationLevels.size()-1);
	}

	public boolean isMultipath() {
		return isMultipath;
	}

	public void setMultipath(boolean isMultipath) {
		this.isMultipath = isMultipath;
	}

	public int[] getLinks(int index) {

		return multipath.get(index);
	}

	public ArrayList<Long> getLightpathsID() {
		return lightpathsID;
	}

	public void setLightpathsID(ArrayList<Long> lightpathsID) {
		this.lightpathsID = lightpathsID;
	}

	public ArrayList<ArrayList<Slot>> getMultiSlotList() {
		return multiSlotList;
	}

	public void setMultiSlotList(ArrayList<ArrayList<Slot>> multiSlotList) {
		this.multiSlotList = multiSlotList;
	}

	public long getLightpathID(int i) {
		
		return this.lightpathsID.get(i);
	}
	
	public int getSlotListSize() {
		return slotList.size();
	}

	public int getCore() {
		return core;
	}

	public void setCore(int core) {
		this.core = core;
	}

	public boolean isConnectionDisruption() {
		return connectionDisruption;
	}

	public void setConnectionDisruption(boolean connectionDisruption) {
		this.connectionDisruption = connectionDisruption;
	}

	public Flow copy() {
		
		Flow f = new Flow(this.id, this.src, this.dst, this.time, this.bw, this.duration, this.cos, this.deadline);
        f.accepeted = this.accepeted;
        f.time = this.time;
        f.modulationLevel = this.modulationLevel;
        
        f.lightpathID = this.lightpathID;
        f.links = this.links.clone();
        
        ArrayList<Slot> slots = new ArrayList<Slot>();
        for(Slot s: slotList) {
        	slots.add(s);
        }
        
        f.setSlotList(slots);
        
		return f;
	}
}
