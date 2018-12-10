package astro;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;

public class DeviceManager {
	
	private TimeWatch overWatch;
	
	public DeviceManager(String fileName) {
		this.overWatch = new TimeWatch(fileName);
	}
	
	public DeviceManager() {
		this.overWatch = new TimeWatch(CONSTANTS.fileName);
	}
	
	//device join
	public int joinDevice(String ip, String peerPort, String leaderPort, String clientPort) throws Exception {
		//----prep to start ZK----
		overWatch.reset();
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String existingDynamicConfig = zkClient.getConfig();
		//System.out.println("Existing Config: \n" +  existingDynamicConfig);
		

		//get free id and create new ZK and dynamic configs
		int id = getFreeIdFromConfig(existingDynamicConfig);
		String zkConfig = createZKConfig(id);
		String serverAddress = "server." + id + "=" + ip + ":" + peerPort + ":" + leaderPort + ";" + clientPort;
		String newDynamicConfig = getNewDynamicConfig(existingDynamicConfig, serverAddress);
		//System.out.println("Updated Config by manually adding new server: \n" +  newDynamicConfig);

		
		//create data_id directory and add required files
		String folder = CONSTANTS.zkDir + "data_" + id;
		new File(folder).mkdirs();
		writeToFile(zkConfig, folder + "/zoo.cfg");
		writeToFile(String.valueOf(id), folder + "/myid");
		writeToFile(newDynamicConfig, folder + "/zoo_replicated" + id + ".cfg.dynamic");
		//----end of prep to start ZK----
		overWatch.elapsedTime("JoinDevice|PrepStartZK");
		
		//----actual device join----
		overWatch.reset();
		// 1. start zk server
		String result = execZKServerCommand(CONSTANTS.zkserver + " start " + folder + "/zoo.cfg");
		if(result.contains("FAILED TO START")) {
			System.out.println("Could not start a new server!");
			System.exit(0);
		}
		overWatch.elapsedTime("JoinDevice|StartZK");
		
		overWatch.reset();
		//2. update configs in existing ensemble
		newDynamicConfig = zkClient.addServerToEnsemble(serverAddress);
		//System.out.println("Updated Config after adding a new server: \n" +  newDynamicConfig);
		overWatch.elapsedTime("JoinDevice|ChangeConfig");
		
		//----end actual device join----
		
		zkClient.close();
		
		return id;
	}
	
	//add resource
	public void addResource(String ip, String port, String resourceType, String props) throws Exception {
		addResource(ip, port, resourceType, false, props);
	}
	
	//add resource
	public void addResource(String ip, String port, String resourceType, boolean readOnly, String props) throws Exception {
		
		overWatch.reset();
		Resource newResource = CONSTANTS.utils.createResourceObject(ip,port,resourceType,readOnly,props);
		String encodedResource = CONSTANTS.resourceEncoder.encode(newResource);
		overWatch.elapsedTime("AddResource|ResourceEncoding");
	
		ZKConnection zkClient = new ZKConnection();
		
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + resourceType;
		
		overWatch.reset();
		zkClient.createNode(pathPrefix + "/" + encodedResource, "".getBytes());
		//System.out.println("Added resource successfully");
		overWatch.elapsedTime("AddResource|CreateNode");
		
		zkClient.close();
	}
	
	//remove resource
	public void removeResource(String ip, String port, String resourceType, String props, boolean force) throws Exception {
		removeResource(ip, port, resourceType, false, props, force);
	}
	
	//remove resource
	public void removeResource(String ip, String port, String resourceType, boolean readOnly, String props, boolean force) throws Exception {
		//System.out.println("-----Inside single resource removal-----");
		Resource resource = CONSTANTS.utils.createResourceObject(ip, port, resourceType, readOnly, props);
		String encodedResource = CONSTANTS.resourceEncoder.encode(resource);
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + resource.getResourceType();
		
		List<String> children = zkClient.getChildren(pathPrefix);
		for(String child: children) {
			if(child.startsWith(encodedResource.substring(0, encodedResource.length()-1))) {
				//System.out.println("Trying to remove resource: " + child);
				String path = pathPrefix + "/" + child;
				if(zkClient.getZNodeData(path).equals("") || force) {
					zkClient.deleteAll(path);
					//System.out.println("\t Resource removed successfully");
				} else {
					//System.out.println("\t Someone is using the resource. Try again later.");
				}
				break;
			}
		}
		zkClient.close();
		//System.out.println("-----Finished single resource removal-----");
	}
	
	//device leave
	public void leaveDevice(String ip, String clientPort, boolean force) throws Exception {
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		
		overWatch.reset();
		//1. remove all resources
		String address =  ip + ":" + clientPort;
		boolean allRemoved = removeResources(zkClient, address, force);
		overWatch.elapsedTime("LeaveDevice|removeResources");
		
		if(allRemoved) {
			//System.out.println("Removed all resources for " + ip + ":" + clientPort);		
			
			overWatch.reset();
			//2. update configs to remove device from ensemble 
			String id = findServerIdFromConfig(zkClient.getConfig(), address);
			String newConfig = zkClient.removeServerFromEnsemble(id);
			overWatch.elapsedTime("LeaveDevice|ChangeConfig");
			
			overWatch.reset();
			//System.out.println("Updated Config after removing the server: \n" +  newConfig);
			//3. stop zk server
			stopZKServer(id);
			overWatch.elapsedTime("LeaveDevice|StopZKServer");
		} else {
			System.out.println("Cannot remove device as one or more resources are being used. Please try again later.");
		}
		
		
		zkClient.close();
		
	
	}
		
