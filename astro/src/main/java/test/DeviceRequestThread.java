package test;

import java.util.List;

import astro.DeviceManager;

public class DeviceRequestThread implements Runnable {
	DeviceRequest deReq;
	
	DeviceRequestThread(DeviceRequest deReq) {
		this.deReq  = deReq;
	}
	
	@Override
	public void run() {
	 try {
			DeviceManager deMan = new DeviceManager();
			DeviceRequest deReq = this.deReq;
			deMan.joinDevice(deReq.ip, deReq.port1, deReq.port2, deReq.port3);
			for(List<String> resource: deReq.resources)
				deMan.addResource(deReq.ip, deReq.port3, resource.get(0), Boolean.valueOf(resource.get(1)), resource.get(2));
			
		 Thread.sleep(6000);
		 deMan.leaveDevice(deReq.ip, deReq.port3, true);
	 }
	 catch (Exception e) {
		 e.printStackTrace();
		 System.out.println("Garbage collector stopped. Retrying...");
	 }
    }

}
