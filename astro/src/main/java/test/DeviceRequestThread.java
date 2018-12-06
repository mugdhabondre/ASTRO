package test;

import java.util.List;

import astro.DeviceManager;
import astro.TimeWatch;

public class DeviceRequestThread implements Runnable {
	DeviceRequest deReq;
	String fileName;
	
	
	DeviceRequestThread(DeviceRequest deReq, String fileName) {
		this.deReq  = deReq;
		this.fileName = fileName;
	}
	
	@Override
	public void run() {
	 try {
//		 	TimeWatch overWatch = new TimeWatch();
			DeviceManager deMan = new DeviceManager(fileName);
			DeviceRequest deReq = this.deReq;
			
			System.out.println("Joining Device");
//			overWatch.reset();
			deMan.joinDevice(deReq.ip, deReq.port1, deReq.port2, deReq.port3);
//			overWatch.elapsedTime("JoinDevice");
			
			System.out.println("Adding Resource");
			for(List<String> resource: deReq.resources) {
//				overWatch.reset();
				deMan.addResource(deReq.ip, deReq.port3, resource.get(0), Boolean.valueOf(resource.get(1)), resource.get(2));
//				overWatch.elapsedTime("AddResource");
			}
			
		 Thread.sleep(6000);
		 
		 System.out.println("Leaving Device");
//		 overWatch.reset();
		 deMan.leaveDevice(deReq.ip, deReq.port3, true);
//		 overWatch.elapsedTime("LeaveDevice");
	 }
	 catch (Exception e) {
		 System.out.println("Device Manager Thread stopped. Retrying...");
		 e.printStackTrace();
	 }
    }

}
