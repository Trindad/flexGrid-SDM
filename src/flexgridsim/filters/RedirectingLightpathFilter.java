package flexgridsim.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Database;
import flexgridsim.Flow;
import flexgridsim.ModulationsMuticore;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VonControlPlane;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;
import vne.VirtualNetworkEmbedding;

public class RedirectingLightpathFilter {
	
	private int targetLink;

	public RedirectingLightpathFilter(int id) {
		targetLink = id;
	}
	
	public void run(PhysicalTopology pt, VonControlPlane cp, VirtualNetworkEmbedding vne) {
		
		Map<Long, Flow> flows = cp.getActiveFlows();
		
		double meanAvailableSlots = 0;
		for(int i = 0; i < pt.getNumLinks(); i++) {
			meanAvailableSlots += (double)pt.getLink(targetLink).getNumFreeSlots()/(pt.getCores() * pt.getNumSlots());
		}
		
		meanAvailableSlots = meanAvailableSlots / (double)pt.getNumLinks();
		
		for(Long key : flows.keySet()) {
			
			if(Arrays.asList( flows.get(key).getLinks() ).contains(targetLink))
			{		
				boolean accept = redirectingLightpath(flows.get(key), pt);
				
				if(!accept) continue;
				
				double current = (double)pt.getLink(targetLink).getNumFreeSlots()/(double)(pt.getCores() * pt.getNumSlots());
				if(current >= meanAvailableSlots) 
				{
					ShapedPlanRF.updateValue(new GridState(4,1), "right", 1);
					
					return;
				}
			}
			
		}
		
		ShapedPlanRF.updateValue(new GridState(4,1), "right", -1);
		
	}

	private boolean redirectingLightpath(Flow flow, PhysicalTopology pt) {
		
		PhysicalTopology temp = new PhysicalTopology(pt);
		
		temp.getNode(flow.getSource()).updateTransponders(1);
		temp.getNode(flow.getDestination()).updateTransponders(1);
		
		int []links = flow.getLinks();
		for (int j = 0; j < links.length; j++) {
    		
            temp.getLink(links[j]).releaseSlots(flow.getSlotList());
            temp.getLink(links[j]).updateNoise(flow.getSlotList(), flow.getModulationLevel());
            
        }
		
		int source =  temp.getLink(links[0]).getSource();
        temp.getNode(source).setComputeResources(flow.getComputingResource());
        
		int destination =  temp.getLink(links[links.length-1]).getSource();
        temp.getNode(destination).setComputeResources(flow.getComputingResource());
        
        for(int i: flow.getLinks()) {
			temp.getLink(i).updateCrosstalk();
		}
        
		org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> ksp = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(temp.getGraph(), 5);
		List< GraphPath<Integer, DefaultWeightedEdge> > p = ksp.getPaths( flow.getSource(), flow.getDestination() );
		
		if(p == null) return false;
		
		ArrayList<int[]> paths = new ArrayList<>();
		for (int k = 0; k < p.size(); k++) {
			
			if(p.contains(targetLink)) continue;
			
			List<Integer> listOfVertices = p.get(k).getVertexList();
			int[] l = new int[listOfVertices.size()-1];
			
			for (int j = 0; j < listOfVertices.size()-1; j++) {
				
				l[j] = pt.getLink(listOfVertices.get(j), listOfVertices.get(j+1)).getID();
			}

			paths.add(links);
		}
		
		int []ratio = new int[paths.size()];
		ArrayList<Integer> indices = new ArrayList<>();
		
		for(int i = 0; i < paths.size(); i++) {
			indices.add(i);
			double r = 0;
			for(int link : paths.get(i)) {
				r += (double)temp.getLink(link).getNumFreeSlots()/(double)(temp.getCores() * temp.getNumSlots());
			}
			
			r = (r/(double)paths.get(i).length)*100.0;
			ratio[i] = (int) r;
		}
		
		indices.sort((a,b) -> ratio[a] - ratio[b]);
		
		
		for(int i : indices) {
			
			links = paths.get(i);
			
			RSA rcsa = new MyRCSA();
			if(fitConnection(flow, bitMapAll(links, temp), links)) {
				
				return true;
			}
		}
        
		return false;
	}

	
	

}
