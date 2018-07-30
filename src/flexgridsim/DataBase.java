package flexgridsim;

import java.util.ArrayList;
import java.util.Map;

/**
 * 
 * @author trindade
 *
 */
public class DataBase {

	public int usedTranspoders;
	public int availableTransponders;
	public Map<Long, Integer> slotsAvailable; 
	public Map<Long, Integer> slotsOccupied; 
}
