package flexgridsim;

/**
 * 
 * @author trindade
 *
 */
public class DefragmentationEvent extends Event{
	
	private String path;

	public DefragmentationEvent(double time, String path) {
		super(time);
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
