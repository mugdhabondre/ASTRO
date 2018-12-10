package test;

import java.util.*;

import astro.UserRequest;

public class UserTest {
	int numThreads = 3;
	Thread[] threads = new Thread[numThreads];
	List<List<UserRequest>> allRequests = new ArrayList<>();
	
	public void createRequestsforUser() {
		// 1
		List<UserRequest> reqList1 = new ArrayList<>();
		UserRequest req = new UserRequest("storage", false, 10);
		reqList1.add(req);
		allRequests.add(reqList1);
		
		// 2
		List<UserRequest> reqList2 = new ArrayList<>();
		UserRequest req1 = new UserRequest("storage", false, 10);
		// UserRequest req2 = new UserRequest("storage", true, 100);
		reqList2.add(req1);
		// reqList2.add(req2);
		allRequests.add(reqList2);
		
		// 3
		List<UserRequest> reqList3 = new ArrayList<>();
		UserRequest req3 = new UserRequest("storage", false, 10);
		// UserRequest req4 = new UserRequest("network", false, 20);
		// UserRequest req5 = new UserRequest("storage", false, 10);
		reqList3.add(req3);
		// reqList3.add(req4);
		// reqList3.add(req5);
		allRequests.add(reqList3);	
		
	}
	
	public void start() {
		createRequestsforUser();
		for(int i=0; i < numThreads; i++) {
		    UserRequestThread tempThread = new UserRequestThread(i, allRequests.get(i));
		    threads[i] = new Thread(tempThread);
		}
		
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}
	}
	
	public void stop() throws InterruptedException {
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}
	}
}

