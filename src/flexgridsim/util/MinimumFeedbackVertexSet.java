package flexgridsim.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import flexgridsim.Flow;

/**
 * Minimum Feedback Vertex Set Algorithm
 * 
 * Paper: Computing Minimum Feedback Vertex Sets by Contraction Operations and its Applications on CAD
 * 
 * @author trindade
 *
 */

public class MinimumFeedbackVertexSet {

	protected Graph graph;
	protected Map<Integer, Long> associationBetweenNodes;//association between vertices and flows
	protected Map<Long, Integer> associationBetweenFlows;
	Map<Long, Flow> flows;
	
	public Graph getGraph() {
		return graph;
	}

	
	public MinimumFeedbackVertexSet(Map<Long, Flow> f) {
		
		
		this.associationBetweenNodes = new HashMap<Integer, Long>();
		this.associationBetweenFlows = new HashMap<Long, Integer>();
		
		Integer node = 0;
		flows = f;
		for(Long key: flows.keySet()) {
			
			associationBetweenNodes.put(node, key);
			associationBetweenFlows.put(flows.get(key).getID(), node);
			node++;
		}
		
		this.graph = new Graph(flows.size());	
	}
	
	
	public ArrayList<Flow> runMFVS() {
		
		ArrayList<Integer> mfvs = new ArrayList<Integer>();
		ArrayList<Flow> flowsToVacancy = new ArrayList<Flow>();
		
		try {
			
			mfvs = graph.minimumFeedbackVertexSet(false);
			System.out.println("A minimum feedback vertex set : "+mfvs);
			
			for(int i = 0; i < mfvs.size(); i++) {
				
				flowsToVacancy.add( flows.get( associationBetweenNodes.get(mfvs.get(i)) ) );
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return flowsToVacancy;
	}
	
	
	public int getNodeIndex(Long key) {
		
		return this.associationBetweenFlows.get(key);
	}
	
	public long getNodeIndex(int key) {
		
		return this.associationBetweenNodes.get(key);
	}
	
	
	static class Edge {
		public int source;
		public int target;
		
		Edge() {source = -1; target = -1;}
	}

	public static class Graph
    {
        private int V;// The maximum number of allowed vertices
        private ArrayList< ArrayList<Integer> > outgoingNeighbors; // The list of outgoing neighbors
        private ArrayList< ArrayList<Integer> > incomingNeighbors; // The list of incoming neighbors
        private Set<Integer> vertices;                   // The set of vertices of the graph
        private ArrayList<Boolean> hasVertex;            // A map indicating which vertices belong to the graph                      
        private int numVertices = 0;                         // The current number of vertices
        private int numEdges = 0;                            // The current number of edges
	    
	    
        private int tarjanCurrentIndex = 0;                   // The current index in the search
        private ArrayList<Integer> tarjanIndex;           // The search index of each vertex
        private ArrayList<Integer> tarjanAncestor;        // The lowest ancestor of each vertex
        private Stack<Integer> tarjanStack;               // The stack used for Tarjan's algorithm
        private ArrayList<Boolean> tarjanInStack;         // Tells if the vertex is in the stack
        private int currentComponent = 0;                     // The current strongly connected component
        private ArrayList<Integer> sccsByNum;             // A vector associating each vertex with                                          
        
        private ArrayList< ArrayList<Integer> > sccs;     // The strongly connected components
        
        private ArrayList<Integer> strongVertices;
         
        Graph(int V)
        {
            this.V = V;
            init(V);
        }
        
        protected void addVertex(int vertex) {
            if (vertex >= 0 && vertex < V && !hasVertex(vertex)) {
                vertices.add(vertex);
                hasVertex.set(vertex, true);
                numVertices--;
            }
        }

        protected void deleteVertices(ArrayList<Integer> v) throws Exception {
        	int n = v.size();
        	for(int i = n-1; i >= 0; i--) {
                this.deleteVertex(v.get(i));
            }
        }
        
        public ArrayList<Integer> getStrongVertices() {
        	
        	return this.strongVertices;
        }

        /*
         * Comparison operators for edges *
         */
        protected boolean operatorEqual(Edge e1, Edge e2) {
            return e1.source == e2.source && e1.target == e2.target;
        }

        protected boolean operatorLess(Edge e1, Edge e2) {
            if (e1.source < e2.source) {
            	return true;
            }
            else if (e1.source == e2.source && e1.target < e2.target) {
            	return true;
            }
            
            return false;
        }

        protected boolean operatorGreater(Edge e1,  Edge e2) {
            return operatorLess(e2, e1);
        }

        protected boolean operatorLessEquals(Edge e1, Edge e2) {
            return operatorLess(e1, e2) || e1 == e2;
        }
		
        protected boolean operatorGreaterEquals( Edge e1, Edge e2) {
            return operatorGreater(e1, e2) || e1 == e2;
        }



        /*
         * Constructors and destructor *
         */
		protected void init(int maxSize) {
            this.outgoingNeighbors = new ArrayList< ArrayList<Integer> >(maxSize);
            this.incomingNeighbors = new ArrayList< ArrayList<Integer> >(maxSize);
            this.hasVertex = new ArrayList<Boolean>();
            this.vertices = new TreeSet<Integer>();
            
            for (int i = 0; i < maxSize; i++) {
            	this.hasVertex.add(false);
            	this.outgoingNeighbors.add(new ArrayList<>());
            	this.incomingNeighbors.add(new ArrayList<>());
            	this.addVertex(i);
            }
            
            this.tarjanCurrentIndex = 0;
			this.tarjanIndex = new ArrayList<Integer>();
			this.tarjanAncestor = new ArrayList<Integer>();
			this.tarjanStack = new Stack<Integer>();
			this.tarjanInStack = new ArrayList<Boolean>();
			this.sccs = new ArrayList< ArrayList<Integer> >();
			this.sccsByNum = new ArrayList<Integer>();
            
            this.V = maxSize;
            this.numVertices = 0;
            this.numEdges  = 0;
            
            this.strongVertices = new ArrayList<Integer>();
        }


        /*
         * Basic accessors *
        */
        public int numberOfVertices() {
            return numVertices;
        }

        int numberOfEdges() {
            return numEdges;
        }

        protected boolean hasVertex(int vertex) {
            if (vertex < 0 || vertex >= V) 
            {
            	return false;
            }
            else 
            {
            	return hasVertex.get(vertex);
            }
        }

        protected boolean hasEdge(int source, int target) throws Exception {
            if (!hasVertex(source) || !hasVertex(target)) {
                return false;
            } 
            else 
            {
                return find(this.outgoingNeighbors.get(source), target) >= 0;

            }
        }

        protected Set<Integer> vertices() {
            return vertices;
        }

        protected ArrayList<Edge> edges() {
    	   ArrayList<Edge> allEdges = new ArrayList<Edge>();
           
    	   for (int i = 0; i < V; i++) {
        	
				Iterator< Integer > jt = this.outgoingNeighbors.get(i).iterator(); 	
				while(jt.hasNext()) {
				        Edge e = new Edge();
				        e.source = i;
				        e.target = jt.next();
				        allEdges.add(e);
				}
           }
           return allEdges;
        }

       protected ArrayList<Integer> incomingNeighbors(int vertex) throws Exception{
            if (hasVertex(vertex)) {
            	return this.incomingNeighbors.get(vertex);
            }
            else {
            	throw new Exception("The vertex does not exist.");
            }
        }

       public ArrayList<Integer> getOutgoingNeighbors(int vertex) throws Exception {
            if (hasVertex(vertex)) 
            {
            	return this.outgoingNeighbors.get(vertex);
            }
            else {
            	throw new Exception("The vertex "+vertex+" does not exist.");
            }
        }

       public void print(boolean edges) throws Exception{
            
            if (edges) 
            {
            	System.out.println(": ");
            	
            	Iterator<Integer> it = vertices.iterator();
            	while (it.hasNext()){
                    
            		ArrayList<Integer> neighbors = getOutgoingNeighbors(it.next());
                    for (int j = 0; j < neighbors.size(); j++) {
                    	System.out.println( "(" + it +"," + neighbors.get(j) + ") ");
                    }
                }
            }
        }

      public void deleteVertex(int vertex) throws Exception {
    	  	
    	  	if (hasVertex(vertex)) 
          	{
    	  		//System.out.println("Delete: "+vertex);  
    	  		ArrayList<Integer> outN = getOutgoingNeighbors(vertex);
    	  		//System.out.println("out "+outN);
    	  		int n = outN.size();
    	  		for (int i = n-1; i >= 0; i--) {
            	   deleteEdge(vertex, outN.get(i));
    	  		}
    	  		
    	  		ArrayList<Integer> inN  = incomingNeighbors(vertex);
    	  		//System.out.println("in "+inN);
    	  		n = inN.size();
    	  		for (int i = n-1; i >= 0 ;  i--) {
            	   deleteEdge(inN.get(i), vertex);
    	  		}
               
    	  		vertices.remove(vertex);
    	  		hasVertex.set(vertex, false);
    	  		numVertices--;
            }
    	  	else
    	  	{
    	  		
    	  	}
        }

		protected void deleteEdge(int source, int target) throws Exception {
        	
            if (hasEdge(source, target)) 
            {
            	this.outgoingNeighbors.get(source).remove(this.outgoingNeighbors.get(source).indexOf(target));
            	this.incomingNeighbors.get(target).remove(this.incomingNeighbors.get(target).indexOf(source));
            	numEdges--;
            }
        }
       
       public void addEdge(int source, int target) throws Exception {

    	    if (hasVertex(source) && hasVertex(target) && !hasEdge(source, target)) {
    	    	System.out.println(source +" - "+target);
    	        outgoingNeighbors.get(source).add(target);
    	        incomingNeighbors.get(target).add(source);
    	        numEdges++;
    	    }
    	}


       protected void deleteEdges(ArrayList<Edge> edges) throws Exception {
            
        	int n = edges.size();
        	for(int i = n-1; i >= 0; i--) 
        	{
        		Edge e = edges.get(i);
                deleteEdge(e.source, e.target);
            }
        }

       protected void mergeVertex(int vertex) throws Exception {
            if (hasVertex(vertex)) 
            {
               ArrayList<Integer> inN = incomingNeighbors(vertex);
               ArrayList<Integer> outN = getOutgoingNeighbors(vertex);
               
               Iterator<Integer> it = inN.iterator();
               while (it.hasNext()) {
            	   
            	   Iterator<Integer> jt = outN.iterator();
            	   while (it.hasNext()) 
            	   {
                       addEdge(it.next(), jt.next());
            	   }
               }
               
               deleteVertex(vertex);
            } 
            else 
            {
            	throw new Exception("The vertex does not exist.");
            }
        }

       protected void mergeVertices(ArrayList<Integer> vs) throws Exception {
            
        	if (vs.size() >= 1) 
            {
                int v = vs.get(0);
                Iterator<Integer> it = vs.iterator();
                while (it.hasNext()) {
                	
                	Integer u = it.next();
                    if (u != v) 
                    {
                       ArrayList<Integer> inN = incomingNeighbors(u);
                       ArrayList<Integer> outN = getOutgoingNeighbors(u);
                       
                       Iterator<Integer> jt = inN.iterator();
                       Integer kt;
                       while(jt.hasNext()) {
                    	   
                    	    Integer i = jt.next();
                            kt = find(vs, i);
                            if (kt == v || kt == -1) {
                            	addEdge(i, v);
                            }
                        }

                       jt = outN.iterator();
                       while(jt.hasNext()) {
                    	   Integer i = jt.next();
                            kt = find(vs, i);
                            
                            if (kt == v || kt == -1) {
                            	addEdge(v, i);
                            }
                        }
                       
                        deleteVertex(u);
                    }
                }
            }
        }
        
        protected Integer find(ArrayList<Integer> v, int a) {
        	
        	Iterator<Integer> it = v.iterator();
            while(it.hasNext()) {
            	if(it.next() == a) {
            		return a;
            	}
            }
            
            return -1;
        }

        protected void mergeVertices(int vertex1, int vertex2) throws Exception {
           ArrayList<Integer> vs = new ArrayList<Integer>();
            vs.add(vertex1);
            vs.add(vertex2);
            mergeVertices(vs);
        }



        /*
         * Subgraphs *
        */
		protected Graph subgraph(ArrayList<Integer> vs) throws Exception {
            Graph h = new Graph(V);
            
            Iterator<Integer> it = vs.iterator();
            h.vertices.clear();
            while (it.hasNext()){
                h.addVertex(it.next());
            }
            
            it = vs.iterator();
            while (it.hasNext()) {
            	Integer i = it.next();
                if (hasVertex(i)) 
                {
                   ArrayList<Integer> outN = getOutgoingNeighbors(i);
                   Iterator<Integer> jt = outN.iterator();
                   while (jt.hasNext()) {
                	   Integer j = jt.next();
                        if (h.hasVertex(j)) 
                        {
                        	h.addEdge(i, j);
                        }
                   }
                }
            }
            return h;
        }



        /*
         * Degrees, sources and sinks *
        */
        protected int inDegree(int vertex) throws Exception {
        	
            if (hasVertex(vertex)) return incomingNeighbors(vertex).size();
            else return -1;
        }

        protected int outDegree(int vertex) throws Exception {
        	
            if (hasVertex(vertex)) return getOutgoingNeighbors(vertex).size();
            else return -1;
        }

        protected int degree(int vertex) throws Exception {
            if (hasVertex(vertex)) return inDegree(vertex) + outDegree(vertex);
            else return -1;
        }

        protected int minDegreeVertex() throws Exception {
            int v = -1;
            int minDegree = 2 * numberOfVertices() + 1;
            for (int i = 0; i < V; i++) {
                if (hasVertex(i) && degree(i) < minDegree) {
                    v = i;
                    minDegree = degree(i);
                }
            }
            
            return v;
        }

        protected int maxDegreeVertex() throws Exception {
            int v = -1;
            int maxDegree = -1;
            for (int i = 0; i < V; i++) {
                if (hasVertex(i) && degree(i) > maxDegree) {
                    v = i;
                    maxDegree = degree(i);
                }
            }
            return v;
        }

        protected boolean isSource(int vertex) throws Exception {
            if (hasVertex(vertex)) 
            {
            	return inDegree(vertex) == 0;
            }
            
            return false;
        }

        protected boolean isSink(int vertex) throws Exception {
            if (hasVertex(vertex)) 
            {
            	return outDegree(vertex) == 0;
            }
            
            return false;
        }

        protected boolean hasLoop(int vertex) throws Exception {
            if (hasVertex(vertex))
            {
               return find(getOutgoingNeighbors(vertex), vertex) >= 0;
            }
            
            return false;
        }

       protected ArrayList<Integer> sources() throws Exception {
           ArrayList<Integer> v = new ArrayList<Integer>();
          
           Iterator<Integer> iterator = vertices.iterator();
           
           while(iterator.hasNext()) {
        	   Integer i = iterator.next();
               if (isSource(i)) 
               {
            	   v.add(i);
               }
            }
           //System.out.println(v);
            return v;
        }

	   protected ArrayList<Integer> sinks() throws Exception{
           ArrayList<Integer> v = new ArrayList<Integer>();
           
           Iterator<Integer> iterator = vertices.iterator();
           while(iterator.hasNext()) {
        	   Integer i = iterator.next();
        	   if (isSink(i)) 
                {
                	v.add(i);
                }
            }
            return v;
        }

       protected ArrayList<Integer> loops() throws Exception {
           
    	   ArrayList<Integer> v = new ArrayList<Integer>();
           
           Iterator<Integer> iterator = vertices.iterator();
           while(iterator.hasNext()) {
        	   Integer i = iterator.next();
               if (hasLoop(i)) v.add(i);
           }
           
           return v;
       }


        /*
         * Strongly connected components *
       */
       protected ArrayList< ArrayList<Integer> > stronglyConnectedComponents() throws Exception {
	            
			for (int i = 0; i < V; i++) {
			    tarjanIndex.add(-1);
			    tarjanInStack.add(false);
			    tarjanAncestor.add(-1);
			    sccs.add(new ArrayList<Integer>());
			}
			
			Iterator<Integer> iterator = vertices.iterator();
			
			while(iterator.hasNext()) {
				
				Integer i = iterator.next();
			    if (tarjanIndex.get(i).equals(-1) && hasVertex(i)) 
			    {
			    	this.tarjan(i, false);
			    }
			}
	
	        return sccs;
        }

       protected ArrayList<Integer> vertexToStronglyConnectedComponentNumber() throws Exception {
            
            for (int i = 0; i < V; i++) {
                tarjanIndex.add(-1);
                tarjanInStack.add(false);
                tarjanAncestor.add(-1);
                sccsByNum.add(-1);
            }
            
            Iterator<Integer> it = vertices.iterator();
            while(it.hasNext()) { 
            	Integer i = it.next();
            	
                if (tarjanIndex.get(i).equals(-1) && hasVertex(i)) {
                	tarjan(i, true);
                }
            }
            
            return sccsByNum;
        }

       /**
        * 
        * @param vertex
        * @param byNumber
        * @throws Exception
        */
       	protected void tarjan(int vertex, boolean byNumber) throws Exception {
            
    	   tarjanIndex.set(vertex, tarjanCurrentIndex);
           tarjanAncestor.set(vertex, tarjanCurrentIndex);
           tarjanCurrentIndex++;
           tarjanStack.push(vertex);
           tarjanInStack.set(vertex, true);
            
           ArrayList<Integer> outN = getOutgoingNeighbors(vertex);
            
           Iterator<Integer> it = outN.iterator();
           while(it.hasNext()) 
           {
        	    Integer i = it.next();
        	    
        	    if(hasVertex(i)) {
	        	   
	                if (tarjanIndex.get(i).equals(-1)) 
	                {
	                    tarjan(i, byNumber);
	                    tarjanAncestor.set(vertex, Math.min(tarjanAncestor.get(vertex), tarjanAncestor.get(i)));
	                } 
	                else if (tarjanInStack.get(i)) 
	                {
	                   tarjanAncestor.set(vertex, Math.min(tarjanAncestor.get(vertex), tarjanIndex.get(i)));
	                }
        	    }
            }
           
            if (tarjanAncestor.get(vertex).equals(tarjanIndex.get(vertex)) && hasVertex(vertex)) 
            {
                if (byNumber) 
                {
                    int u = -1;
                    do {
                        u = tarjanStack.peek();//top
                        sccsByNum.set(u, currentComponent);
                        tarjanStack.pop();
                        tarjanInStack.set(u, false);
                    } while (u != vertex);
                    currentComponent++;
                } 
                else
                {
                    ArrayList<Integer> s = new ArrayList<Integer>();
                    int u = -1;
                    do {
                        u = tarjanStack.peek();
                        s.add(u);
                        tarjanStack.pop();
                        tarjanInStack.set(u, false);
                    } 
                    while (u != vertex);
                    this.sccs.add(s);
                }
            }
        }

		/*
         * Extracting the grounding kernel *
         */
        protected Graph groundingKernel() throws Exception { 
           Graph h = this.makeCopy();
           ArrayList<Integer> out0Vertices;
           
           do {
                out0Vertices = h.out0();
                h.deleteVertices(out0Vertices); 
           } 
           while (out0Vertices.size() != 0);
           
           return h;
        }



        private Graph makeCopy() {
			Graph copy = new Graph(this.V);
			
			copy.outgoingNeighbors = new ArrayList< ArrayList<Integer> >();
			copy.incomingNeighbors = new ArrayList< ArrayList<Integer> >();
			
	        copy.vertices = new TreeSet<Integer>(this.vertices);
	        copy.hasVertex  = new ArrayList<Boolean>(this.hasVertex);                      
	        copy.numVertices = this.numVertices;
	        copy.numEdges = this.numEdges;
	        
	        for (int i = 0; i < this.V; i++) {
	        	copy.outgoingNeighbors.add(new ArrayList<Integer>(this.outgoingNeighbors.get(i)));
	        	copy.incomingNeighbors.add(new ArrayList<Integer>(this.incomingNeighbors.get(i)));
	        }
		    
		    
	        copy.tarjanCurrentIndex = this.tarjanCurrentIndex;
	        copy.tarjanIndex = new ArrayList<Integer>(this.tarjanIndex);
	        copy.tarjanAncestor = new ArrayList<Integer>(this.tarjanAncestor);
	        copy.tarjanStack = new Stack<Integer>();
	        copy.tarjanStack.addAll(this.tarjanStack);
	        copy.tarjanInStack = new ArrayList<Boolean>(this.tarjanInStack);
	        copy.currentComponent = this.currentComponent;
	        copy.sccsByNum = new ArrayList<Integer>(this.sccsByNum);
	        copy.sccs = new ArrayList< ArrayList<Integer> >();
	        
	        for (int i = 0; i < this.sccs.size(); i++) {
	        	copy.sccs.add(new ArrayList<Integer>(this.sccs.get(i)));
	        }
			
			return copy;
		}

		/*************************
         * Contraction operators 
         * @throws Exception *
         *************************/
        /*
         * Operator IN0 *
        */
        protected ArrayList<Integer> in0() throws Exception {
           ArrayList<Integer> in0Vertices = sources();
           //System.out.println(in0Vertices);
           if(!in0Vertices.isEmpty()) deleteVertices(in0Vertices);
           
           return in0Vertices;
       }

        /*
         * Operator OUT0 *
         */
       protected ArrayList<Integer> out0() throws Exception {
           ArrayList<Integer> out0Vertices = sinks();
           if(!out0Vertices.isEmpty()) deleteVertices(out0Vertices);
           
            return out0Vertices;
        }

        /*
         * Operator LOOP *
         */
       protected ArrayList<Integer> loop() throws Exception {
           ArrayList<Integer> loopVertices = loops();
           
           if(!loopVertices.isEmpty()) deleteVertices(loopVertices);
           
           return loopVertices;
        }

        /*
         * Operator IN1 *
       */
       protected ArrayList<Integer> in1() throws Exception {
            
    	   if (loops().size() == 0) 
           {
               ArrayList<Integer> in1Vertices = new ArrayList<Integer>();
               ArrayList<Integer> vs = new ArrayList<Integer>(vertices);
               
               Iterator<Integer> it = vs.iterator();
               while(it.hasNext()) {
            	   Integer i = it.next();
                    if (hasVertex(i) && inDegree(i) == 1 && !hasLoop(i)) {
                        in1Vertices.add(i);
                        mergeVertex(i);
                    }
                }
               
                return in1Vertices;
            } 
            
           return new ArrayList<Integer>();
            
        }

        /*
         * Operator OUT1 *
         */
       protected ArrayList<Integer> out1() throws Exception {
           
    	   if (loops().size() == 0) 
    	   {
              ArrayList<Integer> out1Vertices = new ArrayList<Integer>();
              ArrayList<Integer> vs = new ArrayList<Integer>(vertices);
               
              Iterator<Integer> it = vs.iterator();
              while(it.hasNext()) {
            	  Integer i = it.next();
                 if (hasVertex(i) && outDegree(i) == 1 && !hasLoop(i)) {
                        out1Vertices.add(i);
                        mergeVertex(i);
                  }
              }
              
              return out1Vertices;
            } 
            
    	   return new ArrayList<Integer>();
            
        }

        /*
         * Operator PIE 
         */
       protected ArrayList<Edge> acyclicEdges() throws Exception {
    	   
           ArrayList<Integer> vertexToSCC = vertexToStronglyConnectedComponentNumber();
           
           ArrayList<Edge> ae = new ArrayList<Edge>();
           ArrayList<Edge> allEdges = edges();
           Iterator<Edge> it = allEdges.iterator();
		   while(it.hasNext()) {
			   Edge edge = it.next();
			   
			      if (vertexToSCC.get(edge.source) != vertexToSCC.get(edge.target)) {
			            ae.add(edge);
			      }
				
			}
           
            return ae;
        }

       protected ArrayList<Edge> piEdges() throws Exception {
           ArrayList<Edge> es = new ArrayList<Edge>();
           ArrayList<Edge> allEdges = edges();
           
           Iterator<Edge> it = allEdges.iterator();
           while(it.hasNext()) {
        	   Edge e = it.next();
                if (hasEdge(e.target, e.source)) 
                {
                    es.add(e);
                }
           }
           
           return es;
        }

       protected ArrayList<Edge> pseudoAcyclicEdges() throws Exception {
           Graph h = this.makeCopy();
           ArrayList<Edge> pies = piEdges();
          
           h.deleteEdges(acyclicEdges());
           h.deleteEdges(pies);    
           
           return h.acyclicEdges();
        }

       protected ArrayList<Edge> pie() throws Exception {
            if (loops().size() == 0) 
            {
               ArrayList<Edge> aes = acyclicEdges();
               ArrayList<Edge> paes = pseudoAcyclicEdges();
               deleteEdges(aes);
               deleteEdges(paes);
               ArrayList<Edge> es = aes;
               
               Iterator<Edge> it = paes.iterator();
               while(it.hasNext()) {
                    es.add(it.next());
               }
               
               return es;
            } 
            
            return new ArrayList<Edge>();
            
        }

        /*
         * Operator CORE *
         */

       protected boolean isPiVertex(int vertex) throws Exception {
           ArrayList<Integer> outN = getOutgoingNeighbors(vertex);
           boolean valid = true;
           Iterator<Integer> it = outN.iterator();
           
           while (valid && it.hasNext()) {
        	   Integer i = it.next();
               if(i != outN.get(outN.size()-1)) {
            	   valid = hasEdge(i, vertex);
               }
               else break;
            }
           
            return valid;
        }

        protected ArrayList<Integer> piVertices() throws Exception {
           ArrayList<Integer> vs = new ArrayList<Integer>();
           
           Iterator<Integer> it = vertices.iterator();
           while(it.hasNext()) {
        	   Integer i = it.next();
               if (isPiVertex(i)) vs.add(i);
           }
           
           return vs;
        }

       protected boolean isClique(ArrayList<Integer> vs) throws Exception {
           boolean clique = true;
           Iterator<Integer> it = vs.iterator();
           
           do {
               Iterator<Integer> jt = vs.iterator();
               do {
            	   Integer i = it.next();
            	   Integer j = jt.next();
                   clique = (i == j && !hasEdge(i, j)) || (i != j && hasEdge(i, j));
               } 
               while (clique && jt.next() != vs.get(vs.size()-1));
               it.hasNext();
           } 
           while (clique && it.next() != vs.get(vs.size()-1));
           
           return clique;
        }

        /*
         * Core step
         */
        protected ArrayList<Integer> core() throws Exception {
            if (loops().size() == 0) {
               ArrayList<Integer> piVs = piVertices();
               
               Iterator<Integer> it = piVs.iterator(); 
               while(it.hasNext()) {
            	   Integer k = it.next();
                   ArrayList<Integer> potentialClique = getOutgoingNeighbors(k);
                    potentialClique.add(k);
                    
                    if (isClique(potentialClique)) 
                    {
                        deleteVertices(potentialClique);
                        potentialClique.remove(potentialClique.size()-1);
                        return potentialClique;
                    }
                }
            }
            return new ArrayList<Integer>();
        }

        /*
         * Operator DOME 
         */
       protected ArrayList<Integer> piPredecessors(int vertex) throws Exception {
           ArrayList<Integer> piPreds = new ArrayList<Integer>();
           ArrayList<Integer> preds = incomingNeighbors(vertex);
           
           Iterator<Integer> it = preds.iterator();
           while(it.hasNext()) 
           {
        	   Integer i = it.next();
                if (hasEdge(vertex, i)) piPreds.add(i);
           }
           
           return piPreds;
        }

       protected ArrayList<Integer> piSuccessors(int vertex) throws Exception {
		   ArrayList<Integer> piSucc = new ArrayList<Integer>();
		   ArrayList<Integer> succ = getOutgoingNeighbors(vertex);
		   
		   Iterator<Integer> it = succ.iterator();
		   while(it.hasNext()) {
			   Integer i = it.next();
		       if (hasEdge(i, vertex)) piSucc.add(i);
		   }
           
		   return piSucc;
        }

       protected ArrayList<Integer> nonPiPredecessors(int vertex) throws Exception{
           ArrayList<Integer> nonPiPreds = new ArrayList<Integer>();
           ArrayList<Integer> preds = incomingNeighbors(vertex);
   
           Iterator<Integer> it = preds.iterator();
		   while(it.hasNext()) {
			   Integer i = it.next();
                if (!hasEdge(vertex, i)) nonPiPreds.add(i);
		   }
           
		   return nonPiPreds;
        }

       protected ArrayList<Integer> nonPiSuccessors(int vertex) throws Exception {
           ArrayList<Integer> nonPiSucc = new ArrayList<Integer>();
           ArrayList<Integer> succ = getOutgoingNeighbors(vertex);
           Iterator<Integer> it = succ.iterator();
		   
           while(it.hasNext()) {
        	   Integer i = it.next();
                if (!hasEdge(i, vertex)) nonPiSucc.add(i);
		   }
           return nonPiSucc;
        }

       protected boolean isDominated(int source, int target) throws Exception {
            if (!hasEdge(source, target)) 
            {
            	return false;
            }
            else if (hasEdge(source, target) && hasEdge(target, source)) 
            {
            	return false;
            }
            else 
            {
               ArrayList<Integer> pTarget = incomingNeighbors(target);
               ArrayList<Integer> pSource = nonPiPredecessors(source);
               
               pTarget.sort((a, b) -> a - b);
               pSource.sort((a, b) -> a - b);
               
               
               if(pTarget.addAll(pTarget)) 
               {
                    return true;
               }
               
               ArrayList<Integer> sSource = getOutgoingNeighbors(source);
               ArrayList<Integer> sTarget = nonPiSuccessors(target);
                
                sTarget.sort((a, b) -> a - b);
                sSource.sort((a, b) -> a - b);
                
                if (sSource.addAll(sTarget))
                    return true;
                else
                    return false;
            }
        }

        protected ArrayList<Edge> dominatedEdges() throws Exception {
           ArrayList<Edge> allEdges = edges();
           ArrayList<Edge> des = new ArrayList<Edge>();
           
           Iterator<Edge> it = allEdges.iterator();
           while(it.hasNext()) {
        	   Edge e = it.next();
               if (isDominated(e.source, e.target)) des.add(e);
           }
           
           return des;
        }

       protected ArrayList<Edge> dome() throws Exception {
            if (loops().size() == 0) 
            {
               ArrayList<Edge> des = dominatedEdges();
                deleteEdges(des);
                return des;
            } 
            
            return new ArrayList<Edge>();
        }

        /*
         * Reduction *
       */
       protected ArrayList<Integer> reduce(boolean applyIn0,  boolean applyOut0, boolean applyLoop,
    		   boolean applyIn1,  boolean applyOut1, boolean applyPie,
    		   boolean applyCore, boolean applyDome, boolean verbose, ArrayList<Integer> solution) throws Exception {
          
    	   	if( !hasVertex.contains(true) && ( numEdges <= 0 || numVertices <= 0) ) return new ArrayList<Integer>();
    	   	
            if (applyIn0) {
            
               ArrayList<Integer> in0Vertices = in0();
                if (verbose) {
                	System.out.println("IN0  : " +in0Vertices+" vertex(vertices) has(have) been removed.");
                }
                
            }
            if (applyOut0) {
            	
               ArrayList<Integer> out0Vertices = out0();
                if (verbose) {
                	System.out.println("OUT0 : "+out0Vertices+" vertex(vertices) has(have) been removed.");
                }
            }
            
            if (applyIn1) {
            	
               ArrayList<Integer> in1Vertices = in1();
                if (verbose) {
                	System.out.println("IN1  : "+ in1Vertices+" vertex(vertices) has(have) been merged.");
                }
            }
            if (applyOut1) 
            {
               ArrayList<Integer> out1Vertices = out1();
                if (verbose) {
                	System.out.println("OUT1 : " +out1Vertices+" vertex(vertices) has(have) been merged.");
                }
            }
            if (applyLoop) {
            	
                ArrayList<Integer> loopVertices = loop();
          
                 Iterator<Integer> it = loopVertices.iterator();
                 while(it.hasNext()) {
                  	solution.add(it.next());
                 }

                 strongVertices.addAll(solution);
                 if (verbose) {
                 	System.out.println("LOOP : " +loopVertices+ " vertex(vertices) has(have) been removed.");
                 }
             }
            if (applyPie) 
            {
            	ArrayList<Edge> pieEdges = pie();
               	//for(int i = 0; i < pieEdges.size(); i++) System.out.println(" "+pieEdges.get(i).source+" "+ pieEdges.get(i).target);
               	
                if (verbose) {
                	System.out.println("PIE  : " +pieEdges.size()+" edge(s) has(have) been removed." );
                }
            	reduce(true, true, true, true, true, false, false, false, verbose, solution);
        		//System.out.println("*****************************************************");
            }
            if (applyCore) 
            {
            	ArrayList<Integer> coreVertices = core();
            	Iterator<Integer> it = coreVertices.iterator();
            	while(it.hasNext()) {
                	solution.add(it.next());
            	}
                
            	if (verbose) {
            	   	System.out.println( "CORE : " +coreVertices.size()+" vertex(vertices) has(have) been removed.");
            	}
           		reduce(true, true, true, true, true, false, false, false, verbose, solution);
        		//System.out.println("*****************************************************");
            }
            if (applyDome) 
            {
               ArrayList<Edge> domeEdges = dome();
                if (verbose) {
                	System.out.println( "DOME : " +domeEdges.size()+" edge(s) has(have) been removed.");
                }
            	reduce(true, true, true, true, true, false, false, false, verbose, solution);
        		//System.out.println("*****************************************************");
            }
            
            return solution;
        }

       protected ArrayList<Integer> reduce(boolean verbose) throws Exception {
           int n = 0, m = 0;
           ArrayList<Integer> solution = new ArrayList<Integer>();
           
           do {
   
              n = numberOfVertices();
              m = numberOfEdges();
              ArrayList<Integer> partialSolution = new ArrayList<Integer>();
              partialSolution = reduce(false, false, false, false, false, true, true, true, verbose, partialSolution);
              
              solution.addAll(partialSolution);              
              
            } while (n != numberOfVertices() || m != numberOfEdges());
            
           return solution;
        }

        /*
         * Cycles and feedback vertex sets *
         */
       protected ArrayList<Integer> shortestCycle() throws Exception {
            Graph h = this.makeCopy();
            
            if (h.isAcyclic()) {
                throw new Exception("No shortest cycle ! This graph is acyclic");
            } 
            else 
            {
                int n = 0;
                do {
                    n = h.numberOfVertices();
                    h.in0();
                    h.out0();
                } while (n != h.numberOfVertices());
               
                ArrayList<Integer> shortestCycle = new ArrayList<Integer>();
                ArrayList<Integer> currentCycle = new ArrayList<Integer>();
                
			   int maxLength = h.numberOfVertices();
			   Iterator<Integer> it = vertices().iterator();
			   while(it.hasNext()) {
			        currentCycle = h.shortestCycle(it.next(), maxLength);
			        if ( (currentCycle.size() - 1)  < maxLength) 
			        {
			            shortestCycle = currentCycle;
			            maxLength = currentCycle.size() - 1;
			        }
			    }
                
                return shortestCycle;
            }
        }

        /**
         * Returns the shortest cycle starting from the given vertex
         * if its length does not exceed the given maximum length.
         *
         * @param vertex     the vertex from which the cycle must start
         * @param maxLength  the maximum allowed length for the cycle
         * @return           aArrayList containing in the right order the
         *                   vertices that form the shortest cycle
         * @throws Exception 
         */
       protected ArrayList<Integer> shortestCycle(int vertex, int maxLength) throws Exception {
            
    	   LinkedList<LinkedList<Integer>> paths = new LinkedList<LinkedList<Integer>>();
    	   LinkedList<Integer> path = new LinkedList<Integer>();
            
            path.add(vertex);
            paths.push(path);
            boolean cycleFound = false;
            
            while (!cycleFound && paths != null) {
		        path = paths.peekFirst();
		        paths.pop();
		        
		        cycleFound = path.size() > 1 && path.peekFirst() == path.peekLast();
		        
		        if (!cycleFound && (path.size() - 1) < maxLength) {
		           ArrayList<Integer> outN = getOutgoingNeighbors(path.peekLast());
		           
		           Iterator<Integer> it = outN.iterator();
		           while(it.hasNext()){
		                path.add(it.next());
		                paths.push(path);
		                path.remove(path.size()-1);
		            }
		        }
          }
            
          ArrayList<Integer> cycle = new ArrayList<Integer>(path);
           
          return cycle;
        }

       public boolean isAcyclic() throws Exception {
            return loops().size() == 0 && stronglyConnectedComponents().size() == numberOfVertices();
        }

        protected boolean isFeedbackVertexSet(ArrayList<Integer> fvs) throws Exception {
            Graph h = this.makeCopy();
            h.deleteVertices(fvs);
            return h.isAcyclic();
        }

        /*
         * Upper bound algorithms *
         */
        protected int upperBoundValue(boolean verbose) throws Exception {
            return getUpperBoundSolution(verbose).size();
        }

        protected ArrayList<Integer> getUpperBoundSolution(boolean verbose) throws Exception {
           ArrayList<Integer> solution = new ArrayList<Integer>();
           Graph h = this.makeCopy();
           
           while (h.numberOfVertices() > 0) {
                if (verbose) 
                {
                    System.out.println();
                    h.print(false);
                }
                
                ArrayList<Integer> partialSolution = h.reduce(verbose);
                Iterator<Integer> it = partialSolution.iterator();
                while(it.hasNext()) {
                    solution.add(it.next());
                }
                
                if (h.numberOfVertices() > 0) 
                {
                    int v = h.minDegreeVertex();
                    if (v == -1) 
                    {
                        h.print(true);
                        System.out.println();
                    }
                    
                    h.mergeVertex(v);
                }
            }
            return solution;
        }

        /*
         * Minimum feedback vertex sets *
        */
       protected ArrayList<Integer> minimumFeedbackVertexSet(boolean verbose) throws Exception {
           
    	   Graph h = this.makeCopy();   
           ArrayList<Integer> vs = new ArrayList<Integer>(h.vertices);
           
           return h.mfvs(new ArrayList<Integer>(), vs, 0, 0, true, verbose);
       }
       
       protected int getUpperBoundValue(boolean verbose) throws Exception {
    	    return getUpperBoundSolution(verbose).size();
       }
       
       protected int getLowerBoundValue(boolean verbose) throws Exception {
    	    int lb = 0;
    	    Graph h = this.makeCopy();
    	    while (h.numberOfVertices() > 0) {
    	        if (verbose) 
    	        {
    	            System.out.println();
    	            h.print(false);
    	            System.out.println();
    	        }
    	        lb += h.reduce(verbose).size();
    	        if (h.numberOfVertices() > 0) {
    	            ArrayList<Integer> shortestCycle = h.shortestCycle();
    	            h.deleteVertices(shortestCycle);
    	            lb += 1;
    	        }
    	    }
    	    
    	    return lb;
    	}

       private ArrayList<Integer> mfvs(ArrayList<Integer> solution, ArrayList<Integer> bestSolution, int lowerBound,
                                 int level, boolean reducible, boolean verbose) throws Exception {
            Graph h = this.makeCopy();
            ArrayList<Integer> partialSolution = new  ArrayList<Integer>();
            
            if (reducible) 
            {
                // Reducing the graph
                partialSolution = h.reduce(verbose);
                Iterator<Integer> it = partialSolution.iterator();
                while(it.hasNext()) 
                {
                    solution.add(it.next());
                }
               
                // Partitioning according to strongly connected components
               ArrayList<ArrayList<Integer> > s = h.stronglyConnectedComponents();
               if ( s.size() > 1) 
               {
            	   Iterator< ArrayList<Integer> > itr = s.iterator();
                   while(itr.hasNext()) {
                	   ArrayList<Integer> temp = itr.next();
                       Graph scc = h.subgraph(temp);            
                       ArrayList<Integer> sccSolution = scc.mfvs(new ArrayList<Integer>(), temp, 0, 0, false, verbose);
                       solution.addAll(sccSolution);
                    }
                    return solution;
                }
            }
            if (verbose) 
            {
                System.out.println("Level : " + level);
                h.print(false);

                System.out.println("u = " + h.getUpperBoundValue(true));
                System.out.println("l = " + h.getLowerBoundValue(true));
            }
            
            // Lower bound and initial solution
            int lb = h.getLowerBoundValue(false);
            if (level == 0) 
            {
                lowerBound = lb;
                bestSolution = h.getUpperBoundSolution(false);
               
                Iterator<Integer> it = solution.iterator();
                while(it.hasNext()) 
                {
                    bestSolution.add(it.next());
                }
            }
            
            // Bounding the search
            if (h.numberOfVertices() == 0) 
            {
            	return solution;
            }
            else if (solution.size() + lb > bestSolution.size()) 
            {
            	return bestSolution;
            }
            
            // Branching
            int v = h.maxDegreeVertex();
            Graph leftGraph = h;
            leftGraph.deleteVertex(v);
            solution.add(v);
            ArrayList<Integer> leftSolution = leftGraph.mfvs(solution, bestSolution, lowerBound, level + 1, true, verbose);
           
            if (leftSolution.size() < bestSolution.size()) 
            {
            	bestSolution = leftSolution;
            }
            
            // Rebounding the search
            if (lb == bestSolution.size()) 
            {
            	return bestSolution;
            }
            
            Graph rightGraph = h.makeCopy();
            rightGraph.mergeVertex(v);
            solution.remove(solution.size()-1);
            ArrayList<Integer> rightSolution = rightGraph.mfvs(solution, bestSolution, lowerBound, level + 1, true, verbose);
            
            if (rightSolution.size() < bestSolution.size()) 
            {
            	bestSolution = rightSolution;
            }
            
            return bestSolution;
        }
    }
}
