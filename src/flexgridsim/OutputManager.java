package flexgridsim;

import org.w3c.dom.*;

import flexgridsim.graphs.Graph;
import flexgridsim.graphs.GraphNotFoundException;

/**
 * A class to generate out graphs (under development).
 * 
 * @author pedrom
 */
public class OutputManager {
	private Graph[] graphs;
	private int numberOfGraphs;

	/**
	 * Instantiates a new empty graph plotter.
	 *
	 * @param xml the xml element with the graphs to be plotted
	 */
	public OutputManager(Element xml) {
		NodeList graphlist = xml.getElementsByTagName("graph");
		numberOfGraphs = graphlist.getLength();
		graphs = new Graph[numberOfGraphs];
		for (int i = 0; i < numberOfGraphs; i++) {
			String dimension = ((Element) graphlist.item(i)).getAttribute("dimension");
			int dimensionInt = Integer.parseInt(!dimension.isEmpty() ? dimension : "2");
			graphs[i] = new Graph(
					((Element) graphlist.item(i)).getAttribute("name"),
					"graphs"+((Element) graphlist.item(i)).getAttribute("dots-file"), dimensionInt);
		}
	}
	
	/**
	 * Plot all graphs.
	 */
	public void writeAllToFiles(){
		for (Graph graph : graphs) {
			graph.writeDotsToFile();
		}
	}
	
	/**
	 * Adds the dot to graph.
	 *
	 * @param graphName the graph name
	 * @param value1 the value1
	 * @param value2 the value2
	 */
	public void addDotToGraph(String graphName, double value1, double value2){
		try {
			this.getGraph(graphName).getDataSet().addDot(value1, value2);
		} catch (GraphNotFoundException e) {
		}
	}
	
	/**
	 * Adds the dot to graph.
	 *
	 * @param graphName the graph name
	 * @param value1 the value1
	 * @param value2 the value2
	 */
	public void addDotToGraph(String graphName, double... values){
		try {
			this.getGraph(graphName).getDataSet().addDot(values);
		} catch (GraphNotFoundException e) {
		}
	}
	/**
	 * Gets a graph by its name.
	 *
	 * @param name of the graph
	 * @return the graph
	 * @throws GraphNotFoundException the graphs not found exception
	 */
	public Graph getGraph(String name) throws GraphNotFoundException {
		for (Graph g : graphs) {
			if (g.getName().equals(name)){
				return g;
			}
		}
		throw new GraphNotFoundException();
	}
	
}
