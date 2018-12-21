package flexgridsim.von.mappers;

import flexgridsim.von.VirtualTopology;

public class MAPEMapper extends Mapper {

	public void vonArrival(VirtualTopology von) {
		
		System.out.println("Mapper using Cognitive Mapper");
		cp.blockVon(von.getID());
	}
}
