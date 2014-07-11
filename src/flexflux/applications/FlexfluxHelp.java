package flexflux.applications;

import java.io.IOException;
import java.lang.reflect.Field;

import flexflux.applications.gui.GraphicalFlexflux;

public class FlexfluxHelp {
	
	public static String message = "Displays the details of Flexflux functions";
	
	public static void main(String[] args) throws ClassNotFoundException, IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
	
	
		System.out.println("Flexflux Help\n");
		System.out.println("To get more details about a specific function, call it with the argument -h\n");
		
		System.out.println("List of flexflux functions : \n");
	
		
		for (Class c : GraphicalFlexflux.getClasses("flexflux.applications")){
			
			
			System.out.println("- "+c.getSimpleName());
			
			Field f = c.getField("message");
			System.out.println(f.get(null));
			System.out.println("");
			
		}
	
	
	
	
	
	
	
	}
}
