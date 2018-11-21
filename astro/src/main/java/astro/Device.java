package astro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Device {
	String ip;
	String port;
	List<Resource> resources;
	private static ResourceEncoder resourceEncoder = new ResourceEncoder();
	String host = "128.61.48.64:2181";
	
	public Device(String ip, String port) {
		this.ip = ip;
		this.port = port;
		this.resources = new ArrayList();
	}
	
	public void addResource(String resourceType, List<ResourceProperty> properties ) throws Exception {
		Resource newResource = new Resource(resourceType, this.ip, this.port, properties);
		this.resources.add(newResource);
		String encodedResource = resourceEncoder.encode(newResource);
	
		ZKConnection zkClient = new ZKConnection();
		
		//for the purposes of this project, assume there is always one stable host.
		zkClient.connect(host);
		String pathPrefix = "/" + resourceType;
		
		zkClient.createNode(pathPrefix + "/" + encodedResource, "".getBytes());
		
		zkClient.close();
	}
	
	public void printResources() {
		System.out.println("Printing resources for "+ this.ip + ":" + this.port);
		for (int  i = 0; i < resources.size(); i++) {
			System.out.println(i + ":");
			System.out.println("\tResource Type:" + resources.get(i).getResourceType());
			for (ResourceProperty prop: resources.get(i).getResourceProps()) {
				System.out.println("\t" + prop.getPropType() + ": " + prop.getCapacity());
			}
		}
	}
	
	public void removeResource(int resourceIndex, boolean force) throws Exception {
		
		Resource resource = resources.get(resourceIndex);
		String encodedResource = resourceEncoder.encode(resource);
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(host);
		String pathPrefix = "/" + resource.getResourceType();
		
		List<String> children = zkClient.getChildren(pathPrefix);
		for(String child: children) {
			if(child.startsWith(encodedResource.substring(0, encodedResource.length()-1))) {
				String path = pathPrefix + "/" + child;
				if(zkClient.getZNodeData(path).equals("") || force)
					zkClient.deleteNode(path);
				else {
					System.out.println("Someone is using the resource. Try again later.");
				}
				break;
			}
		}
		zkClient.close();
	}
	
}
