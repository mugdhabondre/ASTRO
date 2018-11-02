package astro;
import java.util.List;
import java.util.Scanner;

public class User {
	String id;
	private static ResourceEncoder resourceEncoder = new ResourceEncoder();
	String host = "";
	
	public User(String  id){
		this.id =  id; // id?
	}
	
	public void start() {
		// Get request from the user
		Scanner reader = new Scanner(System.in);
		System.out.println("Enter the requested resources: 1. Storage 2. Network 3. Compute");
		String request = reader.next();
		System.out.println("You entered: " + request);
		reader.close();
		
		//for each request, we need to execute the following steps - use threads?
		
		/* not needed
		// encode request
		//String encodedRequest = resourceEncoder.encode(request);
		*/
		
		// call resource request
		//String address = resourceRequest(resourceType, propertyValue);
		
		// call resource connect till you get a connection.
		
		
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
		List<String> children = zkClient.getChildren("/" + resourceType);
		
		// Get ceiling of user requirements
		String address = "";
		int currValue = Integer.MAX_VALUE;
		for(String child:children) {
			if(resourceEncoder.decodeIfAvailable(child)) {
				int decodedValue = resourceEncoder.decodePropertyValue(child); 
				if(decodedValue == propertyValue) {
					address = resourceEncoder.decode(child);
					break;
				} else if(decodedValue > propertyValue && decodedValue < currValue) {
					address = resourceEncoder.decode(child);
					currValue = decodedValue;
				}
			}
		}
		zkClient.close();
		
		// Return Ip, port of resource 
		return address;
	}
	
	/*
	 * Resource Connect Phase
	 * */
	public boolean resourceConnect(String address) {
		// Reconnect to the resource node
		// Atomic give the resource and change znode
		return true;
	}
	
	
	public void quit() {
		// Atomic deallocate and change znode
		// disconnect with the server
	}
	
	
}
