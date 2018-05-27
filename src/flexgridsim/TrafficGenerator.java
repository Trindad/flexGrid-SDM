/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package flexgridsim;

import org.w3c.dom.*;

/**
 * Generates the network's traffic based on the information passed through the
 * command line arguments and the XML simulation file.
 * 
 * @author andred, trindade
 */
public class TrafficGenerator {
	
	public TrafficGenerator() {
		
	}

    /**
     * Creates a new TrafficGenerator object.
     * Extracts the traffic information from the XML file and takes the chosen load and
     * seed from the command line arguments.
     * 
     * @param xml file that contains all information about the simulation
     * @param forcedLoad range of offered loads for several simulations
     */
    public static TrafficGenerator generate(Element xml, double forcedLoad)  {
    	
    	if(xml.getAttribute("type").equals("ip")) {
    		
    		return new IPTrafficGenerator(xml, forcedLoad);
    		
    	}
    	else
    	{
    		return new TrafficGeneratorDefault(xml, forcedLoad);
    	}
    }

    /**
     * Generates the network's traffic.
     *
     * @param events EventScheduler object that will contain the simulation events
     * @param pt the network's Physical Topology
     * @param seed a number in the interval [1,25] that defines up to 25 different random simulations
     */
    public void generateTraffic(PhysicalTopology pt, EventScheduler events, int seed) {
    	
    }
    
    /**
     * Gets the calls type info.
     *
     * @return the calls type info
     */
    public TrafficInfo[] getCallsTypeInfo() {
		return null;
    	
    }
    
    /**
     * OC in giga bits.
     *
     * @param oc the oc
     * @return the double
     */
    public double ocInGigaBits(int oc) {
		return oc;
    	
    }
    
    /**
     * OC in mega bits.
     *
     * @param oc the oc
     * @return the double
     */
    public int ocInMegaBits(int oc) {
		return oc;
    	
    }

}
