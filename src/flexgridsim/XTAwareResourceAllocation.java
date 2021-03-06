package flexgridsim;

import java.util.Arrays;
import java.util.LinkedList;

public class XTAwareResourceAllocation {
	
	//parameters to calculate mean inter-core crosstalk
	//Data: Resource Allocation for Space-Division Multiplexing: Optical White Box Versus Optical Black Box Networking
	private static final double B = Math.pow( 4 * Math.pow(10,6), 1);//Propagation constant
	private static final double R = 0.05f;//Bending radius
	private static final double corePitch = 4 * Math.pow(10, -5);
	private static final double k = 4 * Math.pow(10, -4);//Coupling coefficient and modulation format is fixed
	
	private Graph cores;
	
	protected int numberOfCores;
	protected int numberOfCoresAvailable;
	protected int []availableSlots;
	protected double [][]xt;
	protected double [][]modulationDbLimit;
	protected int numberOfSlots;
	
	public XTAwareResourceAllocation(int nOfCores, int nCoresAvailable, int nSlots) {

		this.numberOfCores = nOfCores;
		this.numberOfCoresAvailable = nCoresAvailable;
		this.numberOfSlots = nSlots;
		
		this.xt = new double[this.numberOfCores][this.numberOfSlots];
		this.modulationDbLimit = new double[this.numberOfCores][this.numberOfSlots];
		for(int i = 0; i < this.numberOfCores; i++) {
			for(int j = 0; j < this.numberOfSlots; j++) {
				this.xt[i][j] = modulationDbLimit[i][j] = 0.0f;
			}
			
		}
		
		this.createGraph();
	}
	
	public int getNumberOfCoresAvailable() {
		return numberOfCoresAvailable;
	}

	public void setNumberOfCoresAvailable(int numberOfCoresAvailable) {
		this.numberOfCoresAvailable = numberOfCoresAvailable;
	}
	
	public void resetCrosstalk(int c, int s) {
		xt[c][s] = -80;
		modulationDbLimit[c][s] = -80;
	}
	
	protected double interCoreCrosstalk(int core, int slot, double n, double L) {
		
		if(n == 0) {
			this.xt[core][slot] = -80;
			this.modulationDbLimit[core][slot] = 0;
			return -80;
		}

		double h = (Math.pow(k, 2) / B) * (R / corePitch);
//		System.out.println(h +" "+  10.0f * Math.log10(h/10.0));
		L = (L*1000);
		double exponential = (-1 * (n + 1)) * 2 * h * L;
		
		this.xt[core][slot] = ( n - n * Math.exp(exponential) ) / ( 1.0f + n * Math.exp(exponential) );

//		System.out.println(this.xt[core][slot]);
//		this.meanXT[core] = xt > 0 ? 10.0f * Math.log10(xt)/Math.log10(10) : 0.0f;
//		System.out.println(""+core+" nCores: "+n+" db: "+10.0f * Math.log10(this.meanXT[core])/Math.log10(10)+" xt: "+this.meanXT[core]);
		
		return this.xt[core][slot];
	}
	
	static class Graph
    {
        int V;
        LinkedList<Integer> adjListArray[];
         
        @SuppressWarnings("unchecked")
        Graph(int V)
        {
            this.V = V;
             
            // define the size of array as 
            // number of vertices
            adjListArray = new LinkedList[V];
             
            // Create a new list for each vertex
            // such that adjacent nodes can be stored
            for(int i = 0; i < V ; i++){
                adjListArray[i] = new LinkedList<>();
            }
        }
        /*
         * Adds an edge to an undirected graph
         */
        static void addEdge(Graph graph, int src, int dest)
        {
            // Add an edge from src to dest. 
            graph.adjListArray[src].addFirst(dest);
             
            // Since graph is undirected, add an edge from dest
            // to src also
            graph.adjListArray[dest].addFirst(src);
        }
          
        /*
         *  A utility function to print the adjacency list 
         *  representation of graph
         */
        static void printGraph(Graph graph)
        {       
            for(int v = 0; v < graph.V; v++)
            {
                System.out.println("Adjacency list of vertex "+ v);
                System.out.print("head");
                for(Integer pCrawl: graph.adjListArray[v]){
                    System.out.print(" -> "+pCrawl);
                }
                System.out.println("\n");
            }
        }
    }
     
	private void createGraph() {
		
		this.cores = new Graph(numberOfCores);
		
		//create a graph based in 7-core (star)
		if(numberOfCores == 7) 
		{
			for(int i = 1; i < numberOfCores; i++) {
				Graph.addEdge(cores, 0, i);
			}
			
			Graph.addEdge(cores, 1, numberOfCores-1);

			for(int i = 1; i < numberOfCores-1; i++) {
				
				Graph.addEdge(cores, i, i+1);
			}
			
			
		}
		else if(numberOfCores == 12) {
			
			for(int i = 1; i < (numberOfCores-1); i++) {
				Graph.addEdge(cores, i-1, i);
			}
			
			Graph.addEdge(cores, 0, numberOfCores);
		}
//		System.out.println("GRAPH-XT");
//		 Graph.printGraph(cores);
	}
	
	protected LinkedList<Integer> getAdjacentsCores(int index) {
		
		return cores.adjListArray[index];
	}
	
	
	public void printXTMatrix() {
		
		for(int i = 0; i < numberOfCores; i++) {
			
			System.out.println(Arrays.toString(xt[i]));
		}
		
	}

	public double getXT(int core, int slot) {
		
		return this.xt[core][slot];
	}

	public void setLimitDB(int c, int s, double db) {
		this.modulationDbLimit[c][s] = db;
	}
}
