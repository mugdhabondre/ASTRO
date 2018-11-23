package astro;

public class CONSTANTS {
	public static int zkSessionTimeout = 40000;
	public static String host = "127.0.0.1:2181";
	public static String[] resourcePaths = new String[] {"/storage","/compute","/network"};
	public static ResourceEncoder resourceEncoder = new ResourceEncoder();
}
