package flexgridsim.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import flexgridsim.Flow;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VonControlPlane;
import flexgridsim.rl.GridState;
import flexgridsim.rl.ReinforcementLearningWorld.ShapedPlanRF;
import flexgridsim.rsa.VONRCSA;
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
			
			ArrayList<Integer> t = new ArrayList<>();
			
			for (int i : flows.get(key).getLinks()) {
				t.add(i);
			}
			
			if(t.contains(targetLink))
			{
				PhysicalTopology temp = new PhysicalTopology(pt);
				boolean accept = redirectingLightpath(flows.get(key), temp, cp);
				
				if(!accept) continue;
				
				double current = (double)pt.getLink(targetLink).getNumFreeSlots()/(double)(pt.getCores() * pt.getNumSlots());
				if(current >= meanAvailableSlots) 
				{
				
					ShapedPlanRF.updateValue(new GridState(4,1), "right", 1);
					pt.updateEverything(temp);
					
					return;
				}
			}
			
			
		}
		
		ShapedPlanRF.updateValue(new GridState(4,1), "right", -1);
		
	}

	private boolean redirectingLightpath(Flow flow, PhysicalTopology pt, VonControlPlane cp) {
		
		pt.getNode(flow.getSource()).updateTransponders(1);
		pt.getNode(flow.getDestination()).updateTransponders(1);
		
		int []links = flow.getLinks();
		for (int j = 0; j < links.length; j++) {
    		
            pt.getLink(links[j]).releaseSlots(flow.getSlotList());
            pt.getLink(links[j]).updateNoise(flow.getSlotList(), flow.getModulationLevel());
            
        }
		
		int source =  pt.getLink(links[0]).getSource();
        pt.getNode(source).setComputeResources(flow.getComputingResource());
        
		int destination =  pt.getLink(links[links.length-1]).getSource();
        pt.getNode(destination).setComputeResources(flow.getComputingResource());
        
        for(int i: flow.getLinks()) {
			pt.getLink(i).updateCrosstalk();
		}
        
		org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge> ksp = new org.jgrapht.alg.shortestpath.KShortestPaths<Integer, DefaultWeightedEdge>(pt.getGraph(), 5);
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
				r += (double)pt.getLink(link).getNumFreeSlots()/(double)(pt.getCores() * pt.getNumSlots());
			}
			
			r = (r/(double)paths.get(i).length)*100.0;
			ratio[i] = (int) r;
		}
		
		indices.sort((a,b) -> ratio[a] - ratio[b]);
		
		RedirectingRSA rsa = new RedirectingRSA(pt, cp);
		rsa.paths = paths;
		rsa.indices = indices;
		flow.setAccepeted(false);
		
		rsa.flowArrival(flow);
        
		return flow.isAccepeted();
	}

	protected class RedirectingRSA extends VONRCSA {
		
		public ArrayList<Integer> indices;
		
		public RedirectingRSA(PhysicalTopology pt, VonControlPlane cp) {
			super.setPhysicalTopology(pt);
			super.setVonControlPlane(cp);
		
		}
		
		public void flowArrival(Flow flow) {

			if(pt.getNode(flow.getSource()).getTransponders() <= 0 || pt.getNode(flow.getDestination()).getTransponders() <= 0) {
				
				return;
			}
			
			if(!paths.isEmpty()) 
			{
				int []modulationFormats = new int[paths.size()];
				ArrayList<ArrayList<Slot>> blockOfSLots = getBlockOfSlots(flow, modulationFormats);
				
				for(int i : indices) {
					
					int []links = paths.get(i);
					
					establishConnection(links, blockOfSLots.get(i), modulationFormats[i], flow);
					
					if(flow.isAccepeted()) {
						
						return;
					}
				}
				
			}
		}
	}
	

}
