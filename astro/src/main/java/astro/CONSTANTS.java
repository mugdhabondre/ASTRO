package astro;

public class CONSTANTS {
	public static int zkSessionTimeout = 40000;
	public static String host = "127.0.0.1:2181";
	public static String[] resourcePaths = new String[] {"/storage","/compute","/network"};
	public static ResourceEncoder resourceEncoder = new ResourceEncoder();
	public static Utils utils = new Utils();
	public static String zkserver = "/Users/mugdha_bondre/Documents/Fall_2018/DC/project/ASTRO/zookeeper-3.5.4-beta/bin/zkServer.sh";
	public static String zkDir = "/var/lib/zookeeper/";
	public static String fileName = "/var/lib/zookeeper/DeviceLatency.txt";
	
}
