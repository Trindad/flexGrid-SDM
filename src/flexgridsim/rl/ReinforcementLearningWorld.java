package flexgridsim.rl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import burlap.mdp.auxiliary.DomainGenerator;
import burlap.mdp.core.StateTransitionProb;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.environment.Environment;
import burlap.mdp.singleagent.environment.EnvironmentOutcome;
import burlap.mdp.singleagent.environment.extensions.EnvironmentObserver;
import burlap.mdp.singleagent.environment.extensions.EnvironmentServerInterface;
import burlap.mdp.singleagent.model.FactoredModel;
import burlap.mdp.singleagent.model.RewardFunction;
import burlap.mdp.singleagent.model.statemodel.FullStateModel;
import flexgridsim.voncontroller.Step.ACTIONS;
import flexgridsim.voncontroller.Symptom.SYMPTOM;

public class ReinforcementLearningWorld implements DomainGenerator {
	
	public static final String VAR_X = "x";
	public static final String VAR_Y = "y";

	public static final String ACTION_RIGHT = "right";
	public static final String ACTION_DOWN = "down";

	protected int goalx = 5;
	protected int goaly = 13;
	
	public int [][]map = new int [][] {
	    {1, 1, 1, 1, 1, 1, 1},
	    {1, 0, 0, 0, 1, 1, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 0, 1},
	    
	    {1, 0, 0, 0, 1, 0, 1},
	    {1, 0, 0, 0, 0, 0, 1},
	    {1, 1, 1, 1, 1, 1, 1}
	};

	public void setGoalLocation(){
		this.goalx = map.length - 2;
		this.goaly = map[0].length - 2;
		
		System.out.println("Goal " + goalx + ", " + goaly);
	}


	@Override
	public SADomain generateDomain() {

		SADomain domain = new SADomain();

		domain.addActionTypes(
				new UniversalActionType(ACTION_RIGHT),
				new UniversalActionType(ACTION_DOWN));

		GridWorldStateModel smodel = new GridWorldStateModel();
		RewardFunction rf = new PlanRF(this.goalx, this.goaly);
		TerminalFunction tf = new PlanTF(this.goalx, this.goaly);

		domain.setModel(new FactoredModel(smodel, rf, tf));

		return domain;
	}
	
	protected class GridWorldStateModel implements FullStateModel{


		protected double [][] transitionProbs;

		
		public GridWorldStateModel() {
			int n = 13;
			this.transitionProbs = new double[][] {
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,1},
					{0,0,0,0,0,0,0,0,0,0,0,0,0},
			};
		}

		public List<StateTransitionProb> stateTransitions(State s, Action a) {

			//get agent current position
			GridState gs = (GridState)s;

			int curX = gs.x;
			int curY = gs.y;

			int adir = actionDir(a);

			List<StateTransitionProb> tps = new ArrayList<StateTransitionProb>(4);
			StateTransitionProb noChange = null;
			for(int i = 0; i < 4; i++){

				int [] newPos = this.moveResult(curX, curY, i);
				if(newPos[0] != curX || newPos[1] != curY){
					//new possible outcome
					GridState ns = gs.copy();
					ns.x = newPos[0];
					ns.y = newPos[1];

					//create transition probability object and add to our list of outcomes
					tps.add(new StateTransitionProb(ns, this.transitionProbs[adir][i]));
				}
				else{
					//this direction didn't lead anywhere new
					//if there are existing possible directions
					//that wouldn't lead anywhere, aggregate with them
					if(noChange != null){
						noChange.p += this.transitionProbs[adir][i];
					}
					else{
						//otherwise create this new state and transition
						noChange = new StateTransitionProb(s.copy(), this.transitionProbs[adir][i]);
						tps.add(noChange);
					}
				}

			}


			return tps;
		}

		public State sample(State s, Action a) {

			s = s.copy();
			GridState gs = (GridState)s;
			int curX = gs.x;
			int curY = gs.y;

			int adir = actionDir(a);

			//sample direction with random roll
			double r = Math.random();
			double sumProb = 0.;
			

			//get resulting position
			int [] newPos = this.moveResult(curX, curY, adir);

			//set the new position
			gs.x = newPos[0];
			gs.y = newPos[1];
			
			System.out.println(newPos[0] + ", " + newPos[1] + " action: " + a);

			//return the state we just modified
			return gs;
		}

		protected int actionDir(Action a) {
			int adir = -1;
			if(a.actionName().equals(ACTION_RIGHT)){
				adir = 2;
			}
			else if(a.actionName().equals(ACTION_DOWN)){
				adir = 1;
			}
			
			return adir;
		}


		protected int [] moveResult(int curX, int curY, int direction) {

			//first get change in x and y from direction using 0: north; 1: south; 2:east; 3: west
			int xdelta = 0;
			int ydelta = 0;
			
			if(direction == 1){
				xdelta = 1;
			}
			else if(direction == 2){
				ydelta = 1;
			}

			int nx = curX + xdelta;
			int ny = curY + ydelta;

			int width = map[0].length;
			int height = map.length;
			
//			System.out.println(curX + ", " + curY + " Direction: " + direction + " new pos " + nx + ", " + ny + " : " + map[nx][ny]);
//			System.out.println((nx < 0 ? 1 : 0) + ", " + (nx >= width ? 1 : 0) + ", " + (ny < 0 ? 1 : 0) + ", " + (ny >= height ? 1 : 0));

			//make sure new position is valid (not a wall or off bounds)
			if(nx < 0 || nx >= height || ny < 0 || ny >= width ||
					map[nx][ny] == 1){
				nx = curX;
				ny = curY;
			}


			return new int[]{nx,ny};

		}
	}
	
	public static class PlanRF implements RewardFunction {

		int goalX;
		int goalY;

		public PlanRF(int goalX, int goalY){
			this.goalX = goalX;
			this.goalY = goalY;
		}

		public double reward(State s, Action a, State sprime) {

			int ax = (Integer)s.get(VAR_X);
			int ay = (Integer)s.get(VAR_Y);

			//are they at goal location?
			if(ax == this.goalX && ay == this.goalY){
				return 100.;
			}

			return -1;
		}


	}

	public static class PlanTF implements TerminalFunction {

		int goalX;
		int goalY;

		public PlanTF(int goalX, int goalY){
			this.goalX = goalX;
			this.goalY = goalY;
		}

		public boolean isTerminal(State s) {

			//get location of agent in next state
			int ax = (Integer)s.get(VAR_X);
			int ay = (Integer)s.get(VAR_Y);

			//are they at goal location?
			if(ax == this.goalX && ay == this.goalY){
				return true;
			}

			return false;
		}



	}

}
