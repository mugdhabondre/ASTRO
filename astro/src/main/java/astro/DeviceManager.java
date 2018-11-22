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

public class DeviceManager {
	String host = "127.0.0.1:2181";
	Device[] devices = new Device[255];
	
	//device join
	public int joinDevice(String ip, String peerPort, String leaderPort, String clientPort) throws IOException, InterruptedException, KeeperException {
		
		//client, get config
		ZKConnection zkClient = new ZKConnection();
		
		//for the purposes of this project, assume there is always one stable host.
		zkClient.connect(host);
		String existingConfig = zkClient.getConfig();
		System.out.println("Existing Config: \n" +  existingConfig);
		
		//get free id and create new config
		int id = getFreeId(existingConfig);
		String serverAddress = "server." + id + "=" + ip + ":" + peerPort + ":" + leaderPort + ";" + clientPort;
		String newConfig = getNewConfig(existingConfig, serverAddress);
		System.out.println("Updated Config by manually adding new server: \n" +  newConfig);
		
		//start a new ZK at ip:port with new config
		String zkConfig = createZKConfig(id);
		
		//create data_id directory
		String folder = "/var/zookeeper/data_" + id;
		new File(folder).mkdirs();
		writeToFile(zkConfig, folder + "/zoo.cfg");
		writeToFile(String.valueOf(id), folder + "/myid");
		writeToFile(newConfig, folder + "/zoo_replicated" + id + ".cfg.dynamic");
		
		//start zk server
		execZKServerCommand(folder + "/zoo.cfg", "start");
		
		//update configs in existing ensemble
		newConfig = zkClient.addServerToEnsemble(serverAddress);
		System.out.println("Updated Config after adding a new server: \n" +  newConfig);
		
		Device device = new Device(ip, clientPort);
		devices[id-1] = device;
		zkClient.close();
		
		return id;
	}
	
	//device leave
	public void leaveDevice(int id) throws IOException, InterruptedException, KeeperException {
		//client, get config
		ZKConnection zkClient = new ZKConnection();
		
		//for the purposes of this project, assume there is always one stable host.
		zkClient.connect(host);
		
		String newConfig = zkClient.removeServerFromEnsemble(String.valueOf(id));
		System.out.println("Updated Config after removing the server: \n" +  newConfig);
		
		zkClient.close();
		
		stopZKServer(id);
	}
	
	private void stopZKServer(int id) throws IOException, InterruptedException {
		String folder = "/var/zookeeper/data_" + id;
		execZKServerCommand(folder + "/zoo.cfg", "stop");
	}
	
	public Device getDevice(int id) {
		return devices[id-1];
	}
	
	//utils
	private int getFreeId(String config) {
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
	
	private String getNewConfig(String oldConfig, String serverAddress) {
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
				"dynamicConfigFile=/var/zookeeper/data_" + id + "/zoo_replicated" + id + ".cfg.dynamic";
		return baseConfig;
	}
	
	private void writeToFile(String contents, String filePath) 
			  throws IOException {
			    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
			    writer.write(contents);
			     
			    writer.close();
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
}
