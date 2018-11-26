package flexgridsim.von;

import java.util.ArrayList;

public class VirtualNode {
	public static int ID = 1;
	
	private ArrayList<Integer> candidatePhysicalNodes;
	private int computeResource;
	private int id;
	private int physicalNode;
	
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

}
