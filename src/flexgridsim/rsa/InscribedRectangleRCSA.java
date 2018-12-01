package flexgridsim.rsa;

import java.util.ArrayList;

import org.w3c.dom.Element;

import flexgridsim.Flow;
import flexgridsim.LightPath;
import flexgridsim.PhysicalTopology;
import flexgridsim.Slot;
import flexgridsim.TrafficGenerator;
import flexgridsim.VirtualTopology;
import flexgridsim.util.InscribedRectangle;
import flexgridsim.util.KShortestPaths;
import flexgridsim.util.Rectangle;
import flexgridsim.util.WeightedGraph;

/**
 * @author pedrom
 *
 */
public class InscribedRectangleRCSA implements RSA {

	protected PhysicalTopology pt;
	protected VirtualTopology vt;
	protected ControlPlaneForRSA cp;
	protected WeightedGraph graph;
	
	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, VirtualTopology vt, ControlPlaneForRSA cp,
			TrafficGenerator traffic) {
		this.pt = pt;
		this.vt = vt;
		this.cp = cp;
		this.graph = pt.getWeightedGraph();

	}

	@Override
	public void flowArrival(Flow flow) {
		int demandInSlots = (int) Math.ceil(flow.getRate() / (double) pt.getSlotCapacity());
		KShortestPaths kShortestPaths = new KShortestPaths();
		int[][] kPaths = kShortestPaths.dijkstraKShortestPaths(graph, flow.getSource(), flow.getDestination(), 5);
		boolean[][] spectrum = new boolean[pt.getCores()][pt.getNumSlots()];
		
		for (int k = 0; k < kPaths.length; k++) {
			for (int i = 0; i < spectrum.length; i++) {
				for (int j = 0; j < spectrum[i].length; j++) {
					spectrum[i][j]=true;
				}
			}
			for (int i = 0; i < kPaths[k].length-1; i++) {
				imageAnd(pt.getLink(kPaths[k][i], kPaths[k][i+1]).getSpectrum(), spectrum, spectrum);
			}
//			printSpectrum(spectrum);
			
			InscribedRectangle ir = new InscribedRectangle();
			ArrayList<Rectangle> rectangles = ir.calculateRectangles(spectrum.length, spectrum[0].length, spectrum);

			int[] links = new int[kPaths[k].length - 1];
			for (int j = 0; j < kPaths[k].length - 1; j++) {
				links[j] = pt.getLink(kPaths[k][j], kPaths[k][j + 1]).getID();
			}

			if (fitConnection(rectangles, demandInSlots, links, flow, spectrum)){
				return;
			}
		}
		cp.blockFlow(flow.getID());
		return;
	}
	
	/**
	 * @param rectangles
	 * @param demandInSlots
	 * @param links
	 * @param flow
	 * @param spectrum
	 * @return given a list of rectangles and a demand, the algorithm tries to fit the connector into the spectra
	 */
	public boolean fitConnection(ArrayList<Rectangle> rectangles, int demandInSlots, int[] links, Flow flow, boolean[][] spectrum){
		if (rectangles.size()<=0)
			return false;
		Rectangle minDiff = rectangles.get(0);
		for (Rectangle rectangle : rectangles) {
			if (rectangle.getSize() >= demandInSlots){
					minDiff=rectangle;
					break;
		    }
		}
		if (minDiff==rectangles.get(0) && minDiff.getSize()<demandInSlots)
				return false;
		ArrayList<Slot> fittedSlotList = new ArrayList<Slot>();
		int size = 0;
		for (int i = minDiff.getRowStart(); i <= minDiff.getRowEnd(); i++) {
			for (int j = minDiff.getColStart(); j <= minDiff.getColEnd() && size < demandInSlots; j++) {
				size++;
				fittedSlotList.add(new Slot(i, j));
			}
		}
		if (establishConnection(links, fittedSlotList, 0, flow))
			return true;
		else 
			return false;
	}
	
	/**
	 * @param links
	 * @param slotList
	 * @param modulation
	 * @param flow
	 * @return true if the connection was successfully established; false otherwise
	 */
	public boolean establishConnection(int[] links, ArrayList<Slot> slotList, int modulation, Flow flow){
		long id = vt.createLightpath(links, slotList ,0);
		if (id >= 0) {
			LightPath lps = vt.getLightpath(id);
			flow.setLinks(links);
			flow.setSlotList(slotList);
			cp.acceptFlow(flow.getID(), lps);
			return true;
		} else {
			return false;
		}
	}
		
	protected void imageAnd(boolean[][] img1, boolean[][] img2, boolean[][] res){
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[0].length; j++) {
				res[i][j] = img1[i][j] & img2[i][j];
			}
		}
	}

	@Override
	public void flowDeparture(Flow flow) {

	}

	@Override
	public void simulationInterface(Element xml, PhysicalTopology pt, TrafficGenerator traffic) {
		// TODO Auto-generated method stub
		
	}

}