	// garbage collector
	public void collectGarbage(String ip, String port) throws Exception {
		// create connection
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		
		//get config
		String existingDynamicConfig = zkClient.getConfig();
		System.out.println("Existing Config: \n" +  existingDynamicConfig);
		
		//get list of ip addresses
		List<String> devicesInEnsemble = getServersFromConfig(existingDynamicConfig);
		
		//ping each ip
		List<String> crashedDevices = new ArrayList();
		for(String device: devicesInEnsemble) {
			String[] split = device.split(":");
			String command = "echo ruok | nc " + split[0] + " " + split[1];
			String result = execZKServerCommand(command);
			if(result.equals("")) {
				crashedDevices.add(device);
				System.out.println("Found crashed device: " + device);
			}
			else
				assert(result.equals("imok"));
		}
		
		//if not alive, add to list
		for(String device: crashedDevices) {
			// TODO getconfig each time?
			// find server id from config
			String serverId = findServerIdFromConfig(existingDynamicConfig, device);
			System.out.println("Removing server "+ serverId + " from ensemble...");
			String newConfig = zkClient.removeServerFromEnsemble(serverId);
			System.out.println("New Config after GC: \n" +  newConfig);
			String[] ipport = device.split(":");
			removeResources(zkClient, device, true);
		}
		
		zkClient.close();
		
	}
	
	
	//--------------------------SUPPORTING FUNCTIONS ONLY---------------------------------
	private int getFreeIdFromConfig(String config) {
		String[] lines = config.split(System.getProperty("line.separator"));
		int[] ids = new int[256];
		for(int i=0;i<lines.length-1;i++) {
			String[] split = lines[i].split("=")[0].split("\\.");
			ids[Integer.valueOf(split[1])-1] = 1;
		}
		List<Integer> missingIds = new ArrayList();
		for(int i =0;i<ids.length;i++) {
			if(ids[i]!=1)
				missingIds.add(i+1);
		}
		Random random = new Random();
	    return missingIds.get(random.nextInt(missingIds.size()));
	}
	
	private List<String> getServersFromConfig(String config) {
		List<String> servers = new ArrayList();
		String[] lines = config.split(System.getProperty("line.separator"));
		for(int i=0;i<lines.length-1;i++) {
			String[] split = lines[i].split("=")[1].split(":");
			servers.add(split[0] + ":" + split[split.length-1]);
		}
		return servers;
	}
	
	private String getNewDynamicConfig(String oldConfig, String serverAddress) {
		String[] lines = oldConfig.split(System.getProperty("line.separator"));
		lines[lines.length-1] = serverAddress;
		return String.join(System.getProperty("line.separator"), lines);
	}

	private String createZKConfig(int id) {
		String baseConfig = "maxClientCnxns=60\n" + 
				"reconfigEnabled=true\n" + 
				"dataDir=" + CONSTANTS.zkDir + "data_" + id + "\n" + 
				"syncLimit=5\n" + 
				"initLimit=10\n" + 
				"tickTime=2000\n" + 
				"4lw.commands.whitelist=stat, ruok, conf, isro\n" + 
				"dynamicConfigFile=" + CONSTANTS.zkDir + "data_" + id + "/zoo_replicated" + id + ".cfg.dynamic\n";
		return baseConfig;
	}
	
	private void writeToFile(String contents, String filePath) 
			  throws IOException {
			    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
			    writer.write(contents);
			     
			    writer.close();
			}
	
	private String execZKServerCommand(String command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("bash","-c", command);
		Process process = pb.start();
		
////		System.out.println("Printing command execution output");
		StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null, previous = null;
        while ((line = br.readLine()) != null)
            if (!line.equals(previous)) {
                previous = line;
                out.append(line).append('\n');
            }

//        //Check result
//        if (process.waitFor() == 0) {
//            System.out.println("Success!");
//        }
        
        return out.toString();
        // return "";
        
	}
	
	
	private boolean removeResources(ZKConnection zkClient, String address, boolean force) throws Exception {
		boolean allRemoved = true;
		//System.out.println("-----Inside resource removal-----");
		for(String resourcePath: CONSTANTS.resourcePaths) {
			List<String> children = zkClient.getChildren(resourcePath);
			for(String child: children) {
				if(CONSTANTS.resourceEncoder.decodeAddress(child).equals(address)) {
					//System.out.println("Trying to remove resource: " + child);
					String path = resourcePath + "/" + child;
					// data will always be "" for readOnly resources
					if(zkClient.getZNodeData(path).equals("") || force) {
						zkClient.deleteAll(path);
						//System.out.println("\t Resource removed successfully");
					} else {
						//System.out.println("\t Someone is using the resource. Try again later.");
						allRemoved = false;
					}
				}
			}
		}
		//System.out.println("-----Finished resource removal-----");
		return allRemoved;
	}
	
	private void stopZKServer(String id) throws IOException, InterruptedException {
		String folder = CONSTANTS.zkDir + "data_" + id;
		execZKServerCommand( CONSTANTS.zkserver + " stop " + folder + "/zoo.cfg");
		//System.out.println("Stopped ZK server");
	}

	private String findServerIdFromConfig(String config, String device ) {
		String[] lines = config.split(System.getProperty("line.separator"));
		for(int i=0;i<lines.length-1;i++) {
			String[] split = lines[i].split("=");
			String[] ipPorts = split[1].split(":");
			if(device.equals(ipPorts[0] + ":" +  ipPorts[ipPorts.length-1]))
				return split[0].split("\\.")[1];
		}
		return null;
	}
}
