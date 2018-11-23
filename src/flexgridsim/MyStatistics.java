package flexgridsim;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * The Class MyStatistics.
 */
public class MyStatistics {
	private static MyStatistics singletonObject;
	private OutputManager plotter;
	private PhysicalTopology pt;
	private TrafficGenerator traffic;
    private int minNumberArrivals;
    private int numberArrivals;
    private int arrivals;
    private int departures;
    private int accepted;
    private int blocked;
    private int requiredBandwidth;
    private int blockedBandwidth;
    private int numNodes;
    private int[][] arrivalsPairs;
    private int[][] blockedPairs;
    private int[][] requiredBandwidthPairs;
    private int[][] blockedBandwidthPairs;
    private double load;
    private double totalPowerConsumed;
    private double simTime;
    private double dataTransmitted;
    // Diff
    private int[] arrivalsDiff;
    private int[] blockedDiff;
    private int[] requiredBandwidthDiff;
    private int[] blockedBandwidthDiff;
    private int[][][] arrivalsPairsDiff;
    private int[][][] blockedPairsDiff;
    private int[][][] requiredBandwidthPairsDiff;
    private int[][][] blockedBandwidthPairsDiff;
    private int[][] numberOfUsedTransponders;
    
    //sum
    private double[]modulationFormat;//sum of each kind of modulation format used 
    private double[]coreUsed;//core-index used by each connection
    private double sumXT = 0;//sum of inter-core crosstalk occured
    private double sumXTLinks = 0;
    private double pathLength = 0;
    private int nMultipaths = 0;
    
    /**
     * A private constructor that prevents any other class from instantiating.
     */
    private MyStatistics() {
    	
        numberArrivals = 0;

        arrivals = 0;
        departures = 0;
        accepted = 0;
        blocked = 0;

        requiredBandwidth = 0;
        blockedBandwidth = 0;
    }
    
    /**
     * Creates a new MyStatistics object, in case it does'n exist yet.
     * 
     * @return the MyStatistics singletonObject
     */
    public static synchronized MyStatistics getMyStatisticsObject() {
        if (singletonObject == null) {
            singletonObject = new MyStatistics();
        }
        return singletonObject;
    }
    
