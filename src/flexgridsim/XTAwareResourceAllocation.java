package flexgridsim;

import java.util.LinkedList;

public class XTAwareResourceAllocation {
	
	//parameters to calculate mean inter-core crosstalk
	private static final double B = 4.0f*Math.pow(10.0f,6);//Propagation constant
	private static final double R = 0.055f;//Bending radius
	private static final double corePitch = 4.5f * Math.pow(10.0f, -5);
	private static final double k = 3.16f * Math.pow(10.0f, -5);//Coupling coefficient and modulation format is fixed
	
	private Graph cores;
	
	protected int numberOfCores;
	protected int numberOfCoresAvailable;
	protected int []availableSlots;
	protected double []meanXT;
	
	public XTAwareResourceAllocation(int numberOfCores, int numberOfCoresAvailable) {
		super();
		this.numberOfCores = numberOfCores;
		this.numberOfCoresAvailable = numberOfCoresAvailable;
		
//		System.out.println("B= "+B+" R ="+R+" c= "+corePitch+" k= "+k);
		this.createGraph();
	}
	
	public int getNumberOfCoresAvailable() {
		return numberOfCoresAvailable;
	}

	public void setNumberOfCoresAvailable(int numberOfCoresAvailable) {
		this.numberOfCoresAvailable = numberOfCoresAvailable;
	}
	
	protected double meanInterCoreCrosstalk(int core, double n, double L) {
		
		double h = (2.0f*Math.pow(k, 2) * R) / (Math.pow(B, 1) * corePitch);
		double exponential = (-1.0f * (n + 1.0f)) * h * L;
		double xt = ( ( n - n * Math.exp(exponential) ) / (1.0f + n * Math.exp(exponential) ) );
		
		return xt;
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
			for(int i = 0; i < numberOfCores; i++) {
				Graph.addEdge(cores, 0, i);
			}
			
			Graph.addEdge(cores, 1, numberOfCores-1);

			for(int i = 2; i < numberOfCores-1; i++) {
				
				Graph.addEdge(cores, i-1, i);
				Graph.addEdge(cores, i, i+1);
			}
		}
		else if(numberOfCores == 12) {
			
			for(int i = 1; i < (numberOfCores-1); i++) {
				Graph.addEdge(cores, i-1, i);
			}
			
			Graph.addEdge(cores, 0, numberOfCores-1);
		}
		
	}
	
	protected LinkedList<Integer> getAdjacentsCores(int index) {
		
		return cores.adjListArray[index];
	}

	public double getXT(int core) {
		// TODO Auto-generated method stub
		return 0;
	}
}
