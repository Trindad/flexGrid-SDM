package flexgridsim;

import java.util.ArrayList;

import flexgridsim.util.MultipleKnapsack;

/**
 * 
 * @author trindade
 *
 */

public class OptimizedResourceAssignment {
	
	private MultipleKnapsack optimized;
	
	/**
	 * @param slotsAvailable
	 * @param deadline
	 * @param batch
	 * @param slotCapacity
	 */
	public OptimizedResourceAssignment(ArrayList<Integer> slotsAvailable, BatchConnectionRequest batch, boolean deadline, double slotCapacity, double time) {
		
		int []knapsacks = new int[slotsAvailable.size()];
		int []weights = new int[batch.getNumberOfFlows()];
		double []profities = new double[batch.getNumberOfFlows()];
		
		for(int i = 0; i < knapsacks.length; i++) {
//			System.out.print(" "+slotsAvailable.get(i));
			knapsacks[i] = slotsAvailable.get(i);
		}
		System.out.println();
		
		for(int i = 0; i < weights.length; i++) {
			
			weights[i] = (int) Math.ceil( (double)batch.get(i).getRate() / slotCapacity );
//			System.out.print(" +"+weights[i]);
			
			if(deadline) 
			{
				profities[i] = 1.0f - Math.ceil(batch.getEarliestDeadline().time/time);
			}
			else 
			{
				profities[i] = 1.0f;
			}
		}
		System.out.println();
		
		this.optimized = new MultipleKnapsack(knapsacks, weights, profities);
		
		this.optimized.runMultidimensionalKnapsack();
	}
	
	public ArrayList<ArrayList<Integer>> getEachDemandPerPath() {

		return this.optimized.getSolution();
	}

}
