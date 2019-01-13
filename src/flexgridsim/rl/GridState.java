package flexgridsim.rl;

import java.util.Arrays;
import java.util.List;

import burlap.mdp.core.state.MutableState;
import burlap.mdp.core.state.StateUtilities;
import burlap.mdp.core.state.UnknownKeyException;

public class GridState implements MutableState {
 
		public int x;
		public int y;

		private final static List<Object> keys = Arrays.<Object>asList(ReinforcementLearningWorld.VAR_X, ReinforcementLearningWorld.VAR_Y);

		public GridState() {
		}

		public GridState(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public MutableState set(Object variableKey, Object value) {
			if(variableKey.equals(ReinforcementLearningWorld.VAR_X)){
				this.x = StateUtilities.stringOrNumber(value).intValue();
			}
			else if(variableKey.equals(ReinforcementLearningWorld.VAR_Y)){
				this.y = StateUtilities.stringOrNumber(value).intValue();
			}
			else{
				throw new UnknownKeyException(variableKey);
			}
			return this;
		}

		public List<Object> variableKeys() {
			return keys;
		}

		@Override
		public Object get(Object variableKey) {
			if(variableKey.equals(ReinforcementLearningWorld.VAR_X)){
				return x;
			}
			else if(variableKey.equals(ReinforcementLearningWorld.VAR_Y)){
				return y;
			}
			throw new UnknownKeyException(variableKey);
		}

		@Override
		public GridState copy() {
			return new GridState(x, y);
		}

		@Override
		public String toString() {
			return StateUtilities.stateToString(this);
		}
}
