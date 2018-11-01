package astro;

public class User {
	String id;
	
	public User(String  id){
		this.id =  id; // id?
	}
	
	public void start() {
		// Get request from the user
		// encode request
		// call resource request
		// call resource connect till you get a connection.
		// call quit function
	}
	
	/*
	 * Resource Request phase
	 * */
	public String resourceRequest() {
		// Start ZK client and get ZNode structure
		// Get ceiling of user requirements
		// Return Ip, port of resource 
		return "";
	}
	
	/*
	 * Resource Connect Phase
	 * */
	public boolean resourceConnect(String ipport) {
		// Reconnect to the resource node
		// Atomic give the resource and change znode
		return true;
	}
	
	
	public void quit() {
		// Atomic deallocate and change znode
		// disconnect with the server
	}
	
	
}
