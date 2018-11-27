package astro;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {

	String id;
	
	public User(String  id){
		this.id =  id; // id?
	}
	
	public void start(List<UserRequest> requests) throws Exception {
		List<String> allocationLedger = new ArrayList();
		for (UserRequest req: requests)
			allocationLedger.add("");

		//TODO: for each request, we need to execute the following steps - use threads?
		for(int i=0; i < requests.size(); i++) {
			UserRequest req  = requests.get(i);
			System.out.println("Requesting for Type: " + req.resourceType);
			boolean isConnected = false;
			int retryTimes = 0;
			String childZNodePath = "", updatedChildZNodePath = "";
				
			while(!isConnected && retryTimes<5) {
				retryTimes++;
				
				// call resource request
				childZNodePath = getResourceCandidates(req);
				// call resource connect till you get a connection.
				if (!childZNodePath.equals("")) {
					if(req.readOnly) {
						updatedChildZNodePath = childZNodePath;
						resourceShare(childZNodePath);
						isConnected = true;
						break;
					}
					updatedChildZNodePath = resourceConnect(childZNodePath);
					if(!updatedChildZNodePath.equals(childZNodePath)) {
						isConnected = true;
						break;
					}
				}
				Thread.sleep(3000); //TODO: MAKE IT RANDOM
			}
			allocationLedger.set(i, updatedChildZNodePath);
			if (isConnected)
				System.out.println("Allocated " + updatedChildZNodePath);
			else
				System.out.println("Could not allocate " + updatedChildZNodePath);
		}
		// call quit function
		if (allocationLedger.stream().allMatch(val -> !val.equals(""))) {
			System.out.print("All resources have been allocated, you may proceed!\n");
			Thread.sleep(30000);
			System.out.print("Deallocating your resources. Bye.\n");
		}
		else {
			System.out.print("Could not allocate all the required resources, please retry.\n");
		}
//		quit(allocationLedger);
	}
	
	public void start() throws Exception {
		// Get request from the user
		List<UserRequest> requests = getUserRequests();
		start(requests);
	}
	
	/***
	 * Gets user requests
	 * @return list of request objects
	 * @throws Exception
	 */
	private List<UserRequest> getUserRequests() throws Exception {
		BufferedReader reader =  
                new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter the requested resources: 1. Storage 2. Network 3. Compute");
		String request;
		List<UserRequest> requests = new ArrayList();
		while(!(request = reader.readLine()).equals("end")) {
			String[] splits = request.split(" ");
			UserRequest req = new UserRequest();
			if(splits[0].equals("1")) req.resourceType = "storage";
			else if(splits[0].equals("2")) req.resourceType = "network";
			else if(splits[0].equals("3")) req.resourceType = "compute";
			else throw new IllegalArgumentException();
			
			req.propValue = Integer.parseInt(splits[1]);
			if (splits.length == 3)
				req.readOnly = (splits[2].equals("1"));
			else
				req.readOnly = false;
			
			requests.add(req);
		}
		return requests;
	}
	
	/*
	 * Returns resource candidate(s) for the connect phase
	 */
	public String getResourceCandidates(UserRequest request) throws Exception {
		// Start ZK client and get ZNode structure
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + request.resourceType;
		List<String> children = zkClient.getChildren(pathPrefix);
		
		// Get ceiling of user requirements
		String childZNode = "";
		int currValue = Integer.MAX_VALUE;
		for(String child:children) {
			System.out.println("Trying to see if this is a candidate: " + child);
			if(CONSTANTS.resourceEncoder.decodeIfAvailable(child) && 
					(request.readOnly || zkClient.getZNodeData(pathPrefix + "/" + child).equals(""))) {
				int decodedValue = CONSTANTS.resourceEncoder.decodePropertyValue(child);
				boolean readOnly = CONSTANTS.resourceEncoder.decodeReadOnly(child);
				if(readOnly  == request.readOnly) {
					if(decodedValue == request.propValue) {
						childZNode = child;
						System.out.println("Resource found: " + child);
						break;
					} else if(decodedValue > request.propValue && decodedValue < currValue) {
						childZNode = child;
						currValue = decodedValue;
					}
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
		
		String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
		System.out.println("User trying to connect to: " + zNodeName);
		zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
		
		//check if available
		if(CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals("")) {
			System.out.println("Trying to claim ownership");
			//claim ownership
			zkClient.updateNode(zNodePath, this.id.getBytes());
			System.out.println("Claimed ownership");
		} else { 
			zkClient.close();
			return zNodePath;
		}
		
		//allocate to yourself if you are still the owner
		if(CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals(this.id)) {
			System.out.println("I am still the owner");
			String encodedZNodeName = CONSTANTS.resourceEncoder.encodeAsAllotted(zNodeName);
			zkClient.deleteAll(zNodePath);
			System.out.println("Creating the new node: " + encodedZNodeName + "with data: " + this.id);
			zkClient.createNode(pathPrefix + encodedZNodeName, this.id.getBytes());
			zkClient.close();
			return pathPrefix + encodedZNodeName;
		} else {
			zkClient.close();
			return zNodePath;
		}
		
	}
	
	public void resourceShare(String zNodePath) throws Exception{
		// Reconnect to the resource node
		ZKConnection zkClient = new ZKConnection();
		String[] zNodePathElements = zNodePath.split("/");
		
		String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
		System.out.println("User trying to connect to: " + zNodeName);
		zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
		
		// add a child znode with user id
		zkClient.createNode(zNodePath + "/" + this.id, "".getBytes());
		
		zkClient.close();
	}
	
	public void quit(List<String> allocatedLedger) throws Exception {
		// Atomic deallocate and change znode
		// disconnect with the server
		ZKConnection zkClient = new ZKConnection();
		for(String zNodePath: allocatedLedger) {
			if(!zNodePath.equals("")) {
				String[] zNodePathElements = zNodePath.split("/");
				String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
				zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
				System.out.println("Deallocating " + zNodePath);
				// Check and deallocate
				if(!CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
						zkClient.getZNodeData(zNodePath).equals(this.id)) {
					String encodedZNodeName = CONSTANTS.resourceEncoder.encodeAsDeallotted(zNodeName);
					zkClient.deleteAll(zNodePath);
					zkClient.createNode(pathPrefix + encodedZNodeName, "".getBytes());
				}
				zkClient.close();
			}
		}
		System.out.println("Successfully deallocated resources, exiting.");
	}
}
