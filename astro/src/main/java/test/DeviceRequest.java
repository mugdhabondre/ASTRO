package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceRequest {
	String ip, port1, port2,port3;
	List<List<String>> resources = new ArrayList();
	
	public DeviceRequest(String fileName) {
		try {
			File file = new File(fileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			boolean firstLine = true;
			while ((line = bufferedReader.readLine()) != null) {
				String[] splits = line.split(",");
				if (firstLine == true) {
					this.ip = splits[0];
					this.port1 = splits[1];
					this.port2 = splits[2];
					this.port3 = splits[3];
					firstLine = false;
				} else {
					List<String> resource = new ArrayList<String>();
					for(String each: splits) {
						resource.add(each);
					}
					resources.add(resource);
				}
			}
			fileReader.close();
		} catch (IOException e) {
			System.out.println("File IO failed.\n Error:\n" + e.getMessage());
		}

	}
	
}
