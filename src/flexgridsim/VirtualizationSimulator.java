package flexgridsim;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Centralizes the simulation execution specific for VONs. Defines what the command line
 * arguments do, and extracts the simulation information from the XML file.
 * 
 * @author trindade
 *
 */

public class VirtualizationSimulator extends Simulator {

	private static final String simName = new String("flexgridsim");
    private static final Float simVersion = new Float(3);
    
    /** Verbose flag. */
    public static boolean verbose = false;
    
    /** Trace flag. */
    public static boolean trace = false;
	
	public void Execute(String simConfigFile, boolean verbose, int numberOfSimulations) {
		
		Simulator.verbose = verbose;
		
		if (Simulator.verbose) 
		{
	      //TODO
		}
		try 
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse(new File(simConfigFile));
	
	        // normalize text representation
	        doc.getDocumentElement().normalize();
	        
	        if (!doc.getDocumentElement().getNodeName().equals(simName)) {
	            System.out.println("Root element of the simulation file is " + doc.getDocumentElement().getNodeName() + ", " + simName + " is expected!");
	            System.exit(0);
	        }
	        if (!doc.getDocumentElement().hasAttribute("version")) {
	            System.out.println("Cannot find version attribute!");
	            System.exit(0);
	        }
	        if (Float.compare(new Float(doc.getDocumentElement().getAttribute("version")), simVersion) > 0) {
	            System.out.println("Simulation config file requires a newer version of the simulator!");
	            System.exit(0);
	        }
	        
	        String rsaModule = "flexgridsim.rsa." + ((Element) doc.getElementsByTagName("rsa").item(0)).getAttribute("module");
	        String mapperModule = ((Element) doc.getElementsByTagName("mapper").item(0)).getAttribute("module");
	        
	        if (Simulator.verbose) 
	        {
                System.out.println("RSA module: " + rsaModule);
                System.out.println("VON module: " + mapperModule);
            }
	        
	        OutputManager gp = new OutputManager((Element) doc.getElementsByTagName("graphs").item(0));
            PhysicalTopology pt = new PhysicalTopology((Element) doc.getElementsByTagName("physical-topology").item(0));
	        
	        for (int i = 1; i <= numberOfSimulations; i++) {
	        	
	        	EventScheduler events = new EventScheduler();
	        	
	        	TrafficGenerator traffic = TrafficGenerator.generate((Element) doc.getElementsByTagName("vontraffic").item(0), -1);
	            ((VonTrafficGenerator)traffic).generateTraffic(pt, events, i);
	            
	            MyStatistics st = MyStatistics.getMyStatisticsObject();
	            st.statisticsSetup(gp, pt, traffic, pt.getNumNodes(), 3, 0, 0);

	        	VonControlPlane cp = new VonControlPlane(((Element) doc.getElementsByTagName("rsa").item(0)), events, rsaModule, mapperModule, pt, traffic);
	 	        
	 	        new SimulationRunner(cp, events);
	 	        
	 	        if(Simulator.verbose) 
	 	        {
	 	        	//TODO
	 	        }
	 	        else {
	 	        	
	 	        	st.calculateLastStatistics();
	 	        }
	 	        
	 	       st.finish();
	        }
	        
	    	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
