package astro;

import java.util.ArrayList;
import java.util.List;

public class Utils {
	public static Resource createResourceObject(String ip, String port, String resourceType, boolean readOnly, String props) {
		String[] propSplit = props.split(" ");
		List<ResourceProperty> properties = new ArrayList();
		for(String propString: propSplit) {
			ResourceProperty property = new ResourceProperty();
			property.setPropType(propString.split(":")[0]);
			property.setCapacity(Integer.parseInt(propString.split(":")[1]));
			properties.add(property);
		}
		//default readOnly = False
		Resource newResource = new Resource(resourceType, ip, port, readOnly, properties);
		return newResource;
	}
}
