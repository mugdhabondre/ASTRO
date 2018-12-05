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
	
	//device join
	public int joinDevice(String ip, String peerPort, String leaderPort, String clientPort) throws Exception {
		//----prep to start ZK----
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String existingDynamicConfig = zkClient.getConfig();
		System.out.println("Existing Config: \n" +  existingDynamicConfig);
		
		//get free id and create new ZK and dynamic configs
		int id = getFreeIdFromConfig(existingDynamicConfig);
		String zkConfig = createZKConfig(id);
		String serverAddress = "server." + id + "=" + ip + ":" + peerPort + ":" + leaderPort + ";" + clientPort;
		String newDynamicConfig = getNewDynamicConfig(existingDynamicConfig, serverAddress);
		System.out.println("Updated Config by manually adding new server: \n" +  newDynamicConfig);
		
		//create data_id directory and add required files
		String folder = CONSTANTS.zkDir + "data_" + id;
		new File(folder).mkdirs();
		writeToFile(zkConfig, folder + "/zoo.cfg");
		writeToFile(String.valueOf(id), folder + "/myid");
		writeToFile(newDynamicConfig, folder + "/zoo_replicated" + id + ".cfg.dynamic");
		//----end of prep to start ZK----
		
		//----actual device join----
		// 1. start zk server
		String result = execZKServerCommand(CONSTANTS.zkserver + " start " + folder + "/zoo.cfg");
		if(result.contains("FAILED TO START")) {
			System.out.println("Could not start a new server!");
			System.exit(0);
		}
		
		//2. update configs in existing ensemble
		newDynamicConfig = zkClient.addServerToEnsemble(serverAddress);
		System.out.println("Updated Config after adding a new server: \n" +  newDynamicConfig);
		
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
		Resource newResource = CONSTANTS.utils.createResourceObject(ip,port,resourceType,readOnly,props);
		String encodedResource = CONSTANTS.resourceEncoder.encode(newResource);
	
		ZKConnection zkClient = new ZKConnection();
		
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + resourceType;
		
		zkClient.createNode(pathPrefix + "/" + encodedResource, "".getBytes());
		System.out.println("Added resource successfully");
		
		zkClient.close();
	}
	
	//remove resource
	public void removeResource(String ip, String port, String resourceType, String props, boolean force) throws Exception {
		removeResource(ip, port, resourceType, false, props, force);
	}
	
	//remove resource
	public void removeResource(String ip, String port, String resourceType, boolean readOnly, String props, boolean force) throws Exception {
		System.out.println("-----Inside single resource removal-----");
		Resource resource = CONSTANTS.utils.createResourceObject(ip, port, resourceType, readOnly, props);
		String encodedResource = CONSTANTS.resourceEncoder.encode(resource);
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		String pathPrefix = "/" + resource.getResourceType();
		
		List<String> children = zkClient.getChildren(pathPrefix);
		for(String child: children) {
			if(child.startsWith(encodedResource.substring(0, encodedResource.length()-1))) {
				System.out.println("Trying to remove resource: " + child);
				String path = pathPrefix + "/" + child;
				if(zkClient.getZNodeData(path).equals("") || force) {
					zkClient.deleteAll(path);
					System.out.println("\t Resource removed successfully");
				} else {
					System.out.println("\t Someone is using the resource. Try again later.");
				}
				break;
			}
		}
		zkClient.close();
		System.out.println("-----Finished single resource removal-----");
	}
	
	//device leave
	public void leaveDevice(String ip, String clientPort, boolean force) throws Exception {
		ZKConnection zkClient = new ZKConnection();
		zkClient.connect(CONSTANTS.host);
		
		//1. remove all resources
		String address =  ip + ":" + clientPort;
		boolean allRemoved = removeResources(zkClient, address, force);
		if(allRemoved) {
			System.out.println("Removed all resources for " + ip + ":" + clientPort);		
			
			//2. update configs to remove device from ensemble 
			String id = findServerIdFromConfig(zkClient.getConfig(), address);
			String newConfig = zkClient.removeServerFromEnsemble(id);
			System.out.println("Updated Config after removing the server: \n" +  newConfig);
			//3. stop zk server
			stopZKServer(id);
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
//		System.out.println("Existing Config: \n" +  existingDynamicConfig);
		
		//get list of ip addresses
		List<String> devicesInEnsemble = getServersFromConfig(existingDynamicConfig);
		
		//ping each ip
		List<String> crashedDevices = new ArrayList();
		for(String device: devicesInEnsemble) {
			String[] split = device.split(":");
			String command = "echo ruok | nc " + split[0] + " " + split[1];
			String result = execZKServerCommand(command);
			if(result.equals(""))
				crashedDevices.add(device);
			else
				assert(result.equals("imok"));
		}
		
		//if not alive, add to list
		for(String device: crashedDevices) {
			// TODO getconfig each time?
			// find server id from config
			String serverId = findServerIdFromConfig(existingDynamicConfig, device);
			System.out.println("Removing server "+ serverId + " from ensemble...");
			zkClient.removeServerFromEnsemble(serverId);
			String[] ipport = device.split(":");
			removeResources(zkClient, device, true);
		}
		
		zkClient.close();
		
	}
	
	
	//--------------------------SUPPORTING FUNCTIONS ONLY---------------------------------
//	private Watcher getConfigWatcher(ZKConnection zkClient, String ip, String port) {
//		return new Watcher(){
//	        public void process(WatchedEvent e) {	        	
//	            if(e.getType() == EventType.NodeDataChanged) {
//	            	System.out.println("-----Watcher triggered!-----");
//		        	String config = "";
//		        	Stat stat = new Stat();
//		        	try {
//						config = zkClient.getConfig(this, stat);
//			        	doMagic(zkClient, config, ip, port);
//					} catch (Exception e1) {
//						e1.printStackTrace();
//					}
//		        	System.out.println("-----Watcher complete!-----");
//	            }
//	        }
//	    };
//	}
//
//	private void doMagic(ZKConnection zkClient, String config, String ip, String port) throws Exception {
//		//get children from /devices znode - set A
//		List<String> devicesInZNode = zkClient.getChildren("/devices");
//		
//		//get the servers from config - set B
//		List<String> devicesInEnsemble = getServersFromConfig(config);
//		
//		//do set A-B - set C
//		devicesInZNode.removeAll(devicesInEnsemble);
//		
//		//if set C has devices, for each check if /devices still has it, remove it and its resources if yes
//		if(devicesInZNode.size()>0) {
//			System.out.println("Crashed devices found. Num: " + devicesInZNode.size());
//			for(String device : devicesInZNode) {
//				System.out.println("\t Trying to clean up device: " + device);
//				if(zkClient.deleteNode("/devices/" + device)) {
//					//remove all resources
//					removeResources(zkClient, device, true);
//					System.out.println("\t Removed all resources for " + device);
//				}
//			}
//		} else {
//			System.out.println("No crashed devices found. It's all good, yo!");
//		}
//	}
	
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
		
//		System.out.println("Printing command execution output");
		StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null, previous = null;
        while ((line = br.readLine()) != null)
            if (!line.equals(previous)) {
                previous = line;
                out.append(line).append('\n');
//                System.out.println(line);
            }

//        //Check result
//        if (process.waitFor() == 0) {
//            System.out.println("Success!");
//        }
        
        return out.toString();
        
	}
	
	
	private boolean removeResources(ZKConnection zkClient, String address, boolean force) throws Exception {
		boolean allRemoved = true;
		System.out.println("-----Inside resource removal-----");
		for(String resourcePath: CONSTANTS.resourcePaths) {
			List<String> children = zkClient.getChildren(resourcePath);
			for(String child: children) {
				if(CONSTANTS.resourceEncoder.decodeAddress(child).equals(address)) {
					System.out.println("Trying to remove resource: " + child);
					String path = resourcePath + "/" + child;
					// data will always be "" for readOnly resources
					if(zkClient.getZNodeData(path).equals("") || force) {
						zkClient.deleteAll(path);
						System.out.println("\t Resource removed successfully");
					} else {
						System.out.println("\t Someone is using the resource. Try again later.");
						allRemoved = false;
					}
				}
			}
		}
		System.out.println("-----Finished resource removal-----");
		return allRemoved;
	}
	
	private void stopZKServer(String id) throws IOException, InterruptedException {
		String folder = CONSTANTS.zkDir + "data_" + id;
		execZKServerCommand( CONSTANTS.zkserver + " stop " + folder + "/zoo.cfg");
		System.out.println("Stopped ZK server");
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
