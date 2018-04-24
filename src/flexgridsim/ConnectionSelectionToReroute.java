package flexgridsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

public class ConnectionSelectionToReroute {
	
	private Map<Long, Flow> connectionsToReroute;
	private int nConnections;
	private double fi[];
	private String strategy;
	private ControlPlane cp;
	private PhysicalTopology pt;
	private VirtualTopology vt;
	
	public ConnectionSelectionToReroute(int n, String strategy, ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
		
		this.connectionsToReroute = new HashMap<Long, Flow>(); 
		this.nConnections = n;
		this.strategy = strategy;
		this.cp = cp;
		this.vt = vt;
		this.pt = pt;
	}
	
	public void setFragmentationIndexForEachLink(double fi[]) {
		this.fi = fi;
	}

	public Map<Long, Flow> getConnectionsToReroute() {
		
		if(strategy.equals("MFUSF"))
		{
			MFUSF selection = new MFUSF(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else if(strategy.equals("HUSIF"))
		{
			HUSIF selection = new HUSIF(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else if(strategy.equals("LargestRate"))
		{
			LargestRate selection = new LargestRate(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else if(strategy.equals("SmallestConnections"))
		{
			SmallestConnections selection = new SmallestConnections(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else if(strategy.equals("RandomConnections")) {
			
			RandomConnections selection = new RandomConnections(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else if(strategy.equals("ConnectionsInBottleneckLink")) {
			
			ConnectionsInBottleneckLink selection = new ConnectionsInBottleneckLink(nConnections, fi);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		
		return connectionsToReroute;
	}
	
	/**
	 * Most Frequency Used Slot First Connection Selection
	 * @author trindade
	 *
	 */
	private class MFUSF {

		int nConnections = 0;
		
		public MFUSF(int n) {
			nConnections = n;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			Map<Long, Flow> connections = new HashMap<Long, Flow>();

			int k = 0;
			
			ArrayList<Slot> slotFrequencies = this.calculateUsageFrequency(cp.getActiveFlows());
			
			for(Slot s: slotFrequencies) {
					
				Map<Long, Flow> temp = this.getherAllConnectionWithSlot(cp.getActiveFlows(), s);
				
				for (Long key: temp.keySet()) {
					
					if(k < nConnections && !connections.containsKey(key)) {
						
						connections.put(key, temp.get(key));
						k++;
					}
					
					if(k >= nConnections) {
						
						return connections;
					}
				}
				
			}
			
			return connections;
		}

		private Map<Long, Flow> getherAllConnectionWithSlot(Map<Long, Flow> flows, Slot slot) {
			
			Map<Long, Flow> c = new HashMap<Long, Flow>();
			
			for(Long key: flows.keySet()) {
				
				 ArrayList<Slot> slotList = flows.get(key).getSlotList();
				 
				 for(Slot s: slotList)
				 {
					 if(s.c == slot.c && s.s == slot.s) {
						 c.put(key, flows.get(key));
					 }
				 }
			}
			
			return c;
		}
		
		public int slotFrequency(Map<Long, Flow> flows, Slot slot) {
			
			int n = 0;
			
			for(Long key: flows.keySet()) 
			{
				for(int i = 0; i < flows.get(key).getSlotList().size(); i++) {
					
					int c = flows.get(key).getSlotList().get(i).c;
					int s = flows.get(key).getSlotList().get(i).s;
					if(slot.s == s && c == slot.c) {
						n++;
					}
				}
				
			}
			
			return n;
			
		}
		
		private ArrayList<Slot> calculateUsageFrequency(Map<Long, Flow> flows) {
			
			ArrayList<Slot> slots = new ArrayList<Slot>();
			Map<Long, Flow> flowWithHighCore = new HashMap<Long, Flow>();
			
			int limit = (pt.getCores()-1)/2;
			
			for(Long key: flows.keySet()) {
				
				if(flows.get(key).getSlotList().get(0).getC() < limit) {
					flowWithHighCore.put(key, flows.get(key));
				}
			}
			
			for(int i = 0; i < pt.getCores(); i++) {
				
				for(int j = 0; j < pt.getNumSlots(); j++) {
					
					slots.add( new Slot(i,  j));
				}
			}
			
			
			slots.sort((a,b) -> {
				int fb = slotFrequency(flowWithHighCore, b);
				int fa = slotFrequency(flowWithHighCore, a);
//				System.out.println(fa + " * "+fb);
				if (fb == fa) {
					return b.s - a.s;
				} else if (fb > fa) {
					return 1;
				} else {
					return -1;
				}
			});
			
			return slots;
		}
		
	}
	

	/**
	 * 
	 * Highest Used Slot-Index First
	 * @author trindade
	 *
	 */
	private class HUSIF {

		private int nConnections = 0;
		
		public HUSIF(int n) {
			nConnections = n;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			Map<Long, Flow> connections = new HashMap<Long, Flow>();
			Map<Long, Flow> allconnections = cp.getActiveFlows();

			int k = 0;
			
			ArrayList<Slot> HighestSlotIndex = this.calculateUsageFrequency(allconnections);
			
			for(Slot s: HighestSlotIndex) {
					
				Map<Long, Flow> temp = this.getherAllConnectionWithSlot(allconnections, s);
				
				for (Long key: temp.keySet()) {
					
					if(k < nConnections && !connections.containsKey(key)) {
						
						connections.put(key, temp.get(key));
						k++;
					}
					
					if(k >= nConnections) {
						
						return connections;
					}
				}
				
			}
			
			return connections;
		}

		private Map<Long, Flow> getherAllConnectionWithSlot(Map<Long, Flow> flows, Slot slot) {
			
			Map<Long, Flow> c = new HashMap<Long, Flow>();
			
			for(Long key: flows.keySet()) {
				
				 ArrayList<Slot> slotList = flows.get(key).getSlotList();
				 
				 if(flows.get(key).getRate() <= 12 || (flows.get(key).getRate() == 125 && flows.get(key).getModulationLevel() <= 2) ) continue;
				 
				 for(Slot s: slotList)
				 {
					 if(s.c == slot.c && s.s == slot.s) {
						 c.put(key, flows.get(key));
					 }
				 }
			}
			
			return c;
		}
		
		@SuppressWarnings("unused")
		private int slotIndex(Slot s) {
			return (s.s + ( pt.getCores() - s.c ) );
		}
		
		
		private ArrayList<Slot> calculateUsageFrequency(Map<Long, Flow> flows) {
			
			ArrayList<Slot> slots = new ArrayList<Slot>();
			
			for(int i = 0; i < pt.getCores(); i++) {
				
				for(int j = 0; j < pt.getNumSlots(); j++) {
					
					for(Long key: flows.keySet()) {
						
						Slot s = new Slot(i,  j);
						if(containsSlot( flows.get(key).getSlotList(), s ) && !slots.contains(s))
						{
							slots.add(s);
						}
					}
					
				}
			}

//			slots.sort((a, b) -> b.s - a.s);
			
			slots.sort( (a , b) -> {
				int diff = a.s - b.s;
				
				if(diff != 0) {
					return diff;
				}
				
				return ( a.c - b.c );
			});
			
			return slots;
		}

		private boolean containsSlot(ArrayList<Slot> slotList, Slot slot) {
			
			for(Slot s: slotList) {
				
				if(s.c == slot.c && s.s == slot.s) {
					return true;
				}
			}
			return false;
		}
		
	}

	private class LargestRate {
		
		private int nConnections = 0;
		
		public LargestRate(int n) {
			nConnections = n;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			Map<Long, Flow> connections = new HashMap<Long, Flow>();
			Map<Long, Flow> allconnections = cp.getActiveFlows();

			int k = 0;
		
			for (Long key: allconnections.keySet()) { 
				
				Flow flow = allconnections.get(key);
		
				 if(flow.getRate() <= 12 || (flow.getRate() <= 125 && flow.getModulationLevel() <= 2) ) continue;
				
				if(k < nConnections && !connections.containsKey(key)) 
				{
					connections.put(key, flow);
					k++;
				}
				
				if(k >= nConnections) {
					
					return connections;
				}
			}
			
			return connections;
		}
	}
	
	
	private class SmallestConnections {
		
		private int nConnections = 0;
		
		public SmallestConnections(int n) {
			nConnections = n;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			Map<Long, Flow> connections = new HashMap<Long, Flow>();
			Map<Long, Flow> allconnections = cp.getActiveFlows();

			int k = 0;
		
			for (Long key: allconnections.keySet()) { 
				
				Flow flow = allconnections.get(key);
		
				 if(allconnections.get(key).getRate() >= 100 ) continue;
				
				if(k < nConnections && !connections.containsKey(key)) 
				{
					connections.put(key, flow);
					k++;
				}
				
				if(k >= nConnections) {
					
					return connections;
				}
			}
			
			return connections;
		}
	}
	
	
	private class RandomConnections {
		
		private int nConnections = 0;
		
		public RandomConnections(int n) {
			nConnections = n;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			nConnections++;
			return null;
		}
	}

	

	private class ConnectionsInBottleneckLink {
		
		private int nConnections = 0;
		private double fi[];
		
		public ConnectionsInBottleneckLink(int n, double []f) {
			this.nConnections = n;
			this.fi = f;
		}
		
		public ArrayList<Long>getConnections(Map<Long, Flow> flows) {
			
//			ClosenessCentrality<Integer,DefaultWeightedEdge> cc = new ClosenessCentrality<Integer,DefaultWeightedEdge>(pt.getGraph());
			
			ArrayList<Double> sumLightpath = new ArrayList<Double>();
			ArrayList<Long> indices = new ArrayList<Long>();
			ArrayList<Integer> indicesOfIndices = new ArrayList<Integer>(); 
			
			for(Long key: flows.keySet()) {
				
				Flow flow = flows.get(key);
				
				int []links = flow.getLinks();
				double s = 0;
				for(int i : links) {
//					double cci = cc.getVertexScore(pt.getLink(i).getDestination()) + cc.getVertexScore(pt.getLink(i).getSource());
					s += (fi[i] * 100);
				}
				
				sumLightpath.add(s);
				indices.add(key);
				indicesOfIndices.add(indices.size()-1);
			}
//			System.out.println(Arrays.toString(sumLightpath.toArray()));
			
			indicesOfIndices.sort( (a , b) -> {
				if (sumLightpath.get(b) > sumLightpath.get(a)) {
					return 1;
				} else if (sumLightpath.get(b) < sumLightpath.get(a)) {
					return -1;
				} else {
					return 0;
				}
			});
			
			ArrayList<Long> sortedIndices = new ArrayList<Long>();
			
			for (Integer k : indicesOfIndices) {
				sortedIndices.add(indices.get(k));
			}
			
			return sortedIndices;
		}
		
		public Map<Long, Flow> run(ControlPlane cp, PhysicalTopology pt, VirtualTopology vt) {
			
			Map<Long, Flow> connections = new HashMap<Long, Flow>();
			Map<Long, Flow> flows = cp.getActiveFlows();

			int k = 0;
			
			ArrayList<Long> orderConnections = getConnections(flows);
		
			for (Long key: orderConnections) { 
				
				if(k < nConnections && !connections.containsKey(key) && flows.get(key).getRate() < 400) 
				{
					connections.put(key, flows.get(key));
					k++;
					
				}
				
				if(k >= nConnections) {
					
					return connections;
				}
			}
			
			return connections;
		}
	}
}
