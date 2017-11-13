package flexgridsim.util;
import java.util.ArrayList;

import gurobi.*;

/**
 * Copyright 2017, Gurobi Optimization, Inc.
 * 
 * @author trindade
 *
 */
public class MultipleKnapsack {

	private  int nCategories;
	private String[] Categories;
	private String[] items;
	private int nItems;
	private double[] cost;
	private int[] values;
	private int min[];
	private int max[];
	private ArrayList<ArrayList<Integer>> solution;
	private boolean consideringCost = false;
	/**
	 * Set values
	 * @param regions
	 * @param demandInSlots
	 * @param profit
	 */
	public MultipleKnapsack(int []knapsacks, int []weights, double []profities) {
		
		 this.Categories = new String[knapsacks.length];
		 this.nCategories = knapsacks.length;
		      
		 //limits of knapsack capacities
		 this.max = new int[knapsacks.length];
		 this.min = new int[knapsacks.length];
		 
		 for(int i = 0; i < nCategories; i++) {
			 this.max[i] = knapsacks[i];
			 this.min[i] = 0;
			 this.Categories[i] = "ID: "+Integer.toString(knapsacks[i]);
		 }
		 
		
		  // Set of items
		  items = new String[weights.length];
		  this.nItems = items.length;
		 
		 for(int i = 0; i < nItems; i++) {
			 items[i] = Integer.toString(i);
		 }
		 this.cost =  profities;
		
		 // values for the items
		 this.values = weights;
	}
	
	/**
	 * Run the ILP
	 */
	public void runMultidimensionalKnapsack() {
		    
		try {	      
		      // Model
		      GRBEnv env = new GRBEnv();
		      GRBModel model = new GRBModel(env);
		      model.set(GRB.StringAttr.ModelName, "mkp");

		      // Create decision variables for the information,
		      // which we limit via bounds
		      GRBVar []decision = new GRBVar [nCategories];
		      
		      System.out.println("nitems: " + nItems + ", nCategories: " + nCategories);
			  
			  for (int i = 0; i < nCategories; i++) {
				  
				  decision[i] = model.addVar(min[i], max[i], 0, GRB.INTEGER,
		                         Categories[i]);
			  }
		      
		      // Create decision variables for the items to put
		      GRBVar[][] put = new GRBVar[nCategories][nItems];
		      
		      for (int i = 0; i < nCategories; i++) {
			      for (int j = 0; j < nItems; j++) {
			    	  double c_j = cost[j];
			    	  if(consideringCost == true) {
			    		  c_j = (1.0f - c_j);
			    	  }
			    	  put[i][j] = model.addVar(0, 1, c_j , GRB.BINARY, items[j]);
			      }
		      }
		      
		      // The objective is to maximize the costs
		      model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
		      
		      
		      // constraints
			  for (int i = 0; i < nCategories; ++i) {
				  
				GRBLinExpr ntot = new GRBLinExpr();
				
		        for (int j = 0; j < nItems; j++) {
		    	  
		          ntot.addTerm(values[j], put[i][j]);
		        }
		        
		        model.addConstr(ntot, GRB.EQUAL, decision[i], Categories[i]);
			  }
			  
			  for (int j = 0; j < nItems; j++) {
		    	  GRBLinExpr lhs = new GRBLinExpr();
		    	  
		    	  for (int i = 0; i < nCategories; i++) {
		    		  lhs.addTerm(1, put[i][j]);
		    	  }
		    	  
		    	  model.addConstr(lhs, GRB.LESS_EQUAL, 1, "put");
		      }
			  

			  // Solve
		      model.optimize();
//		      printSolution(model, put, decision);
		      setSolution(put);//convert solution to integer
		      
		      // Dispose of model and environment
		      model.dispose();
		      env.dispose();
		    } 
		    catch (GRBException e) 
		    {
		      System.out.println("Error code: " + e.getErrorCode() + ". " +
		          e.getMessage());
		    }
		  }

		  @SuppressWarnings("unused")
		private static void printSolution(GRBModel model, GRBVar[][] put,
		                                    GRBVar[] value) throws GRBException {
		    if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
		      System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
		      
		      for (int j = 0; j < put.length; j++) {
		    	  System.out.println("\nPut:");
		    	for(int i = 0; i < put[j].length; i++) {
			        if (put[j][i].get(GRB.DoubleAttr.X) > 0.0001) {
			          System.out.println(put[j][i].get(GRB.StringAttr.VarName) + " " +
			              put[j][i].get(GRB.DoubleAttr.X));
			        }
		    	}
				System.out.println("\nAcumulated value:");

			    System.out.println(value[j].get(GRB.StringAttr.VarName) + " " +
			        value[j].get(GRB.DoubleAttr.X));

				System.out.println();
		      }
		      
		    } else {
		      System.out.println("No solution");
		    }
		  }

		private void setSolution(GRBVar[][] put) {
			
			solution = new ArrayList<ArrayList<Integer>>();
			
			for(int i = 0; i < put.length; i++) {
				solution.add(new ArrayList<Integer>());
				
				for(int j = 0; j < put[i].length; j++) {
					try {
						if( put[i][j].get(GRB.DoubleAttr.X) >= 1 )
							solution.get(i).add(Integer.parseInt(put[i][j].get(GRB.StringAttr.VarName)));
					} 
					catch (GRBException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		public ArrayList<ArrayList<Integer>> getSolution() {
			
			return solution;
		}
}
