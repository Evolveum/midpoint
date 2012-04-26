/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author semancik
 *
 */
public class MiscUtil {
	
	private static final int BUFFER_SIZE = 2048; 
	
	public static <T> Collection<T> union(Collection<T>... sets) {
		Set<T> resultSet = new HashSet<T>();
		for (Collection<T> set: sets) {
			if (set != null) {
				resultSet.addAll(set);
			}
		}
		return resultSet;
	}
	
	public static boolean unorderedCollectionEquals(Collection a, Collection b) {
		Comparator<?> comparator = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return o1.equals(o2) ? 0 : 1;
			}
		};
		return unorderedCollectionEquals(a, b, comparator);
	}
	
	/**
	 * Only zero vs non-zero value of comparator is important. 
	 */
	public static boolean unorderedCollectionEquals(Collection a, Collection b, Comparator comparator) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		Collection outstanding = new ArrayList(b.size());
		outstanding.addAll(b);
		for (Object ao: a) {
			boolean found = false;
			Iterator iterator = outstanding.iterator();
			while(iterator.hasNext()) {
				Object oo = iterator.next();
				if (comparator.compare(ao, oo) == 0) {
					iterator.remove();
					found = true;
				}
			}
			if (!found) {
				return false;
			}
		}
		if (!outstanding.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public static int unorderedCollectionHashcode(Collection collection) {
		// Stupid implmentation, just add all the hashcodes
		int hashcode = 0;
		for (Object item: collection) {
			hashcode += item.hashCode();
		}
		return hashcode;
	}
	
	public static String readFile(File file) throws IOException {
		StringBuffer fileData = new StringBuffer(BUFFER_SIZE);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[BUFFER_SIZE];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[BUFFER_SIZE];
        }
        reader.close();
        return fileData.toString();
	}

	/**
	 * Try to get java property from the object by reflection
	 */
	public static <T> T getJavaProperty(Object object, String propertyName, Class<T> propetyClass) {
		String getterName = "get" + StringUtils.capitalize(propertyName);
		Method method;
		try {
			method = object.getClass().getMethod(getterName);
		} catch (SecurityException e) {
			throw new IllegalArgumentException("Security error getting getter for property "+propertyName+": "+e.getMessage(),e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("No getter for property "+propertyName+" in "+object+" ("+object.getClass()+")");
		}
		if (method == null) {
			throw new IllegalArgumentException("No getter for property "+propertyName+" in "+object+" ("+object.getClass()+")");
		}
		if (!propetyClass.isAssignableFrom(method.getReturnType())) {
			throw new IllegalArgumentException("The getter for property " + propertyName + " returns " + method.getReturnType() +
					", expected " + propetyClass+" in "+object+" ("+object.getClass()+")");
		}
		try {
			return (T) method.invoke(object);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException("Error invoking getter for property "+propertyName+" in "+object+" ("+object.getClass()+"): "
					+e.getClass().getSimpleName()+": "+e.getMessage(),e);
		}
	}

	public static <T> Collection<T> createCollection(T... items) {
		Collection<T> collection = new ArrayList<T>(items.length);
		for (T item: items) {
			collection.add(item);
		}
		return collection;
	}
		
}
