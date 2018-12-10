package test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeviceTest {

	int numThreads = 3;
	Thread[] threads = new Thread[numThreads];
	List<DeviceRequest> deviceRequests = new ArrayList();
	
	private void createDeviceRequestObjects() {
		File folder = new File("/Users/mugdha_bondre/Documents/Fall_2018/DC/project/ASTRO/astro/deviceRequests");
		File[] listOfFiles = folder.listFiles();
		for(File file: listOfFiles) {
			if(file.isFile() && !file.isHidden()) {
				DeviceRequest deviceRequest = new DeviceRequest(file.getPath());
				deviceRequests.add(deviceRequest);
			}
		}
	}
	
	public void start() throws InterruptedException {
		createDeviceRequestObjects();
		for(int i=0; i < numThreads; i++) {
		    DeviceRequestThread deReqThread = new DeviceRequestThread(deviceRequests.get(i), "/var/lib/zookeeper/analysis/DeviceLatencyMicro_"+i+".txt");
		     threads[i] = new Thread(deReqThread);
		}
		
		for (int i = 0; i < threads.length; i++) {
			Thread.sleep((long)(Math.random() * 100));
			threads[i].start();
		}
	}
	
	public void stop() throws InterruptedException {
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
	}
}