    /**
     * Throws an exception to stop a cloned MyStatistics object from
     * being created.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
    
    /**
     * Attributes initializer.
     *
     * @param plotter the graph plotter
     * @param pt the pt
     * @param traffic 
     * @param numNodes number of nodes in the network
     * @param numClasses number of classes of service
     * @param minNumberArrivals minimum number of arriving events
     * @param load the load of the network
     */
    public void statisticsSetup(OutputManager plotter, PhysicalTopology pt, TrafficGenerator traffic, int numNodes, int numClasses, int minNumberArrivals, double load) {
    	this.plotter = plotter;
    	this.pt = pt;
    	this.traffic = traffic;
        this.numNodes = numNodes;
        this.load = load;
        this.arrivalsPairs = new int[numNodes][numNodes];
        this.blockedPairs = new int[numNodes][numNodes];
        this.requiredBandwidthPairs = new int[numNodes][numNodes];
        this.blockedBandwidthPairs = new int[numNodes][numNodes];

        this.minNumberArrivals = minNumberArrivals;
        numberOfUsedTransponders = new int[numNodes][numNodes];
        //Diff
        this.arrivalsDiff = new int[numClasses];
        this.blockedDiff = new int[numClasses];
        this.requiredBandwidthDiff = new int[numClasses];
        this.blockedBandwidthDiff = new int[numClasses];
        for (int i = 0; i < numClasses; i++) {
            this.arrivalsDiff[i] = 0;
            this.blockedDiff[i] = 0;
            this.requiredBandwidthDiff[i] = 0;
            this.blockedBandwidthDiff[i] = 0;
        }
        this.arrivalsPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedPairsDiff = new int[numNodes][numNodes][numClasses];
        this.requiredBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        this.blockedBandwidthPairsDiff = new int[numNodes][numNodes][numClasses];
        this.totalPowerConsumed = 0;
        this.simTime = 0;
        this.dataTransmitted = 0;
        
        modulationFormat = new double[6];//number of modulation format used in this simulator
        for(int i = 0; i < modulationFormat.length; i++) {
        	modulationFormat[i] = 0;
        }
        
        coreUsed = new double[pt.getCores()];
        for(int i = 0; i < coreUsed.length; i++) {
        	coreUsed[i] = 0;
        }
    }
	/**
	 * Calculate last statistics for the graph generation.
	 */
	public void calculateLastStatistics(){
		
		//bandwidth block graph
		plotter.addDotToGraph("mbbr", load, ((float) blockedBandwidth) / ((float) requiredBandwidth));
		plotter.addDotToGraph("bp", load, ((float) blocked) / ((float) arrivals));
		int count = 0;
        float bbr = 0, jfi, sum1 = 0, sum2 = 0;
        if (blocked == 0) {
            bbr = 0;
        } 
        else {
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        
//        System.out.println(bbr);
        
        double bbrJFI = 0;
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (i != j) {
//                    if (blockedPairs[i][j] == 0) {
//                        bbr = 0;
//                    } else {
                        bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                        bbrJFI = ((float) ((float) requiredBandwidthPairs[i][j] - blockedBandwidthPairs[i][j]) ) / ((float) requiredBandwidthPairs[i][j]) * 100;
//                    }
                    count++;
                    sum1 += bbrJFI;
                    sum2 += bbrJFI * bbrJFI;
                }
            }
        }
        jfi = (sum1 * sum1) / ((float) count * sum2);
        plotter.addDotToGraph("jfi", load, jfi);
//        System.out.println("jfi= "+jfi+ "sum1= "+sum1+" sum2="+sum2 + " "+count);
    	//POWE CONSUPTION
    	double PCoxc = 0;
    	for (int i = 0; i < pt.getNumNodes(); i++) {
			PCoxc = PCoxc += pt.getNodeDegree(i) * 85 + 150;
		}
    	double PCedfa = pt.getNumLinks() * 200;
    	totalPowerConsumed += simTime * (PCoxc + PCedfa);
    	plotter.addDotToGraph("pc", load, (totalPowerConsumed)/(simTime*1000));
    	plotter.addDotToGraph("ee", load, dataTransmitted/( totalPowerConsumed / 1000));
    	plotter.addDotToGraph("data", load, dataTransmitted);
    	plotter.addDotToGraph("ee2", load, (((float) blockedBandwidth) / ((float) requiredBandwidth)) / (totalPowerConsumed/(simTime*1000)));
  
    	int n = accepted;
    	
    	if(nMultipaths >= 1) {
    		n = nMultipaths;
    	}
    	
    	//modulation statistics
    	double []modulationParams = new double[modulationFormat.length + 1];
    	modulationParams[0] = load;
    	for (int i = 0; i < modulationFormat.length; i++) {
    		modulationParams[i+1] = (modulationFormat[i]/n);
    	}
    	plotter.addDotToGraph("modulation", modulationParams);
    	
    	
    	//inter-core crosstalk statistics
    	double xtLevel = 60 + ((double)sumXT / (double)n);//60db is the capacity of switch optical
    	plotter.addDotToGraph("xt",load, xtLevel);
    	
    	double xtAvg = ((double)sumXT / (double)n);
    	plotter.addDotToGraph("xtavg", load, xtAvg);
    	
    	//core-index statistics
    	double []cores = new double[coreUsed.length + 1];
    	cores[0] = load;
    	for (int i = 0; i < coreUsed.length; i++) {
    		cores[i+1] = (coreUsed[i]/n);
    	}
    	plotter.addDotToGraph("cores", cores);
    	
    	//average path length
    	plotter.addDotToGraph("avgpathlength", load, (pathLength/(double)n) );
    	
    	int nlinks = 0;
    	for(int i = 0; i < pt.getNumLinks(); i++) {
    		double xt = pt.getLink(i).getSumOfInterCoreCrosstalk();
    		if(xt < 0) {
    			sumXTLinks += xt;
    			nlinks++;
    		}
        	
        }
        
