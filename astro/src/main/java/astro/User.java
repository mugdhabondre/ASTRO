package astro;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
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
		List<Request> requests = getUserRequests();
		List<String> allocationLedger = new ArrayList();
		Collections.fill(allocationLedger, "");
		//TODO: for each request, we need to execute the following steps - use threads?
		for(int i=0; i < requests.size(); i++) {
			Request req  = requests.get(i);
			boolean isConnected = false;
			int retryTimes = 0;
			String childZNodePath = "", updatedChildZNodePath = "";
			while(!isConnected && retryTimes<5) {
				retryTimes++;
				// call resource request
				childZNodePath = getResourceCandidates(req.resourceType, req.propValue);
				// call resource connect till you get a connection.
				updatedChildZNodePath = resourceConnect(childZNodePath);
				if(!updatedChildZNodePath.equals(childZNodePath)) {
					isConnected = true;
					break;
				}
				Thread.sleep(3000); //TODO: MAKE IT RANDOM
			}
			if (isConnected) {
				allocationLedger.set(i, childZNodePath);
			}
		}
		
		// call quit function
		if (allocationLedger.stream().allMatch(val -> !val.equals(""))) {
			System.out.print("All resources have been allocated, you may proceed!");
			Thread.sleep(10000);
			System.out.print("Deallocating you resources. Bye.");
		}
		else {
			System.out.print("Could not allocate all the required resources, please retry.");
		}
		quit(allocationLedger);
	}
	
	/***
	 * Gets user requests
	 * @return list of request objects
	 * @throws Exception
	 */
	private List<Request> getUserRequests() throws Exception {
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
		
		return requests;
	}
	
	/*
	 * Returns resource candidate(s) for the connect phase
	 * */
	public String getResourceCandidates(String resourceType, int propertyValue) throws Exception {
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
			if(resourceEncoder.decodeIfAvailable(child) && 
					zkClient.getZNodeData(pathPrefix + "/" + child).equals("")) {
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
	public String resourceConnect(String zNodePath) throws Exception {
		// Reconnect to the resource node
		ZKConnection zkClient = new ZKConnection();
		String[] zNodePathElements = zNodePath.split("/");
		String pathPrefix = "/" + zNodePathElements[0] + "/", zNodeName = zNodePathElements[1];
		zkClient.connect(resourceEncoder.decodeAddress(zNodeName));
		
		//atomic give the resource and change znode
		//needed? synchronized(this) {
		
		//check if available
		if(resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals("")) {
			//claim ownership
			zkClient.updateNode(zNodePath, this.id.getBytes());
		} else { 
			zkClient.close();
			return zNodePath;
		}
		
		//allocate to yourself if you are still the owner
		if(resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals(this.id)) {
			String encodedZNodeName = resourceEncoder.encodeAsAllotted(zNodeName);
			zkClient.deleteNode(zNodePath);
			zkClient.createNode(pathPrefix + encodedZNodeName, this.id.getBytes());
			zkClient.close();
			return pathPrefix + encodedZNodeName;
		} else {
			zkClient.close();
			return zNodePath;
		}
		
	}
	
	
	public void quit(List<String> allocatedLedger) throws Exception {
		// Atomic deallocate and change znode
		// disconnect with the server
		ZKConnection zkClient = new ZKConnection();
		for(String zNodePath: allocatedLedger) {
			String[] zNodePathElements = zNodePath.split("/");
			String pathPrefix = "/" + zNodePathElements[0] + "/", zNodeName = zNodePathElements[1];
			zkClient.connect(resourceEncoder.decodeAddress(zNodeName));
			
			// Check and deallocate
			if(!resourceEncoder.decodeIfAvailable(zNodeName) &&
					zkClient.getZNodeData(zNodePath).equals(this.id)) {
				String encodedZNodeName = resourceEncoder.encodeAsDeallotted(zNodeName);
				zkClient.deleteNode(zNodePath);
				zkClient.createNode(pathPrefix + encodedZNodeName, "".getBytes());
			} 
			zkClient.close();
		}
	}
}
