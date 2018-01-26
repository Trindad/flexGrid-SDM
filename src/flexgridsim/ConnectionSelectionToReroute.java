package flexgridsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConnectionSelectionToReroute {
	
	private Map<Long, Flow> connectionsToReroute;
	private int nConnections;
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

	public Map<Long, Flow> getConnectionsToReroute() {
		
		if(strategy.equals("MFUSF"))
		{
			MFUSF selection = new MFUSF(nConnections);
			connectionsToReroute = selection.run(cp, pt, vt);
		}
		else 
		{
			HUSIF selection = new HUSIF(nConnections);
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
					return b.c - a.c;
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
				 
				 if(flows.get(key).getRate() <= 12 || (flows.get(key).getRate() == 125 && flows.get(key).getModulationLevel() >= 4) ) continue;
				 
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
			Map<Long, Flow> flowWithHighCore = new HashMap<Long, Flow>();
			
			int limit = (pt.getCores()-1)/2;
			
			for(Long key: flows.keySet()) {
				
//				if(flows.get(key).getRate() >= 13) {
					flowWithHighCore.put(key, flows.get(key));
//				}
			}
			
			for(int i = 0; i < pt.getCores(); i++) {
				
				for(int j = 0; j < pt.getNumSlots(); j++) {
					
					for(Long key: flowWithHighCore.keySet()) {
						
						Slot s = new Slot(i,  j);
						if(containsSlot( flowWithHighCore.get(key).getSlotList(), s ) && !slots.contains(s))
						{
							slots.add(s);
						}
					}
					
				}
			}
//			
//			slots.sort((a,b) -> {
//				int fb = slotIndex(b);
//				int fa = slotIndex(a);
////				System.out.println(fa + " * "+fb+" "+a.c+" "+b.c);
//				if (fb == fa) {
//					return b.c - a.c;
//				} else 
//					if (fb > fa) {
//					return 1;
//				} else {
//					return -1;
//				}
//			});
			
//			slots.sort((a, b) -> b.c - a.c);
			slots.sort((a, b) -> b.s - a.s);
			
//			slots.sort((a, b) -> {
//				
//				if(a.c == b.c) {
//					return b.s - a.s;
//				}
//				
//				return b.c - a.c;
//			});
			
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
}
