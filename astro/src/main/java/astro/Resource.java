package astro;

import java.util.List;

public class Resource {
	String resourceType;
	String ip, port;
	boolean readOnly;
	List<ResourceProperty> properties;
	
	public Resource(String resourceType, String ip, String port, boolean readOnly, List<ResourceProperty> properties) {
		this.resourceType = resourceType;
		this.ip = ip;
		this.port = port;
		this.readOnly = readOnly;
		this.properties = properties;
	}
	
	public String getResourceType() {
		return this.resourceType;
	}
	
	public void addResourceProp(ResourceProperty prop) {
		this.properties.add(prop);
	}
	
	public List<ResourceProperty> getResourceProps() {
		return this.properties;
	}
	
	public String getResourceIP() {
		return this.ip;
	}
	
	public String getResourcePort() {
		return this.port;
	}
	
	public boolean getReadOnlyFlag() {
		return this.readOnly;
	}
	
}