    	plotter.addDotToGraph("xtmean",load, (sumXTLinks / (double) nlinks));
	}
	
	
	public boolean[][]bitMapAll(int []links) {
		
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		for (int i = 0; i < pt.getCores(); i++) {
			for (int j = 0; j < pt.getNumSlots(); j++) {
				spectrum[i][j] = true;
			}
		}
		
		for(int i : links) {
			bitMap(pt.getLink(i).getSpectrum(), spectrum, spectrum);
		}
		
		return spectrum;
	}
	
	public void bitMap(boolean[][] s1, boolean[][] s2, boolean[][] result) {

		for (int i = 0; i < result.length; i++) {
			
			for (int j = 0; j < result[i].length; j++) {
				result[i][j] = s1[i][j] && s2[i][j];
			}
		}
	}
	
	public double getFragmentatioRatio() {
		double availableSlots = 0;
		
		double w = 0;
		int count = 0;
		for (int i = 0; i < pt.getNumNodes()-1; i++) {
			for (int j = i+1; j < pt.getNumNodes(); j++) {
				
				org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> kShortestPaths1 = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getGraph(), 1);
				List< GraphPath<Integer, DefaultWeightedEdge> > KPaths = kShortestPaths1.getPaths( i, j );
				
				List<Integer> listOfVertices = KPaths.get(0).getVertexList();
				int[] links = new int[listOfVertices.size()-1];
				for (int a = 0; a < listOfVertices.size()-1; a++) {
					
					links[a] = pt.getLink(listOfVertices.get(a), listOfVertices.get(a+1)).getID();
				}
				
				int maxG = pt.getNumSlots() + 1;
				boolean [][]spectrum = bitMapAll(links);
				for (int a = 0; a < spectrum.length; a++) {
					int n = 0;
					int max = 0;
					for (int b = 0; b < spectrum[a].length; b++) {
						if(spectrum[a][b]) {
							n++;
							if(n > max) {
								max = n;
							}
						}
						else
						{
							n = 0; 
						}
					}
					maxG = Math.min(maxG, max);
				}
				w += ((double) (maxG) / (double)(pt.getNumSlots()));
				count++;
			}
			
		}

		availableSlots = w / (double)count  ;
		return (1.0 - availableSlots);
		
	}
	
	/**
	 * This metric is based on: Spectrum Management in Heterogeneous Bandwidth Networks
	 * Authors: Rui Wang and Biswananth Mukherjee
	 * Globecom 2012
	 * @return
	 */
	public void setFragmentatioRatio() {
		double availableSlots = 0;
		
		double w = 0;
		int count = 0;
		for (int i = 0; i < pt.getNumNodes()-1; i++) {
			for (int j = i+1; j < pt.getNumNodes(); j++) {
				
				org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> kShortestPaths1 = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getGraph(), 1);
				List< GraphPath<Integer, DefaultWeightedEdge> > KPaths = kShortestPaths1.getPaths( i, j );
				
				List<Integer> listOfVertices = KPaths.get(0).getVertexList();
				int[] links = new int[listOfVertices.size()-1];
				for (int a = 0; a < listOfVertices.size()-1; a++) {
					
					links[a] = pt.getLink(listOfVertices.get(a), listOfVertices.get(a+1)).getID();
				}
				
				int maxG = pt.getNumSlots() + 1;
				boolean [][]spectrum = bitMapAll(links);
				for (int a = 0; a < spectrum.length; a++) {
					int n = 0;
					int max = 0;
					for (int b = 0; b < spectrum[a].length; b++) {
						if(spectrum[a][b]) {
							n++;
							if(n > max) {
								max = n;
							}
						}
						else
						{
							n = 0; 
						}
					}
					maxG = Math.min(maxG, max);
				}
				w += ((double) (maxG) / (double)(pt.getNumSlots()));
				count++;
			}
			
		}

		availableSlots = w / (double)count  ;
		plotter.addDotToGraph("availableslotratio", load, availableSlots);
		double fragmentationRatio = 1.0 - availableSlots;
		
		plotter.addDotToGraph("fragmentationratio", load, fragmentationRatio);
	}
	/**
	 * Calculate periodical statistics.
	 */
	public void calculatePeriodicalStatistics(){
		
		setFragmentatioRatio();
		
		
		//fragmentation graph
		double fragmentationMean = 0;
    	for (int i = 0; i < pt.getNumLinks(); i++) {
    		try {
    			fragmentationMean += pt.getLink(i).getFragmentationRatio(traffic.getCallsTypeInfo(), 12.5);//pt.getSlotCapacity());
    		} catch (NullPointerException e) {
    			
    		}
		}
    	fragmentationMean = fragmentationMean / pt.getNumLinks();
    	
    	plotter.addDotToGraph("fragmentation", load, fragmentationMean);
    	double meanTransponders = 0;
    	for (int i = 0; i < numberOfUsedTransponders.length; i++) {
			for (int j = 0; j < numberOfUsedTransponders[i].length; j++) {
				if (numberOfUsedTransponders[i][j]>0){
					meanTransponders += numberOfUsedTransponders[i][j];
				}
			}
		}
    	
//    	meanTransponders = meanTransponders / size;
    	if (Double.compare(meanTransponders, Double.NaN)!=0){
    		plotter.addDotToGraph("transponders", load, meanTransponders);
    	}
    	double xtps = 0;
    	int linksXtps = 0;
    	for (int i = 0; i < pt.getNumLinks(); i++) {
    		try {
    			double xt = pt.getLink(i).getCrossTalkPerSlot();
    			if (xt>0){
    				xtps += xt;
    				linksXtps++;
    			}
    		} catch (NullPointerException e) {
    			
    		}
		}
    	if (xtps!=0)
    		plotter.addDotToGraph("xtps", load, xtps/ linksXtps);
    	
    	//BFR
//    	double BFR = 0;
//    	for (int i = 0; i < pt.getNumLinks(); i++) {
//			FlexGridLink link = pt.getLink(i);
//			double sumbe = link.getNumFreeSlots();
//			int B = link.getSlots();
//			double psi =0;
//			
//			if (sumbe < B) {
//				psi = (double)1.0 - (link.maxNumberOfContiguousSlots()/sumbe);
//			} else {
//				psi = 0;
//			}
//			BFR += psi;
//		}
//    	BFR = BFR/pt.getNumLinks();
//    	plotter.addDotToGraph("bfr", load, BFR);
	}
	
    /**
     * Adds an accepted flow to the statistics.
     * 
     * @param flow the accepted Flow object
     * @param lightpath lightpath of the flow
     */
    public void acceptFlow(Flow flow, LightPath lightpath) {
        if (this.numberArrivals > this.minNumberArrivals){
        	if (flow == null) {
        		return;
        	}
        	
        	if (flow.isBatchRequest) {
        		this.accepted += Math.max(flow.getNumberOfFlowsGroomed(), 1);
        	} else {
        		this.accepted++;
        		this.modulationFormat[flow.getModulationLevel()]++;
        		
        	}
//        	System.out.println("updated now: "+this.accepted+" ID:"+flow.getID());
        	
        	if (flow.getLinks() == null) {
        		return;
        	}
        	
        	int links =  flow.getLinks().length+1;
//        	plotter.addDotToGraph("modulation", load, (flow.getModulationLevel()+1) );
            plotter.addDotToGraph("hops", load, links);
           
           
            
            dataTransmitted += flow.getRate();
            for (int i = 0; i < pt.getCores(); i++) {
            	totalPowerConsumed += flow.getDuration() * flow.getSlotList().size() * Modulations.getPowerConsumption(flow.getModulationLevel());
            }
            numberOfUsedTransponders[flow.getSource()][flow.getDestination()]++;
        }
    }
    
    
    /**
     * Adds an accepted flow to the statistics.
     * 
     * @param flow the accepted Flow object
     * @param lightpath lightpath of the flow
     */
    public void acceptFlow(Flow flow, ArrayList<LightPath> lightpath) {
        if (this.numberArrivals > this.minNumberArrivals){
        	if (flow == null) {
        		return;
        	}
        	
        	if (flow.isBatchRequest) {
        		this.accepted += Math.max(flow.getNumberOfFlowsGroomed(), 1);
        	} else {
        		this.accepted++;
        		
        		if(flow.isMultipath()) for(int modulation : flow.getModulationLevels()) this.modulationFormat[modulation]++;
        		
        	}
//        	System.out.println("updated now: "+this.accepted+" ID:"+flow.getID());
        	
        	if (flow.getLinks() == null) {
        		return;
        	}
        	
        	
        	ArrayList<int[]> multipaths = flow.getMultipath();
        	for(int k = 0; k < multipaths.size() ; k++) {
	        	int links =  multipaths.get(k).length+1;
	        	plotter.addDotToGraph("modulation", load, flow.getModulationLevel(k));
	            plotter.addDotToGraph("hops", load, links);
	            dataTransmitted += flow.getRate();
	            for (int i = 0; i < pt.getCores(); i++) {
	            	totalPowerConsumed += flow.getDuration() * flow.getSlotList().size() * Modulations.getPowerConsumption(flow.getModulationLevel());
	            }
	            numberOfUsedTransponders[flow.getSource()][flow.getDestination()]++;
        	}
        }
    }
    
    /**
     * Groomed flow.
     *
     * @param flow the flow
     */
    public void groomedFlow(Flow flow){
    	if (this.numberArrivals > this.minNumberArrivals){
            dataTransmitted += flow.getRate();
            for (int i = 0; i < pt.getCores(); i++) {
            	totalPowerConsumed += flow.getDuration() * flow.getSlotList().size() *Modulations.getPowerConsumption(flow.getModulationLevel());
            }
        }
    }
    /**
     * Adds a blocked flow to the statistics.
     * 
     * @param flow the blocked Flow object
     */
    public void blockFlow(Flow flow) {
        if (this.numberArrivals > this.minNumberArrivals) {
        	
        	if (flow.isBatchRequest) {
        		this.blocked += Math.max(flow.getNumberOfFlowsGroomed(), 1);
        	} else {
        		this.blocked++;
        	}
        	
        	if(flow.isConnectionDisruption()) {
        
                this.modulationFormat[flow.getModulationLevel()] -= 1;
                this.pathLength -= flow.getPathLength();
                this.accepted--;
        	}
    		
        	int cos = flow.getCOS();
            this.blockedDiff[cos]++;
            this.blockedBandwidth += flow.getRate();
            this.blockedBandwidthDiff[cos] += flow.getRate();
            this.blockedPairs[flow.getSource()][flow.getDestination()]++;
            this.blockedPairsDiff[flow.getSource()][flow.getDestination()][cos]++;
            this.blockedBandwidthPairs[flow.getSource()][flow.getDestination()] += flow.getRate();
            this.blockedBandwidthPairsDiff[flow.getSource()][flow.getDestination()][cos] += flow.getRate();
        }
    }
    
    /**
     * Adds an event to the statistics.
     * 
     * @param event the Event object to be added
     */
    public void addEvent(Event event) {
    	simTime = event.getTime();
        try {
            if (event instanceof FlowArrivalEvent) {
                this.numberArrivals++;
                if (this.numberArrivals > this.minNumberArrivals) {
                    int cos = ((FlowArrivalEvent) event).getFlow().getCOS();
                    this.arrivals++;
                    this.arrivalsDiff[cos]++;
                    this.requiredBandwidth += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthDiff[cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.arrivalsPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()]++;
                    this.arrivalsPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos]++;
                    this.requiredBandwidthPairs[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()] += ((FlowArrivalEvent) event).getFlow().getRate();
                    this.requiredBandwidthPairsDiff[((FlowArrivalEvent) event).getFlow().getSource()][((FlowArrivalEvent) event).getFlow().getDestination()][cos] += ((FlowArrivalEvent) event).getFlow().getRate();
                }
                if (Simulator.verbose && Math.IEEEremainder((double) arrivals, (double) 10000) == 0) {
                    System.out.println(Integer.toString(arrivals));
                }
            }
            else if (event instanceof FlowDepartureEvent) {
                if (this.numberArrivals > this.minNumberArrivals) {
                    this.departures++;
                }
                Flow f = ((FlowDepartureEvent)event).getFlow();
                if (f.isAccepeted()){
                	this.numberOfUsedTransponders[f.getSource()][f.getDestination()]--;
                }
            }
            if (this.numberArrivals % 100 == 0){
            	calculatePeriodicalStatistics();
            	
            }
            if (this.numberArrivals % 5000 == 0){
            	
//            	System.out.println(event.getTime()+","+BFR);
            }
        }
        
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * This function is called during the simulation execution, but only if
     * verbose was activated.
     * 
     * @return string with the obtained statistics
     */
    public String fancyStatistics() {
        float acceptProb, blockProb, bbr;
        if (accepted == 0) {
            acceptProb = 0;
        } else {
            acceptProb = ((float) accepted) / ((float) arrivals) * 100;
        }
        if (blocked == 0) {
            blockProb = 0;
            bbr = 0;
        } else {
            blockProb = ((float) blocked) / ((float) arrivals) * 100;
            bbr = ((float) blockedBandwidth) / ((float) requiredBandwidth) * 100;
        }
        
        double ct = ((double)sumXT / (double)accepted);
        
        for(int i = 0; i < pt.getNumLinks(); i++) {
        	sumXTLinks += pt.getLink(i).getSumOfInterCoreCrosstalk();
        }
        
        double ctLinks = sumXTLinks / (double) pt.getNumLinks();

        String stats = "Arrivals \t: " + Integer.toString(arrivals) + "\n";
        stats += "Required BW \t: " + Integer.toString(requiredBandwidth) + "\n";
        stats += "Departures \t: " + Integer.toString(departures) + "\n";
        stats += "Accepted \t: " + Integer.toString(accepted) + "\t(" + Float.toString(acceptProb) + "%)\n";
        stats += "XT-flow: "+ "\t" + Double.toString(ct) + "\n";
        stats += "XT-links: "+ "\t" + Double.toString(ctLinks) + "\n";
        stats += "Blocked \t: " + Integer.toString(blocked) + "\t(" + Float.toString(blockProb) + "%)\n";
        stats += "BBR     \t: " + Float.toString(bbr) + "%\n";
        stats += "\n";
        stats += "Blocking probability per s-d pair:\n";
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                stats += "Pair (" + Integer.toString(i) + "->" + Integer.toString(j) + ") ";
                stats += "Calls (" + Integer.toString(arrivalsPairs[i][j]) + ")";
                if (blockedPairs[i][j] == 0) {
                    blockProb = 0;
                    bbr = 0;
                } else {
                    blockProb = ((float) blockedPairs[i][j]) / ((float) arrivalsPairs[i][j]) * 100;
                    bbr = ((float) blockedBandwidthPairs[i][j]) / ((float) requiredBandwidthPairs[i][j]) * 100;
                }
                stats += "\tBP (" + Float.toString(blockProb) + "%)";
                stats += "\tBBR (" + Float.toString(bbr) + "%)\n";
            }
        }

        return stats;
    }
	
    
    /**
     * Terminates the singleton object.
     */
    public void finish()
    {
        singletonObject = null;
    }

	public void updateInterCoreCrosstalk(Flow flow) {
		
		nMultipaths = accepted;
		if(!flow.isMultipath()){ 
			coreUsed[flow.getSlotList().get(0).c]++;
		}
		else
		{
			for(ArrayList<Slot> s: flow.getMultiSlotList()) {
				coreUsed[s.get(0).c]++;
			}
			
			nMultipaths += (flow.getMultipath().size()-1);
		}
		
		if(flow.getPathLength() == 0) {
			System.out.println("ERROR while getting path length");
		}
//		System.out.println(flow.getSumOfXT());
		pathLength +=  (double)flow.getPathLength();
		sumXT += flow.getSumOfXT();
	}
}