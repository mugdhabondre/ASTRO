package astro;

import java.util.ArrayList;
import java.util.List;

public class Resource {
	String resourceType;
	String ip, port;
	List<ResourceProperty> properties;
	
	public Resource(String resourceType, String ip, String port) {
		this.resourceType = resourceType;
		this.ip = ip;
		this.port = port;
		properties = new ArrayList();
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
	
}
