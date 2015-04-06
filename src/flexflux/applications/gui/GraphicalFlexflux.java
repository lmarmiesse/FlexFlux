/**
 * 21 juin 2013 
 */
package flexflux.applications.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import flexflux.applications.FlexfluxTest;
import flexflux.gui.MainFrame;

/**
 * 
 * Main class for FlexFlux GUI
 * 
 * @author lmarmiesse 21 juin 2013
 * 
 */
public class GraphicalFlexflux {

	public static void main(String[] args) {

		FlexfluxTest.doUnitTests = false;
		FlexfluxTest.main(args);

		try {
			new MainFrame(FlexfluxTest.okSolvers,
					getClasses("flexflux.applications"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Scans all classes accessible from the context class loader which belong
	 * to the given package.
	 * 
	 * @param packageName
	 *            The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static List<Class<?>> getClasses(String packageName)
			throws ClassNotFoundException, IOException {

		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');

		Enumeration<URL> resources = classLoader.getResources(path);

		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();

			try {
				JarURLConnection urlcon = (JarURLConnection) (resource
						.openConnection());

				JarFile jar = urlcon.getJarFile();

				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					String entry = entries.nextElement().getName();

					if (entry.contains("applications/Flex")) {

						entry = entry.substring(0,
								entry.length() - ".class".length());

						classes.add(Class.forName(entry.replace('/', '.')));

					}
				}
			} catch (java.lang.ClassCastException e) {

				classes.addAll(findClasses(
						new File(URLDecoder.decode(resource.getFile(), "UTF-8")),
						packageName));
			}

		}

		ArrayList<Class<?>> orderedClasses = new ArrayList<Class<?>>();
		// change order*
		int maxOrder = 0;
		for (Class c : classes) {
			Field f = null;
			int orderValue = -1;
			try {
				f = c.getField("order");
				orderValue = (int) f.get(null);
			} catch (NoSuchFieldException | SecurityException
					| IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			if (orderValue > maxOrder) {
				maxOrder = orderValue;
			}
		}

		for (int i = 0; i <= maxOrder; i++) {
			for (Class c : classes) {
				Field f = null;
				int orderValue = -1;
				try {
					f = c.getField("order");
					orderValue = (int) f.get(null);
				} catch (NoSuchFieldException | SecurityException
						| IllegalArgumentException | IllegalAccessException e) {
				}
				if (orderValue == i) {
					orderedClasses.add(c);
				}
			}

		}

		for (Class c : classes) {
			if (!orderedClasses.contains(c) && !c.getSimpleName().equals("") ) {
				orderedClasses.add(c);
			}

		}

		//
		return orderedClasses;

	}

	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirs.
	 * 
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class<?>> findClasses(File directory, String packageName)
			throws ClassNotFoundException {

		List<Class<?>> classes = new ArrayList<Class<?>>();

		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {

			if (file.isDirectory()) {
			} else if (file.getName().endsWith(".class")
					&& file.getName().contains("Flexflux")) {

				classes.add(Class.forName(packageName
						+ '.'
						+ file.getName().substring(0,
								file.getName().length() - 6)));
			}
		}
		return classes;
	}
}
