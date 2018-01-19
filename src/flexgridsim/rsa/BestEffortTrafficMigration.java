package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;
import flexgridsim.util.MinimumFeedbackVertexSet;

public class BestEffortTrafficMigration {

	private Map<Long, Flow> flows;
	private Map<Flow, LightPath> mappedFlows;
	private ControlPlaneForRSA cp;
	private PhysicalTopology pt;
	private VirtualTopology vt;
	
	
	public BestEffortTrafficMigration(ControlPlaneForRSA cp, PhysicalTopology pt, VirtualTopology vt,
			Map<Long, Flow> flowsIndex, Map<Flow, LightPath> flowsAccepted) {
		
		this.flows = flowsIndex;
		this.mappedFlows = flowsAccepted;
		this.vt = vt;
		this.cp = cp;
		this.pt = pt;
	}

	public Map<Long, Flow> runBestEffort() {
		
//		Map<Long, Flow> lastStatus = cp.getActiveFlows();

		MinimumFeedbackVertexSet mfvs = new MinimumFeedbackVertexSet(flows.size(), flows); //dependency graph
		
		
//		for(Long key: flows.keySet()) {
//			
//			if(!flows.get(key).isConnectionDisruption()) {
//				
//				for(Long k: flows.keySet()) {
//					
//					if(key != k)
//					{
//						if(this.dependency(flows.get(key), lastStatus.get(k)))
//						{
//							mfvs.getGraph().addEdge(mfvs.getGraph(), mfvs.getNodeIndex(flows.get(key).getID()),  mfvs.getNodeIndex(lastStatus.get(k).getID()));
//						}
//					}
//					
//				}
//			}
//		}
//		
//		//It is not a DAG
//		if(mfvs.getGraph().isCyclic()) {
//			
//			System.out.println("There is a cycle");
//		}
//		
//		Map<Long, Flow> v = new HashMap<Long, Flow>();
//		
//		while(mfvs.getGraph().V >= 1)
//		{
//			LinkedList<Integer> adjListArray[] = mfvs.getGraph().getAdjacentListArray() ;
//			
//			for(LinkedList<Integer> it: adjListArray) {
//				
//				int i = it.element();
//				if(mfvs.getGraph().getOutDegree(mfvs.getGraph(), i) == 0)
//				{
//					v.put((long) i, flows.get((long)i));
//					mfvs.getGraph().deleteNode(mfvs.getGraph(), i);
//				}
//			}
//		}
		
		
//		return v;
		
		return flows;
	}
	
	//There is intersection between paths;
	private boolean intersectionBetweenPaths(int[] l1, int[] l2) {
		
		if(l1.equals(l2)) return true;
		
		int n = l1.length < l2.length ? l1.length : l2.length;
		
		for(int i = 0; i < n; i++) {
			
			if(l1[i] == l2[i]) 
			{
				return true;
			}	
		}
		
		return false;
	}
	
	
	private int tensorProduct(ArrayList<Slot> sl1, ArrayList<Slot> sl2) {
		
		int []s1 = new int[pt.getNumSlots()];
		int []s2 = new int[pt.getNumSlots()];
		
		for(int i = 0; i < s1.length; i++) {
			s1[i] = s2[i] = 0;
		}
		
		for(int i = 0; i < sl1.size(); i++) s1[sl1.get(i).s] = 1;
		for(int i = 0; i < sl2.size(); i++) s1[sl2.get(i).s] = 1;
		
		int result = 0;
		for(int i = 0; i < s1.length; i++) {
			result += (s1[i]  * s2[i]);
		}
		
		
		return result;
	}

	/**
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 */
	private boolean dependency(Flow f1, Flow f2) {
		
		int []l1 = f1.getLinks();
		int []l2 = f2.getLinks();
		
		if(intersectionBetweenPaths(l1, l2)) 
		{
			ArrayList<Slot> sl1 = f1.getSlotList(); 
			ArrayList<Slot> sl2 = f1.getSlotList(); 
			
			if(sl1.get(0).c == sl2.get(0).c)
			{
				if(this.tensorProduct(sl1, sl2) >= 1) 
				{
						return true;
				}
				
			}
		}
		
		return false;
	}
	
	

}
