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
		String existingDynamicConfig = zkClient.getConfig(getConfigWatcher(zkClient, ip, clientPort));
		System.out.println("Existing Config: \n" +  existingDynamicConfig);
		
		//get free id and create new ZK and dynamic configs
		int id = getFreeIdFromConfig(existingDynamicConfig);
		String zkConfig = createZKConfig(id);
		String serverAddress = "server." + id + "=" + ip + ":" + peerPort + ":" + leaderPort + ";" + clientPort;
		String newDynamicConfig = getNewDynamicConfig(existingDynamicConfig, serverAddress);
		System.out.println("Updated Config by manually adding new server: \n" +  newDynamicConfig);
		
		//create data_id directory and add required files
		String folder = "/var/zookeeper/data_" + id;
		new File(folder).mkdirs();
		writeToFile(zkConfig, folder + "/zoo.cfg");
		writeToFile(String.valueOf(id), folder + "/myid");
		writeToFile(newDynamicConfig, folder + "/zoo_replicated" + id + ".cfg.dynamic");
		//----end of prep to start ZK----
		
		//----actual device join----
		// 1. start zk server
		execZKServerCommand(folder + "/zoo.cfg", "start");
		
		//2. update configs in existing ensemble
		newDynamicConfig = zkClient.addServerToEnsemble(serverAddress);
		System.out.println("Updated Config after adding a new server: \n" +  newDynamicConfig);
		
		//3. add to /devices znode
		zkClient.createNode("/devices/" + ip + ":" + clientPort, String.valueOf(id).getBytes());
		System.out.println("Added device to /devices");
		//----end actual device join----
		
		zkClient.close();
		
		return id;
	}
	
	//add resource
	public void addResource(String ip, String port, String resourceType, String props) throws Exception {
		Resource newResource = createResourceObject(ip,port,resourceType,props);
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
		System.out.println("-----Inside single resource removal-----");
		Resource resource = createResourceObject(ip, port, resourceType, props);
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
					zkClient.deleteNode(path);
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
		boolean allRemoved = removeResources(zkClient, ip, clientPort, force);
		if(allRemoved) {
			System.out.println("Removed all resources for " + ip + ":" + clientPort);
			
			//2. remove from device znode
			String devicePath = "/devices/" + ip + ":" + clientPort;
			int id = Integer.valueOf((String)zkClient.getZNodeData(devicePath));
			zkClient.deleteNode(devicePath);
			System.out.println("Deleted node from /devices");
			
			//3. update configs to remove device from ensemble 
			String newConfig = zkClient.removeServerFromEnsemble(String.valueOf(id));
			System.out.println("Updated Config after removing the server: \n" +  newConfig);
			//4. stop zk server
			stopZKServer(id);
		} else {
			System.out.println("Cannot remove device as one or more resources are being used. Please try again later.");
		}
		
		zkClient.close();
		
	
	}
		
	//--------------------------SUPPORTING FUNCTIONS ONLY---------------------------------
	private Watcher getConfigWatcher(ZKConnection zkClient, String ip, String port) {
		return new Watcher(){
	        public void process(WatchedEvent e) {	        	
	            if(e.getType() == EventType.NodeDataChanged) {
	            	System.out.println("-----Watcher triggered!-----");
		        	String config = "";
		        	Stat stat = new Stat();
		        	try {
						config = zkClient.getConfig(this, stat);
			        	doMagic(zkClient, config, stat);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
		        	System.out.println("-----Watcher complete!-----");
	            }
	        }

			private void doMagic(ZKConnection zkClient, String config, Stat stat) throws Exception {
				//get children from /devices znode - set A
				List<String> devicesInZNode = zkClient.getChildren("/devices");
				
				//get the servers from config - set B
				List<String> devicesInEnsemble = getServersFromConfig(config);
				
				//do set A-B - set C
				devicesInZNode.removeAll(devicesInEnsemble);
				
				//if set C has devices, for each check if /devices still has it, remove it and its resources if yes
				if(devicesInZNode.size()>0) {
					System.out.println("Crashed devices found. Num: " + devicesInZNode.size());
					for(String device : devicesInZNode) {
						System.out.println("\t Trying to clean up device: " + device);
						if(zkClient.deleteNode("/devices/" + device)) {
							//remove all resources
							removeResources(zkClient, ip, port, true);
							System.out.println("\t Removed all resources for " + device);
						}
					}
				} else {
					System.out.println("No crashed devices found. It's all good, yo!");
				}
			}
	    };
	}

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
				"dataDir=/var/zookeeper/data_" + id + "\n" + 
				"syncLimit=5\n" + 
				"initLimit=10\n" + 
				"tickTime=2000\n" + 
				"4lw.commands.whitelist=stat, ruok, conf, isro" + 
				"dynamicConfigFile=/var/zookeeper/data_" + id + "/zoo_replicated" + id + ".cfg.dynamic";
		return baseConfig;
	}
	
	private void writeToFile(String contents, String filePath) 
			  throws IOException {
			    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
			    writer.write(contents);
			     
			    writer.close();
			}

	private Resource createResourceObject(String ip, String port, String resourceType, String props) {
		String[] propSplit = props.split(" ");
		List<ResourceProperty> properties = new ArrayList();
		for(String propString: propSplit) {
			ResourceProperty property = new ResourceProperty();
			property.setPropType(propString.split(":")[0]);
			property.setCapacity(Integer.parseInt(propString.split(":")[1]));
			properties.add(property);
		}
		
		Resource newResource = new Resource(resourceType, ip, port, properties);
		return newResource;
	}
	
	private void execZKServerCommand(String configPath, String command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("bash","-c","/media/meghana/5E6AE6FA1846AC9C/GaTech/Fall2018/DisCo/Project/ASTRO/zookeeper-3.5.4-beta/bin/zkServer.sh "
					+ command + " " + configPath);
		Process process = pb.start();
		
		System.out.println("Printing command execution output");
		StringBuilder out = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null, previous = null;
        while ((line = br.readLine()) != null)
            if (!line.equals(previous)) {
                previous = line;
                out.append(line).append('\n');
                System.out.println(line);
            }

        //Check result
        if (process.waitFor() == 0) {
            System.out.println("Success!");
        }
        
	}
	
	private boolean removeResources(ZKConnection zkClient, String ip, String port, boolean force) throws Exception {
		boolean allRemoved = true;
		String address = ip + ":" + port;
		System.out.println("-----Inside resource removal-----");
		for(String resourcePath: CONSTANTS.resourcePaths) {
			List<String> children = zkClient.getChildren(resourcePath);
			for(String child: children) {
				if(CONSTANTS.resourceEncoder.decodeAddress(child).equals(address)) {
					System.out.println("Trying to remove resource: " + child);
					String path = resourcePath + "/" + child;
					if(zkClient.getZNodeData(path).equals("") || force) {
						zkClient.deleteNode(path);
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
	
	private void stopZKServer(int id) throws IOException, InterruptedException {
		String folder = "/var/zookeeper/data_" + id;
		execZKServerCommand(folder + "/zoo.cfg", "stop");
	}

}
