package astro;

public class UserRequest {
	String resourceType;
	boolean readOnly;
	int propValue;
	
	public UserRequest() {
		
	}
	
	public UserRequest(String resourceType, boolean readOnly, int propValue) {
		this.resourceType = resourceType;
		this.readOnly = readOnly;
		this.propValue = propValue;
	}
}
