package astro;

public class GarbageCollectorThread implements Runnable {
	boolean exit = false;
	DeviceManager deMan;
	String ip="127.0.0.1", port="2181";
	
	public GarbageCollectorThread(DeviceManager deviceManager) {
		this.deMan =  deviceManager; 
	}
	
    public void run() {
        while(!exit) {
	       	 try {
	       		Thread.sleep((long)(Math.random() * 6000));
	       		 deMan.collectGarbage(ip, port);
	       		 System.out.println("Garbage Collector running...");
	       	 }
	       	 catch (Exception e) {
	       		 e.printStackTrace();
	       		 System.out.println("Garbage collector stopped. Retrying...");
	       	 }
        }
    }
    
    public void stop() {
   	 exit = true;
    }
}
