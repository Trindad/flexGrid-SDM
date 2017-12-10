package flexgridsim;

/**
 * 
 * @author trindade
 *
 */
public class DefragmentationEvent extends Event{
	
	private LightPath path;

	public DefragmentationEvent(double time, LightPath path) {
		super(time);
		this.path = path;
	}

	public LightPath getPath() {
		return path;
	}

	public void setPath(LightPath path) {
		this.path = path;
	}

}
