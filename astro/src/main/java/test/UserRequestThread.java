package test;

import java.util.List;

import astro.CONSTANTS;
import astro.User;
import astro.UserRequest;

public class UserRequestThread implements Runnable {
	private final int i;
	private final List<UserRequest> requests;
	
	UserRequestThread(int i, List<UserRequest> requests) {
		this.i = i;
		this.requests = requests;
	}
	
	@Override
	public void run() {
		User user = new User(String.valueOf(this.i), CONSTANTS.userFileName.split("\\.txt")[0]+ "_" + i + ".txt");
		try {
			System.out.println("Starting User_"+i+" Thread");
			user.start(this.requests);
			Thread.sleep(15000);
			List<String> ledger = user.getAllocationLedger();
			System.out.println("User_"+i+ " resources:");
			for(int j = 0; j < ledger.size(); j++) {
				System.out.println(ledger.get(j));
			}
			System.out.println("Quiting User_"+i+" Thread");
			user.quit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Unable to start User_"+i);
		}
	}

}
