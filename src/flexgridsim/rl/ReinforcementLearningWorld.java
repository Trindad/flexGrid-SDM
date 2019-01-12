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

public class ReinforcementLearningWorld implements DomainGenerator {
	
	public static final String VAR_X = "x";
	public static final String VAR_Y = "y";

	public static final String ACTION_BLOCK_COSTLY_NODE = "block_costly_node";
	public static final String ACTION_RECONFIGURATION_PERFORMANCE_LINK = "south";
	public static final String ACTION_BLOCK_BALANCED_LINK = "block_balanced_link";
	public static final String ACTION_LIMIT_OVERLOAD_LINK = "limit_overload_node";
	public static final String ACTION_LIMIT_COSTLY_NODE = "limit_costly_node";
	public static final String ACTION_NOTHING = "nothing";


	protected int goalx = 10;
	protected int goaly = 10;

	//ordered so first dimension is x
	protected int [][] map = new int[][]{
			{0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0},
			{1,0,1,1,1,1,1,1,0,1,1},
			{0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,0},
	};

	public void setGoalLocation(int goalx, int goaly){
		this.goalx = goalx;
		this.goaly = goaly;
	}


	@Override
	public SADomain generateDomain() {

		SADomain domain = new SADomain();


		domain.addActionTypes(
				new UniversalActionType(ACTION_BLOCK_BALANCED_LINK),
				new UniversalActionType(ACTION_BLOCK_COSTLY_NODE),
				new UniversalActionType(ACTION_LIMIT_COSTLY_NODE),
				new UniversalActionType(ACTION_LIMIT_OVERLOAD_LINK),
				new UniversalActionType(ACTION_RECONFIGURATION_PERFORMANCE_LINK),
				new UniversalActionType(ACTION_NOTHING));

		GridWorldStateModel smodel = new GridWorldStateModel();
		RewardFunction rf = new PlanRF(this.goalx, this.goaly);
		TerminalFunction tf = new PlanTF(this.goalx, this.goaly);

		domain.setModel(new FactoredModel(smodel, rf, tf));

		return domain;
	}
	
	protected class GridWorldStateModel implements FullStateModel{


		protected double [][] transitionProbs;

		
		public GridWorldStateModel() {
			int n = 6;
			this.transitionProbs = new double[n][n];
			for(int i = 0; i < n; i++){
				for(int j = 0; j < n; j++){
					double p = i != j ? 0.2/3 : 0.8;
					transitionProbs[i][j] = p;
				}
			}
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
			int dir = 0;
			for(int i = 0; i < 4; i++){
				sumProb += this.transitionProbs[adir][i];
				if(r < sumProb){
					dir = i;
					break; //found direction
				}
			}

			//get resulting position
			int [] newPos = this.moveResult(curX, curY, dir);

			//set the new position
			gs.x = newPos[0];
			gs.y = newPos[1];

			//return the state we just modified
			return gs;
		}

		protected int actionDir(Action a) {
			int adir = -1;
			if(a.actionName().equals(ACTION_BLOCK_BALANCED_LINK)){
				adir = 0;
			}
			else if(a.actionName().equals(ACTION_BLOCK_COSTLY_NODE)){
				adir = 1;
			}
			else if(a.actionName().equals(ACTION_LIMIT_COSTLY_NODE)){
				adir = 2;
			}
			else if(a.actionName().equals(ACTION_LIMIT_OVERLOAD_LINK)){
				adir = 3;
			}
			else if(a.actionName().equals(ACTION_RECONFIGURATION_PERFORMANCE_LINK)){
				adir = 4;
			}
			else if(a.actionName().equals(ACTION_NOTHING)){
				adir = 5;
			}
			
			return adir;
		}


		protected int [] moveResult(int curX, int curY, int direction) {

			//first get change in x and y from direction using 0: north; 1: south; 2:east; 3: west
			int xdelta = 0;
			int ydelta = 0;
			if(direction == 0){
				ydelta = 1;
			}
			else if(direction == 1){
				ydelta = -1;
			}
			else if(direction == 2){
				xdelta = 1;
			}
			else{
				xdelta = -1;
			}

			int nx = curX + xdelta;
			int ny = curY + ydelta;

			int width = ReinforcementLearningWorld.this.map.length;
			int height = ReinforcementLearningWorld.this.map[0].length;

			//make sure new position is valid (not a wall or off bounds)
			if(nx < 0 || nx >= width || ny < 0 || ny >= height ||
					ReinforcementLearningWorld.this.map[nx][ny] == 1){
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
