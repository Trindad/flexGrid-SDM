package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.rsa.ZhangDefragmentationRCSA;

public class BalanceDefragmentationRCSA extends ZhangDefragmentationRCSA{

	protected double []fi;

	public void setFragmentationIndexOfEachLink(double []fi) {
		
		this.fi = fi;
	}
	
	public void flowArrival(Flow flow) {

		ArrayList<int[]> kPaths = findKPaths(flow);//find K-Shortest paths using Dijkstra
		ArrayList<Integer> indices = orderKPaths(kPaths);//sort paths by the fragmentation index from each link

		for(int i: indices)
		{
			ArrayList<Slot> slotList = canBeProvided(flow, kPaths.get(i)); 
			
			if(!slotList.isEmpty()) 
			{
				if(establishConnection(kPaths.get(i), slotList, flow.getModulationLevel(), flow)) 
				{
					return;
				}
			}
		}
		
		this.connectionDisruption.add(flow);
		flow.setConnectionDisruption(true);
		this.nConnectionDisruption++;
	}

	private ArrayList<Integer>orderKPaths(ArrayList<int[]> kPaths) {
		
		ArrayList<Integer> sumLightpath = new ArrayList<Integer>();
		ArrayList<Integer> indices = new ArrayList<Integer>(); 
		int index = 0;
		
		for(int[] links: kPaths) {

			double s = Double.NEGATIVE_INFINITY;
			for(int i = 0; i < links.length; i++) {
				s = s < fi[ links[i] ] ? fi[ links[i] ] : s;
			}
			
			sumLightpath.add( (int)(s * 1000) );
			indices.add(index);
			index++;
		}
		
//		System.out.println(Arrays.toString(sumLightpath.toArray()));
		indices.sort( (a , b) -> sumLightpath.get(a) - sumLightpath.get(b));
		
		return indices;
	}
	
}
