/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import java.util.ArrayList;
//import java.util.Arrays;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.w3c.dom.*;

import flexgridsim.util.WeightedGraph;

/**
 * The physical topology of a network refers to he physical layout of devices
 * on a network, or to the way that the devices on a network are arranged and
 * how they communicate with each other.
 * 
 * @author andred, pedrom, trindade
 */
public class PhysicalTopology {

    private int nodes;
    private int links;
    private int cores;
    private int slots;
    private double slotBw;
    private OXC[] nodeVector;
    private FlexGridLink[] linkVector;
    private FlexGridLink[][] adjMatrix;
    
    private SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph;
    
    /**
     * Creates a new PhysicalTopology object.
     * Takes the XML file containing all the information about the simulation
     * environment and uses it to populate the PhysicalTopology object.
     * The physical topology is basically composed of nodes connected by
     * links, each supporting different wavelengths.
     * 
     * @param xml file that contains the simulation environment information
     */
    public PhysicalTopology(Element xml) {
        int id, src, dst;
        
        double delay, weight;
        int distance;

        try {
            if (Simulator.verbose) {
                System.out.println(xml.getAttribute("name"));
            }
            cores = Integer.parseInt(xml.getAttribute("cores"));
            slots = Integer.parseInt(xml.getAttribute("slots"));
            slotBw = Double.parseDouble(xml.getAttribute("slotsBandwidth")); //legacy, but usable if no modulation is used

            // Process nodes
            NodeList nodelist = xml.getElementsByTagName("node");
            nodes = nodelist.getLength();
            if (Simulator.verbose) {
                System.out.println(Integer.toString(nodes) + " nodes");
            }
            nodeVector = new OXC[nodes];
            //<node id="0" grooming-in-ports="16" grooming-out-ports="16" wlconverters="4" wlconversion-range="2"/>
            for (int i = 0; i < nodes; i++) {
                id = Integer.parseInt(((Element) nodelist.item(i)).getAttribute("id"));
            }

            // Process links
            NodeList linklist = xml.getElementsByTagName("link");
            links = linklist.getLength();
            if (Simulator.verbose) {
                System.out.println(Integer.toString(links) + " links");
            }
            
            linkVector = new FlexGridLink[links];
            adjMatrix = new FlexGridLink[nodes][nodes];
          
            for (int i = 0; i < links; i++) {
                id = Integer.parseInt(((Element) linklist.item(i)).getAttribute("id"));
                src = Integer.parseInt(((Element) linklist.item(i)).getAttribute("source"));
                dst = Integer.parseInt(((Element) linklist.item(i)).getAttribute("destination"));
                delay = Double.parseDouble(((Element) linklist.item(i)).getAttribute("delay"));
                weight = Double.parseDouble(((Element) linklist.item(i)).getAttribute("weight"));
                distance = Integer.parseInt(((Element) linklist.item(i)).getAttribute("distance"));
                linkVector[id] = adjMatrix[src][dst] = new FlexGridLink(id, src, dst, cores, delay, slots, weight, distance);
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public PhysicalTopology(PhysicalTopology p) {
    	
    	this.nodes = p.nodes;
        this.links = p.links;
        this.cores = p.cores;
        this.slots = p.slots;
        this.slotBw = p.slotBw;
        
        this.nodeVector = p.nodeVector.clone();
        this.linkVector = p.linkVector.clone();
        
        this.graph = p.graph;
        
        this.adjMatrix = new FlexGridLink[this.nodes][this.nodes];
        for(int i = 0; i < p.adjMatrix.length; i++) {
        	this.adjMatrix[i] = p.adjMatrix[i].clone();
        }
    }
    
    /**
     * Retrieves the number of nodes in a given PhysicalTopology.
     * 
     * @return the value of the PhysicalTopology's nodes attribute
     */
    public int getNumNodes() {
        return nodes;
    }
    
    /**
     * @return the numero of fiber cores used in the links
     */
    public int getCores() {
  		return cores;
  	}
    
    /**
     * Retrieves the capacity of each slot in a given PhysicalTopology.
     * 
     * @return the value of the PhysicalTopology's slotBw attribute
     */
    public double getSlotCapacity() {
		return slotBw;
	}


	/**
     * Retrieves the number of links in a given PhysicalTopology.
     * 
     * @return number of items in the PhysicalTopology's linkVector attribute
     */
    public int getNumLinks() {
        return linkVector.length;
    }
    
    /**
     * Retrieves the number of wavelengths in a given PhysicalTopology.
     * 
     * @return the value of the PhysicalTopology's wavelengths attribute
     */
    public int getNumSlots() {
        return slots;
    }
    
    /**
     * Retrieves a specific node in the PhysicalTopology object.
     * 
     * @param id the node's unique identifier
     * @return specified node from the PhysicalTopology's nodeVector
     */
    public OXC getNode(int id) {
        return nodeVector[id];
    }
    
    /**
     * Retrieves a specific link in the PhysicalTopology object,
     * based on its unique identifier.
     * 
     * @param linkid the link's unique identifier
     * @return specified link from the PhysicalTopology's linkVector
     */
    public FlexGridLink getLink(int linkid) {
        return linkVector[linkid];
    }
    
    /**
     * Retrieves a specific link in the PhysicalTopology object,
     * based on its source and destination nodes.
     * 
     * @param src the link's source node
     * @param dst the link's destination node
     * @return the specified link from the PhysicalTopology's adjMatrix
     */
    public FlexGridLink getLink(int src, int dst) {
    	
        return adjMatrix[src][dst];
    }
    
    /**
     * Retrives a given PhysicalTopology's adjancency matrix, which
     * contains the links between source and destination nodes.
     * 
     * @return the PhysicalTopology's adjMatrix
     */
    public FlexGridLink[][] getAdjMatrix() {
        return adjMatrix;
    }
    
    /**
     * Says whether exists or not a link between two given nodes.
     * 
     * @param node1 possible link's source node
     * @param node2 possible link's destination node
     * @return true if the link exists in the PhysicalTopology's adjMatrix
     */
    public boolean hasLink(int node1, int node2) {
        if (adjMatrix[node1][node2] != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a weighted graph with vertices, edges and weights representing
     * the physical network nodes, links and weights implemented by this class
     * object.
     * 
     * @return      an WeightedGraph class object 
     */
    public WeightedGraph getWeightedGraph() {
       
    	WeightedGraph g = new WeightedGraph(nodes);
        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                if (hasLink(i, j)) {
                    g.addEdge(i, j, getLink(i, j).getWeight());
                }
            }
        }
        
        return g;
    }
    
    public void setGraph() {
    	
    	graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    			
    	for(int i = 0; i < nodes; i++) graph.addVertex(i);
    	
    	for (int i = 0; i < nodes-1; i++) {
            
    		for (int j = i+1; j < nodes; j++) {
               
            	if (hasLink(i, j)) 
            	{
//            		System.out.println(" src: "+i+" dst: "+j);
                	DefaultWeightedEdge edge = graph.addEdge(i, j);
                	graph.setEdgeWeight(edge, getLink(i, j).getWeight());
                }
            }
        }
    }
    
    
    public Graph<Integer, DefaultWeightedEdge> getGraph() {
    	return graph;
    }
    

    /**
     * Gets the node degree.
     *
     * @param nodeID the node id
     * @return the node degree
     */
    public int getNodeDegree(int nodeID){
    	int degree = 0;
    	for (int i = 0; i < adjMatrix[nodeID].length; i++) {
			if (adjMatrix[nodeID][i] != null){
				degree++;
			}
		}
    	return degree;
    }
    
    
    /**
     * Can groom.
     * @param flow 
     * @param slotList 
     *
     * @return true, if successful
     */
    public boolean canGroom(Flow flow, ArrayList<Slot> slotList){
		if (this.getLink(flow.getSource(), flow.getDestination()).areSlotsAvailable(slotList, flow.getModulationLevel())){
			return true;
		} else {
			return false;
		}
    }
    
    /**
     * Prints all nodes and links between them in the PhysicalTopology object.
     * 
     * @return string containing the PhysicalTopology's adjMatrix values
     */
    @Override
    public String toString() {
        String topo = "";
        for (int i = 0; i < nodes; i++) {
            for (int j = 0; j < nodes; j++) {
                if (adjMatrix[i][j] != null) {
                    topo += adjMatrix[i][j].toString() + "\n\n";
                }
            }
        }
        return topo;
    }
    
    
    public boolean canAcceptCrosstalk(int[] links, ArrayList<Slot> slotList, double db) {
    	
    	double xt = 0.0f;
		for(int i : links) {
			for(Slot s: slotList) {
				int controller = this.getLink(i).getInterCoreCrosstalkInAdjacent(s);
				xt += this.getLink(i).getNewXT(s, controller);
			}
		}
		
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		
//		System.out.println(xt + " "+ db + " cond: " + (xt >= db) + " ::: " + Arrays.toString(slotList.toArray()));
		return xt >= db;
    }
    
    public double sumOfInterCoreCrosstalk(int[] links, ArrayList<Slot> slotList, double db) {
    	
    	double xt = 0.0f;
		for(int i : links) {
			for(Slot s: slotList) {
				int controller = this.getLink(i).getInterCoreCrosstalkInAdjacent(s);
				xt += this.getLink(i).getNewXT(s, controller);
			}
		}
		
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		
		return xt;
    }
    
    public boolean canAcceptInterCrosstalk(Flow flow, int []links, ArrayList<Slot> s1, ArrayList<Slot> s2) {

		double xt = 0.0f;
		double db =  ModulationsMuticore.inBandXT[flow.getModulationLevel()];
		for(int i : links) {
			for(Slot s: s1) {
				int controller = 0;
				if(contains(s2, s)) controller++;
				xt += this.getLink(i).getNewXT(s, controller);
			}
		}
		
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		
		return xt >= db;
	}

    public boolean canAcceptInterCrosstalk(Flow flow, ArrayList<Integer> links, ArrayList<Slot> s1, ArrayList<Slot> s2) {

		double xt = 0.0f;
		double db =  ModulationsMuticore.inBandXT[flow.getModulationLevel()];
		for(int i : flow.getLinks()) {
			for(Slot s: s1) {
				int controller = 0;
				if(contains(s2, s) && links.contains(i)) {
					controller++;
				}
				xt += this.getLink(i).getNewXT(s, controller);
			}
		}
//		System.out.println(xt);
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		
		return xt >= db;
	}
    
    public boolean canAcceptInterCrosstalk(Flow flow, ArrayList<Integer> links, int []l, ArrayList<Slot> s1, ArrayList<Slot> s2) {

		double xt = 0.0f;
		double db =  ModulationsMuticore.inBandXT[flow.getModulationLevel()];
		for(int i : l) {
			for(Slot s: s1) {
				int controller = 0;
				if(contains(s2, s) && links.contains(i)) {
					controller++;
				}
				xt += this.getLink(i).getNewXT(s, controller);
			}
		}
		
		xt = xt > 0 ? ( 10.0f * Math.log10(xt)/Math.log10(10) ) : 0.0f;//db
		
		return xt >= db;
	}
	
	private boolean contains(ArrayList<Slot> slotList, Slot slot) {
		for(Slot s: slotList) {
			
			if(s.c == slot.c && s.s == slot.s) return true;
		}
		
		return false;
	}

	public void resetAllSpectrum() {

		for(int i = 0; i < linkVector.length; i++) {
			this.getLink(i).resetSpectrum();
			this.getLink(i).resetNoises();
			this.getLink(i).resetCrosstalk();
		}
	}

	
	
	public void updateEverything(PhysicalTopology p) {
		this.nodes = p.nodes;
        this.links = p.links;
        this.cores = p.cores;
        this.slots = p.slots;
        this.slotBw = p.slotBw;
        
        this.nodeVector = p.nodeVector.clone();
        this.linkVector = p.linkVector.clone();
        
        this.adjMatrix = new FlexGridLink[this.nodes][this.nodes];
        for(int i = 0; i < p.adjMatrix.length; i++) {
        	this.adjMatrix[i] = p.adjMatrix[i].clone();
        	
//        	for (int j = 0; j < p.adjMatrix[i].length; j++) {
//        		System.out.print(i + " " + j + ": " + this.adjMatrix[i][j] + " || ");
//        	}
//        	System.out.println();
        }
	}

	public int getNumberOfAvailableSlots(int []indexes) {
		
		int n = this.cores * this.slots;
		
		for (int index : indexes) {
			n = this.getLink(index).getSlotsAvailable() < n ? this.getLink(index).getSlotsAvailable() : n;
		}
		
		return n;
	}
}
