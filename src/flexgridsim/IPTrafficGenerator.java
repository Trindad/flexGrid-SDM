package flexgridsim;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import flexgridsim.util.Distribution;

/**
 * Generates the network's traffic based on the information passed through the
 * command line arguments and the XML simulation file.
 * 
 * @author trindade
 */
public class IPTrafficGenerator extends TrafficGenerator{
	
	private double T;//cycle of changes in seconds
	private double a = Math.sqrt(2);//constant value to ensure the non-negative result
	private double changeTrafficPeriod;
	private double suddenTrafficChanges;
	private int rate;
	
	private double hours;
	private double load;
	private int maxRate;
	private TrafficInfo[] callsTypesInfo;
	private double meanRate;
	private double meanHoldingTime;
	private int TotalWeight;
	private int numberCallsTypes;
	private Element xml;
	
	Distribution distributionLogNormal = new Distribution();

	public IPTrafficGenerator(Element xml, double forcedLoad) {
		
		int cos, weight;
        double holdingTime;
        this.xml = xml;
        hours = Integer.parseInt(xml.getAttribute("hours"));
     
        T = hours * 3600;
        load = forcedLoad;
        changeTrafficPeriod = T / 100; 
        suddenTrafficChanges = T/200;
        System.out.println(changeTrafficPeriod);
        if (load == 0) {
            load = Double.parseDouble(xml.getAttribute("load"));
        }
        
        maxRate = Integer.parseInt(xml.getAttribute("max-rate"));

        if (Simulator.verbose) {
            System.out.println(xml.getAttribute("hours") + " hours, " + xml.getAttribute("load") + " erlangs.");
        }

        // Process calls
        NodeList callslist = xml.getElementsByTagName("calls");
        numberCallsTypes = callslist.getLength();
       
        callsTypesInfo = new TrafficInfo[numberCallsTypes];

        TotalWeight = 0;
        meanRate = 0;
        meanHoldingTime = 0;

        for (int i = 0; i < numberCallsTypes; i++) {
            TotalWeight += Integer.parseInt(((Element) callslist.item(i)).getAttribute("weight"));
        }
        
        holdingTime = Double.parseDouble(((Element) callslist.item(0)).getAttribute("holding-time"));
        rate = Integer.parseInt(((Element) callslist.item(0)).getAttribute("rate"));
        cos = Integer.parseInt(((Element) callslist.item(0)).getAttribute("cos"));
        weight = Integer.parseInt(((Element) callslist.item(0)).getAttribute("weight"));
        meanRate += (double) rate * ((double) weight / (double) TotalWeight);
        meanHoldingTime += holdingTime * ((double) weight / (double) TotalWeight);
        callsTypesInfo[0] = new TrafficInfo(holdingTime, rate, cos, weight);
	}
	
	 /**
     * Generates the network's traffic.
     *
     * @param events EventScheduler object that will contain the simulation events
     * @param pt the network's Physical Topology
     * @param seed a number in the interval [1,25] that defines up to 25 different random simulations
     */
    public void generateTraffic(PhysicalTopology pt, EventScheduler events, int seed) {
    
        /* Compute the arrival time
         *
         * load = meanArrivalRate x holdingTime x bw/maxRate
         * 1/meanArrivalRate = (holdingTime x bw/maxRate)/load
         * meanArrivalTime = (holdingTime x bw/maxRate)/load
         */
        double meanArrivalTime = (meanHoldingTime * (meanRate / (double) maxRate)) / load;

        // Generate events
        int type, src, dst;
        double time = 0.0;
        int id = 0;
        int numNodes = pt.getNumNodes();
        Distribution dist1, dist2, dist3, dist4, dist5;
        dist1 = new Distribution(1, seed);
        dist2 = new Distribution(2, seed);
        dist3 = new Distribution(3, seed);
        dist4 = new Distribution(4, seed);
       
        dist5 = new Distribution();
        
        double minSigma = 3;
        double maxSigma = 7;
        double currentSigma = 1;
        double changesCycle = 0;
        while(time < T) {
        	
            type = dist1.nextInt(TotalWeight);
            src = dst = dist2.nextInt(numNodes);
            while (src == dst) {
                dst = dist2.nextInt(numNodes);
            }
            
            if(changesCycle >= changeTrafficPeriod) {
        		
            	double sigma = dist5.nextDoubleInTheInterval(minSigma, maxSigma);
        		changeTrafficPeriod += changesCycle;
        		distributionLogNormal.setSigma(sigma);
            }
            else if(changesCycle >= suddenTrafficChanges) {
            	
            	double sigma = dist5.nextDoubleInTheInterval(currentSigma, currentSigma+2);
            	currentSigma = sigma;
            	suddenTrafficChanges += changesCycle;
        		distributionLogNormal.setSigma(sigma);
            }
            
            double holdingTime;
            Distribution distributionFourier = new Distribution();
            double plus = getTrafficDemand(distributionFourier.nextDoubleInTheInterval(0, 1), distributionFourier.nextDoubleInTheInterval(0, 1), time) * 10000;
    		int rateInMbps = rate + (int)plus;
    
            holdingTime = dist4.nextExponential(callsTypesInfo[type].getHoldingTime());
		    System.out.println(id+" "+src+" "+dst+" "+time+" bw : "+rateInMbps+" "+holdingTime+" "+callsTypesInfo[type].getCOS()+" "+time+(holdingTime*0.5)+" "+plus);
			Flow newFlow = new Flow(id, src, dst, time, rateInMbps, holdingTime, callsTypesInfo[type].getCOS(), time+(holdingTime*0.5));
            
            Event event = null;
        	event = new FlowArrivalEvent(time, newFlow);
            time += dist3.nextExponential(meanArrivalTime);
            events.addEvent(event);
            event = new FlowDepartureEvent(time + holdingTime, id, newFlow);
            events.addEvent(event);
            changesCycle += time;
            
            id++;
        }
        
        System.out.println(id+ " "+time+ " "+T);
    }
    
	
	public double getTrafficDemand(double b, double c, double t) {
		
		double beta = distributionLogNormal.getLogNormal();
		
		return ( beta * (a + b * Math.cos( (2 * Math.PI * t) / T) + c * Math.sin((2 * Math.PI * t) / T) ) );
	}
}
