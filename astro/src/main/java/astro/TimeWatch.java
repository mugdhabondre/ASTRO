package astro;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TimeWatch {
    long starts;
    String fileName;
    
    public TimeWatch(String FileName) {
    	this.fileName = FileName;
    }

    public TimeWatch() {
    	this.fileName = CONSTANTS.fileName;
    }
    
    public TimeWatch start() {
        return this.reset();
    }
    
    public TimeWatch reset() {
        starts = System.currentTimeMillis();
        return this;
    }

    public long elapsedTime(String key) {
        long ends = System.currentTimeMillis();
        long interval = ends - starts;
        appendStrToFile("\n" + key + ": " + String.valueOf(interval), fileName);
        return interval;
    }

    public long elapsedTime(TimeUnit unit, String key) {
        return unit.convert(elapsedTime(key), TimeUnit.MILLISECONDS);
    }
    
    public static void appendStrToFile(String str, String fileName){ 
		try { 
			// Open given file in append mode. 
			BufferedWriter out = new BufferedWriter( 
				new FileWriter(fileName, true)); 
				out.write(str); 
				out.close(); 
			} 
			catch (IOException e) { 
				System.out.println("exception occoured  while writing to file:" + e); 
			} 
	}
}
