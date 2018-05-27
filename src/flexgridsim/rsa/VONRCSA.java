package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.w3c.dom.Element;

import com.sun.javafx.fxml.expression.BinaryExpression;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.ModulationsMuticore;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.WeightedGraph;

/**
 * 
 * @author trindade
 *
 */

public class VONRCSA implements RSA{

	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp,
			TrafficGenerator traffic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flowArrival(Flow flow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flowDeparture(Flow flow) {
		// TODO Auto-generated method stub
		
	}

}
