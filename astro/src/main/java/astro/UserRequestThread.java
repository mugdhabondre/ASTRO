package astro;

import java.util.List;

public class UserRequestThread implements Runnable {
	private final int i;
	private final List<UserRequest> requests;
	
	UserRequestThread(int i, List<UserRequest> requests) {
		this.i = i;
		this.requests = requests;
	}
	
	@Override
	public void run() {
		User user = new User(String.valueOf(this.i));
		try {
			user.start(this.requests);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Unable to start user"+i);
		}
	}

}
