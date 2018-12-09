package flexgridsim;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import flexgridsim.von.VirtualTopology;
import flexgridsim.von.VirtualTopologyGenerator;

import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

public class VonTrafficGenerator extends TrafficGenerator {

	private int calls;//number of vons
	private Element xml;
	private int minNodes;
	private int maxNodes;
	private int connectivityProbability;
	private int minAlternativeNodes;
	private int maxAlternativeNodes;
	private int minComputingResources;
	private int maxComputingResources;
	private int minCapacity;
	private int maxCapacity;
	
	private double meanHoldingTime = 100;
	private double averageRate = 5;//average rate of 5 requests per 100 time units
	private double timeUnits = 100;
	
	public VonTrafficGenerator(Element xml) {
	
		this.xml = xml;
        this.calls = Integer.parseInt(xml.getAttribute("calls"));
        NodeList settings = xml.getElementsByTagName("setting");
        
        for (int i = 0; i < settings.getLength(); i++) {
        	Element setting = (Element) settings.item(i);
//        	System.out.println(setting.getAttribute("name") + " " + setting.getFirstChild().getNodeValue());
        	switch (setting.getAttribute("name")) {
        	case "minNodes":
        	    this.minNodes = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "maxNodes":
        	    this.maxNodes = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "connectivityProbability":
        	    this.connectivityProbability = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "minAlternativeNodes":
        	    this.minAlternativeNodes = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "maxAlternativeNodes":
        	    this.maxAlternativeNodes = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "minComputingResources":
        	    this.minComputingResources = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "maxComputingResources":
        	    this.maxComputingResources = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "minCapacity":
        	    this.minCapacity = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	case "maxCapacity":
        	    this.maxCapacity = Integer.parseInt(setting.getFirstChild().getNodeValue());
        	    break;
        	}
        }
     
	}
	
	 public void generateTraffic(PhysicalTopology pt, EventScheduler events, int seed) {
	    	System.out.println("VON traffic generator");
	    	
	    	ExponentialDistribution exp = new ExponentialDistribution(meanHoldingTime);
	    	double time = 0;
//	    	PoissonDistribution poisson = new PoissonDistribution();
	    		    	
	    	for(int i = 0; i < calls; i++) {
	    		
	    		VirtualTopology newVon = VirtualTopologyGenerator.generate(pt, minNodes, maxNodes, connectivityProbability, minAlternativeNodes, maxAlternativeNodes,
						minComputingResources, maxComputingResources, minCapacity, maxCapacity);
	            newVon.holdingTime = exp.sample();
	            
//	            System.out.println("holdingTime: "+newVon.holdingTime);
//	            time += poisson.sample();
	            
	            newVon.arrivalTime = time;
//		    	System.out.println("arrivalTime: "+newVon.arrivalTime);
	            
		    	Event event = new VonArrivalEvent(time, newVon);
	            events.addEvent(event);
	            event = new VonDepartureEvent(time + newVon.holdingTime, i, newVon);
	            events.addEvent(event);
	            
	            time += nextTime(averageRate/timeUnits);

	    	}
	 }
	 
	public double nextTime(double rateParameter) {
		
		 return (-Math.log(1.0 - Math.random()) / rateParameter);
	}
}
