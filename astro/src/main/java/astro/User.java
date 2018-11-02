package astro;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class User {
	
	class Request {
		String resourceType;
		int propValue;
	}
	
	String id;
	private static ResourceEncoder resourceEncoder = new ResourceEncoder();
	String host = "";
	
	public User(String  id){
		this.id =  id; // id?
	}
	
	public void start() throws Exception {
		// Get request from the user
		BufferedReader reader =  
                new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter the requested resources: 1. Storage 2. Network 3. Compute");
		String request;
		List<Request> requests = new ArrayList();
		while(!(request = reader.readLine()).equals("end")) {
			String[] splits = request.split(" ");
			Request req = new Request();
			if(splits[0].equals("1")) req.resourceType = "Storage";
			else if(splits[0].equals("2")) req.resourceType = "Network";
			else if(splits[0].equals("3")) req.resourceType = "Compute";
			else throw new IllegalArgumentException();
			
			req.propValue = Integer.parseInt(splits[1]);
			requests.add(req);
		}	
		
		//for each request, we need to execute the following steps - use threads?
		for(Request req: requests) {
			boolean isConnected = false;
			int retryTimes = 0;
			while(!isConnected && retryTimes<5) {
				retryTimes++;
				// call resource request
				String childZNodePath = resourceRequest(req.resourceType, req.propValue);
				// call resource connect till you get a connection.
				isConnected = resourceConnect(childZNodePath);
				Thread.sleep(3000);
			}
		}
		
		// call quit function
	}
	
	/*
	 * Resource Request phase
	 * */
	public String resourceRequest(String resourceType, int propertyValue) throws Exception {
		// Start ZK client and get ZNode structure
		ZKConnection zkClient = new ZKConnection();
		
		//for the purposes of this project, assume there is always one stable host.
		zkClient.connect(host);
		String pathPrefix = "/" + resourceType;
		List<String> children = zkClient.getChildren(pathPrefix);
		
		// Get ceiling of user requirements
		String childZNode = "";
		int currValue = Integer.MAX_VALUE;
		for(String child:children) {
			if(resourceEncoder.decodeIfAvailable(child)) {
				int decodedValue = resourceEncoder.decodePropertyValue(child); 
				if(decodedValue == propertyValue) {
					childZNode = child;
					break;
				} else if(decodedValue > propertyValue && decodedValue < currValue) {
					childZNode = child;
					currValue = decodedValue;
				}
			}
		}
		zkClient.close();
		
		// Return Ip, port of resource 
		return childZNode.equals("") ? childZNode : pathPrefix + "/" + childZNode;
	}
	
	/*
	 * Resource Connect Phase
	 * */
	public boolean resourceConnect(String zNodePath) throws Exception {
		// Reconnect to the resource node
		ZKConnection zkClient = new ZKConnection();
		String[] zNodePathElements = zNodePath.split("/");
		String pathPrefix = "/" + zNodePathElements[0] + "/", zNodeName = zNodePathElements[1];
		zkClient.connect(resourceEncoder.decodeAddress(zNodeName));
		
		//atomic give the resource and change znode
		synchronized(this) {
			if(resourceEncoder.decodeIfAvailable(zNodeName)) {
				String encodedZNodeName = resourceEncoder.encodeAsAllotted(zNodeName);
				zkClient.deleteNode(zNodePath);
				zkClient.createNode(pathPrefix + encodedZNodeName, this.id.getBytes());
				return true;
			}
		}
		
		return false;
	}
	
	
	public void quit() {
		// Atomic deallocate and change znode
		// disconnect with the server
	}
	
	
}
