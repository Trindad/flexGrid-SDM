package flexgridsim.rsa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.VirtualTopology;
import flexgridsim.util.MinimumFeedbackVertexSet;

public class BestEffortTrafficMigration {

	private Map<Long, Flow> flows;
	private Map<Long, Flow> allFlowsToReroute;
	private int nTrafficDisruption = 0;
	private ControlPlaneForRSA cp;
	private PhysicalTopology pt;
	private VirtualTopology vt;
	
	public BestEffortTrafficMigration(ControlPlaneForRSA cp, PhysicalTopology pt, VirtualTopology vt,
			Map<Long, Flow> f, Map<Long, Flow> all) {
		
		this.flows = f;
		this.vt = vt;
		this.cp = cp;
		this.pt = pt;
		this.allFlowsToReroute = all;
	}
	
	public int getNumberOfTrafficDisruption() {
		
		return this.nTrafficDisruption;
	}
	
	public MinimumFeedbackVertexSet constructDependencyGraph() {
		
		MinimumFeedbackVertexSet mfvs = new MinimumFeedbackVertexSet(flows); //dependency graph
		ArrayList< Long[] > temp = new ArrayList< Long[] >();
		
		for(Long key: flows.keySet()) {
			
			for(Long k: allFlowsToReroute.keySet()) {
				
				if( flows.get(key).getID() != allFlowsToReroute.get(k).getID() && this.isAccepted(flows, allFlowsToReroute.get(k).getID()) )
				{
					if(this.isNewEdge( flows.get(key).getID(),  allFlowsToReroute.get(k).getID(), temp)) {
						
						if(this.dependency(flows.get(key), allFlowsToReroute.get(k)) >= 1 ) {
							
							try {
								
								mfvs.getGraph().addEdge(mfvs.getNodeIndex(flows.get(key).getID()),  mfvs.getNodeIndex(allFlowsToReroute.get(k).getID()));
								temp.add(new Long[] { flows.get(key).getID(),  allFlowsToReroute.get(k).getID() });
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				
			}
		}
		
		return mfvs;
	}

	public Map<Long, Flow> runBestEffort() throws Exception {

		MinimumFeedbackVertexSet mfvs = constructDependencyGraph();
		
		if(mfvs.getGraph().numberOfVertices() >= 1 ) {
			
			ArrayList< ArrayList<Flow> > flowsToRestore = new ArrayList< ArrayList<Flow> >();
			if(!mfvs.getGraph().isAcyclic()) {
				mfvs.getGraph().print(true);
				System.out.println("There is a cycle");
				ArrayList<Flow> resultMFVS = mfvs.runMFVS();
				this.nTrafficDisruption = 0;
				
				flowsToRestore = moveToVacancy(resultMFVS);
				mfvs = constructDependencyGraph();//construct a new graph
			}
			
			
			Map<Long, Flow> newOrderFlows = new HashMap<Long, Flow>();
			
			while(mfvs.getGraph().numberOfVertices() >= 1)
			{
				for(Long key: flows.keySet()) {
					
					int u =  mfvs.getNodeIndex(flows.get(key).getID());
					
					if(mfvs.getGraph().hasVertex(u)) {
						
						ArrayList<Integer> out = mfvs.getGraph().getOutgoingNeighbors(u);
						
						if(out.size() == 0)
						{
							mfvs.getGraph().deleteVertex(u);
							newOrderFlows.put(key, flows.get(key));
						}
					}
					else
					{
						newOrderFlows.put(key, flows.get(key));
					}
					
				}
			}
			
			if(cp.isRerouting()) {
				cp.updateControlPlane(pt, vt, newOrderFlows);
			}
			
			restoreFlows(flowsToRestore);
			
			return new HashMap<Long, Flow>();
		}
		
		return flows;
	}

	private void restoreFlows(ArrayList<ArrayList<Flow>> flowsToRestore) {
		
		for(ArrayList<Flow> f: flowsToRestore) {
			
			cp.updateControlPlane(pt, vt, f.get(0));
		}
	}

	private boolean isNewEdge(long l, long b, ArrayList<Long[]> array) {
		
		for(int i = 0; i < array.size(); i++) {
			
			if( (array.get(i)[0] == l && array.get(i)[1] == b) || (array.get(i)[0] == b && array.get(i)[1] == l) ) {
				
				return false;
			}
		}
		
		return true;
	}

	private boolean isAccepted(Map<Long, Flow> f, long id) {
		
		for(Long key: f.keySet()) {
			
			if(f.get(key).getID() == id) {
				return !f.get(key).isConnectionDisruption();
			}
		}
		
		return false;
	}

	
	public void removeFlowFromPT(Flow flow, LightPath lightpath, PhysicalTopology ptTemp, VirtualTopology vtTemp) {

    	int[] links;
        links = lightpath.getLinks();
        
    	for (int j = 0; j < links.length; j++) {
    		ptTemp.getLink(links[j]).releaseSlots(lightpath.getSlotList());
    		ptTemp.getLink(links[j]).updateNoise(lightpath.getSlotList(), flow.getModulationLevel());
    		ptTemp.getLink(links[j]).updateCrosstalk(flow.getSlotList());
        }
    	
    	vtTemp.removeLightPath(lightpath.getID());
    }
	
	private ArrayList< ArrayList<Flow> >  moveToVacancy(ArrayList<Flow> flows) {
		
		ArrayList< ArrayList<Flow> > flowsToRestore = new ArrayList< ArrayList<Flow> >();
		
		for(Flow flow: flows) {
			
			SCVCRCSA rcsa = new SCVCRCSA();
			rcsa.simulationInterface(null, pt, vt, cp, null);
			
			ArrayList<Flow> tmp = new ArrayList<Flow>();
			Flow oldFlow = flow.copy();
			tmp.add(oldFlow);
			
			
			removeFlowFromPT(flow, vt.getLightpath(flow.getLightpathID()), pt, vt);
			rcsa.runRCSA(flow, oldFlow.getLinks(), oldFlow.getSlotList());
			
			tmp.add(flow);
			flowsToRestore.add(tmp);
		}
		
		return flowsToRestore;
	}
	
	/**
	 * There is intersection between paths
	 * @param l1
	 * @param l2
	 * @return
	 */
	private boolean intersectionBetweenPaths(int[] l1, int[] l2) {
	
		if(l1.equals(l2)) return true;

		 
		for(int i = 0; i < l1.length; i++) {
			
			for(int j = 0; j < l2.length; j++) {
				
				if(l1[i] == l2[j]) 
				{
					return true;
				}
			}	
		}
		
		return false;
	}
	
	/**
	 * Tensor product between two sets of slots
	 * @param sl1
	 * @param sl2
	 * @return
	 */
	private int tensorProduct(ArrayList<Slot> sl1, ArrayList<Slot> sl2) {
		
		int []s1 = new int[pt.getNumSlots()];
		int []s2 = new int[pt.getNumSlots()];
		
		
		
		for(int i = 0; i < pt.getNumSlots(); i++) {
			s1[i] = s2[i] = 0;
		}
		
		for(int i = 0; i < sl1.size(); i++) {
			
			s1[sl1.get(i).s] = 1;
		}
		
		for(int i = 0; i < sl2.size(); i++) {
			s2[sl2.get(i).s] = 1;
		}
		
		
		int result = 0;
		for(int i = 0; i < s1.length; i++) {
			result += (s1[i] * s2[i]);
		}
		
//		for(int i = 0; i < sl1.size(); i++) {
//			System.out.print(sl1.get(i).s);
//		}
//		
//		System.out.println();
//		for(int i = 0; i < sl2.size(); i++) {
//			System.out.print(sl2.get(i).s);
//		}
//		
//		System.out.println();

		return result;
	}

	/**
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 */
	private int dependency(Flow f1, Flow f2) {
		
		int []l1 = f1.getLinks();
		int []l2 = f2.getLinks();
		
		if(intersectionBetweenPaths(l1, l2)) 
		{
			ArrayList<Slot> sl1 = f1.getSlotList(); 
			ArrayList<Slot> sl2 = f2.getSlotList(); 
			
			if(sl1.get(0).c == sl2.get(0).c)
			{
				return this.tensorProduct(sl1, sl2);
			}
		}
		
		return 0;
	}
}
