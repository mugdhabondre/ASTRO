package astro;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.zookeeper.KeeperException;

public class User {

	String id;
	TimeWatch overWatch;
	List<String> allocationLedger;

	public User(String  id){
		this.id =  id; // id?
		allocationLedger = new ArrayList();
		overWatch = new TimeWatch();
	}
	
	public User(String  id, String fileName){
		this.id =  id; // id?
		allocationLedger = new ArrayList();
		overWatch = new TimeWatch(fileName);
	}
	
	public void start(List<UserRequest> requests) throws IOException, InterruptedException {
		for (UserRequest req: requests)
			allocationLedger.add("");

		//TODO: for each request, we need to execute the following steps - use threads?
		for(int i=0; i < requests.size(); i++) {
			UserRequest req  = requests.get(i);
			// System.out.println("Requesting for Type: " + req.resourceType);
			boolean isConnected = false;
			int retryTimes = 0;
			String childZNodePath = "", updatedChildZNodePath = "";
			List<String> candidates;
			
			while(!isConnected && retryTimes<5) {
				retryTimes++;
				
				// call resource request
				// childZNodePath = getResourceCandidates(req);
				overWatch.reset();
				try {
					candidates = getResourceCandidates(req);
				}
				catch (KeeperException e) {
					// System.out.println("Keeper exception caught for GetResCands, continuing : " + e.getMessage());
					continue;
				}
				overWatch.elapsedTime("GetResourceCandidates");
				
				overWatch.reset();
				// call resource connect till you get a connection.
				for(String candidate: candidates) {
					// Share without checking if ReadOnly
					// System.out.println("User " + this.id + " trying for candidate: " + candidate);
					try {
						if(req.readOnly) {
							updatedChildZNodePath = candidate;
							resourceShare(candidate);
							isConnected = true;
							break;
						} else {
							// Extra checks for non-readOnly resources
							updatedChildZNodePath = resourceConnect(candidate);
							if(!updatedChildZNodePath.equals(candidate)) {
								isConnected = true;
								break;
							}
						}
					} catch (KeeperException e) {
						// System.out.println("Keeper exception caught for " + candidate + "for user id : " + this.id  + " continuing : " + e.getMessage());
						continue;
					}
				}
				overWatch.elapsedTime("ResourceConnect");
				if (isConnected == true) {
					allocationLedger.set(i, updatedChildZNodePath);
					break;
				}
				// System.out.println("Could not connect. retrying for user id : " + this.id);
				Thread.sleep((long)(Math.random() * 1000));
			}
			
//			if (isConnected)
//				System.out.println("Allocated " + updatedChildZNodePath);
//			else
//				System.out.println("Could not allocate " + updatedChildZNodePath);
		}
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
	public List<String> getResourceCandidates(UserRequest request) throws IOException, InterruptedException, KeeperException {
		// Start ZK client and get ZNode structure
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + request.resourceType;
		List<String> children = zkClient.getChildren(pathPrefix);
		
		List<String> candidates = new ArrayList<String>();
		// Get ceiling of user requirements
		String childZNode = "";
		int currValue = Integer.MAX_VALUE;
		for(String child:children) {
			// System.out.println("Trying to see if this is a candidate: " + child);
			if(CONSTANTS.resourceEncoder.decodeIfAvailable(child) && 
					(request.readOnly || zkClient.getZNodeData(pathPrefix + "/" + child).equals(""))) {
				int decodedValue = CONSTANTS.resourceEncoder.decodePropertyValue(child);
				boolean readOnly = CONSTANTS.resourceEncoder.decodeReadOnly(child);
				if(readOnly == request.readOnly && decodedValue >= request.propValue) {
					// System.out.println("Resource found: " + child);
					candidates.add(pathPrefix + "/" + child);
				}
			}
		}
		zkClient.close();
		
		// Return Ip, port of resource 
		// return childZNode.equals("") ? childZNode : pathPrefix + "/" + childZNode;
		Collections.sort(candidates, new Utils.ZNodeComparator());
		return candidates;
		
	}
	
	/*
	 * Resource Connect Phase
	 * */
	public String resourceConnect(String zNodePath) throws KeeperException, InterruptedException, IOException {
		// Reconnect to the resource node
		ZKConnection zkClient = new ZKConnection();
		String[] zNodePathElements = zNodePath.split("/");
		
		String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
		// System.out.println("User trying to connect to: " + zNodeName);
		zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
		
		// Latency check wrapper
		//overWatch.reset();
		//check if available
		if(CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals("")) {
			//System.out.println("Trying to claim ownership");
			//claim ownership
			zkClient.updateNode(zNodePath, this.id.getBytes());
			//System.out.println("Claimed ownership");
		} else { 
			zkClient.close();
			return zNodePath;
		}
		// endOf check if available
		// overWatch.elapsedTime("ResourceConnect|Phase1");
		// endOf Latency check wrapper
		
		
		// Latency check
		// overWatch.reset();
		// allocate to yourself if you are still the owner
		if(CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
				zkClient.getZNodeData(zNodePath).equals(this.id)) {
			//System.out.println("I am still the owner");
			String encodedZNodeName = CONSTANTS.resourceEncoder.encodeAsAllotted(zNodeName);
			zkClient.deleteAll(zNodePath);
//			System.out.println("Creating the new node: " + encodedZNodeName + "with data: " + this.id);
			zkClient.createNode(pathPrefix + encodedZNodeName, this.id.getBytes());
			zkClient.close();
			// overWatch.elapsedTime("ResourceConnect|Phase2");
			return pathPrefix + encodedZNodeName;
		} else {
			zkClient.close();
			// overWatch.elapsedTime("ResourceConnect|Phase2");
			return zNodePath;
		}
		
	}
	
	public void resourceShare(String zNodePath) throws IOException, InterruptedException, KeeperException{
		// Reconnect to the resource node
		ZKConnection zkClient = new ZKConnection();
		String[] zNodePathElements = zNodePath.split("/");
		
		String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
		// System.out.println("User trying to connect to: " + zNodeName);
		zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
		
		// add a child znode with user id
		zkClient.createNode(zNodePath + "/" + this.id, "".getBytes());
		
		zkClient.close();
	}
	
	public void quit() throws Exception {
		// Atomic deallocate and change znode
		// disconnect with the server
		ZKConnection zkClient = new ZKConnection();
		
		for(String zNodePath: allocationLedger) {
			if(!zNodePath.equals("")) {
				// Connect to resource
				overWatch.reset();
				String[] zNodePathElements = zNodePath.split("/");
				String pathPrefix = "/" + zNodePathElements[1] + "/", zNodeName = zNodePathElements[2];
				zkClient.connect(CONSTANTS.resourceEncoder.decodeAddress(zNodeName));
				overWatch.elapsedTime("QuitUser|Connect");
				
				// System.out.println("Deallocating " + zNodePath);
				// Check and deallocate
				overWatch.reset();
				if(!CONSTANTS.resourceEncoder.decodeIfAvailable(zNodeName) &&
						zkClient.getZNodeData(zNodePath).equals(this.id)) {
					String encodedZNodeName = CONSTANTS.resourceEncoder.encodeAsDeallotted(zNodeName);
					zkClient.deleteAll(zNodePath);
					zkClient.createNode(pathPrefix + encodedZNodeName, "".getBytes());
				}
				overWatch.elapsedTime("QuitUser|Deallocate");
				zkClient.close();
			}
		}
		//System.out.println("Successfully deallocated resources, exiting.");
	}
	
	public List<String> getAllocationLedger() {
		return allocationLedger;
	}

	public void setAllocationLedger(List<String> allocationLedger) {
		this.allocationLedger = allocationLedger;
	}
	
}
