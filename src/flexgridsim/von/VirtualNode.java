package flexgridsim.von;

import java.util.ArrayList;

/**
 * 
 * @author trindade
 *
 */
public class VirtualNode {
	public static int ID = 0;
	
	private ArrayList<Integer> candidatePhysicalNodes;
	private int computeResource;
	private int id;
	private int physicalNode;
	private double requestResource;
	
	public VirtualNode() {
		id = VirtualNode.ID++;
	}
	
	public ArrayList<Integer> getCandidatePhysicalNodes() {
		return candidatePhysicalNodes;
	}



	public void setCandidatePhysicalNodes(ArrayList<Integer> candidatePhysicalNodes) {
		this.candidatePhysicalNodes = candidatePhysicalNodes;
	}



	public int getComputeResource() {
		return computeResource;
	}



	public void setComputeResource(int computeResource) {
		this.computeResource = computeResource;
	}



	public int getId() {
		return id;
	}



	public void setId(int id) {
		this.id = id;
	}



	public int getPhysicalNode() {
		return physicalNode;
	}

	public void setPhysicalNode(int physicalNode) {
		this.physicalNode = physicalNode;
	}

	public double getRequestResource() {
		return requestResource;
	}

	public void setRequestResource(double requestResource) {
		this.requestResource = requestResource;
	}
}
