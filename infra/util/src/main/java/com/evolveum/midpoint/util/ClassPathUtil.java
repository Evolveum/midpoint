/**
 * 
 */
package com.evolveum.midpoint.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Radovan Semancik
 *
 */
public class ClassPathUtil {
	
	public static Set<Class> listClasses(String packageName) throws IOException, ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Set<Class> classes = new HashSet<Class>();

		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        
        for (File directory : dirs) {
            addAllClasses(directory, packageName, classes);
        }
		return classes;
	}
	
	private static void addAllClasses(File directory, String packageName, Set<Class> classes) throws ClassNotFoundException {
		if (!directory.exists()) {
        	return;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                addAllClasses(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
    }


}
