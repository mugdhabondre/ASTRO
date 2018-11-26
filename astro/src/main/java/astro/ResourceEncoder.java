package astro;

public class ResourceEncoder {
	
	public String encode(Resource resource) {
		String encoded = "";
		for(ResourceProperty prop: resource.getResourceProps()) {
		encoded += String.valueOf(prop.getCapacity()) + "|";
		}
		// Add ip and port to the encoded string
		encoded += resource.getResourceIP() + ":" + resource.getResourcePort()  + "|";
		// Add read only flag
		encoded +=  resource.getReadOnlyFlag()  ? "1" + "|" : "0" + "|";
		// Add isAllocated = false
		encoded += "0";
		
		return encoded; 
	}
	
	public String decodeAddress(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		return splits[splits.length - 3]; //ip:port
	}
	
	public boolean decodeReadOnly(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		return splits[splits.length - 2].equals("1"); //read only bit
	}
	
	public int decodePropertyValue(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		return Integer.valueOf(splits[0]);
	}
	
	public boolean decodeIfAvailable(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		if (splits[splits.length - 1].equals("0") || decodeReadOnly(resourceCode)) {
			return true;
		}
		return false;
	}
	
	public String encodeAsAllotted(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		splits[splits.length - 1] = "1";
		resourceCode = String.join("|", splits);
		return resourceCode;
	}
	
	public String encodeAsDeallotted(String resourceCode) {
		String[] splits = resourceCode.split("\\|");
		splits[splits.length - 1] = "0";
		resourceCode = String.join("|", splits);
		return resourceCode;
	}
}
