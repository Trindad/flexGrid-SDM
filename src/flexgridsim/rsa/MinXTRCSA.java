package flexgridsim.rsa;

import java.util.ArrayList;

import flexgridsim.Flow;
import flexgridsim.Slot;
import flexgridsim.util.Rectangle;

/**
 * @author pedrom
 *
 */
public class MinXTRCSA extends InscribedRectangleRCSA {
	public boolean fitConnection(ArrayList<Rectangle> rectangles, int demandInSlots, int[] links, Flow flow, boolean[][] spectrum) {
		if (rectangles.size() <= 0)
			return false;
		
		ArrayList<Rectangle> all = new ArrayList<Rectangle>();
		for (Rectangle rectangle : rectangles) {
			if (rectangle.getSize() >= demandInSlots) {
				all.add(rectangle);
				break;
			}
		}
		int totalSize = 100000000;
		if (all.isEmpty()){
			totalSize = 0;
			for (int i = 0; i < rectangles.size() && totalSize < demandInSlots; i++) {
				all.add(rectangles.get(i));
				totalSize += rectangles.get(i).getSize();
			}
		}
		if (all.isEmpty() || totalSize < demandInSlots)
			return false;
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int size = 0;
		for (Rectangle r:all){
			for (int i = r.getRowStart(); i <= r.getRowEnd() && size < demandInSlots; i++) {
				for (int j = r.getColStart(); j <= r.getColEnd() && size < demandInSlots; j++) {
					size++;
					fittedSlotList.add(new Slot(i, j));
				}
			}
		}
		if (establishConnection(links, fittedSlotList, 0, flow))
			return true;
		else
			return false;
	}
}