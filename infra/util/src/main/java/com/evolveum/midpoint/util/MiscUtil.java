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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.CollectionUtils;

/**
 * @author semancik
 *
 */
public class MiscUtil {
	
	private static final int BUFFER_SIZE = 2048; 
	private static DatatypeFactory df = null;

    static {
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining Datatype Factory instance", dce);
        }
    }
	
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
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public static <T> Collection<T> createCollection(T... items) {
		Collection<T> collection = new ArrayList<T>(items.length);
		for (T item: items) {
			collection.add(item);
		}
		return collection;
	}
	
	/**
	 * n-ary and that ignores null values.
	 */
	public static Boolean and(Boolean... operands) {
		Boolean result = null;
		for (Boolean operand: operands) {
			if (operand == null) {
				continue;
			}
			if (!operand) {
				return false;
			}
			result = true;
		}
		return result;
	}

	public static boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

    /**
     * Converts a java.util.Date into an instance of XMLGregorianCalendar
     *
     * @param date Instance of java.util.Date or a null reference
     * @return XMLGregorianCalendar instance whose value is based upon the
     *         value in the date parameter. If the date parameter is null then
     *         this method will simply return null.
     */
    public static XMLGregorianCalendar asXMLGregorianCalendar(java.util.Date date) {
        if (date == null) {
            return null;
        } else {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(date.getTime());
            return df.newXMLGregorianCalendar(gc);
        }
    }

    /**
     * Converts an XMLGregorianCalendar to an instance of java.util.Date
     *
     * @param xgc Instance of XMLGregorianCalendar or a null reference
     * @return java.util.Date instance whose value is based upon the
     *         value in the xgc parameter. If the xgc parameter is null then
     *         this method will simply return null.
     */
    public static java.util.Date asDate(XMLGregorianCalendar xgc) {
        if (xgc == null) {
            return null;
        } else {
            return xgc.toGregorianCalendar().getTime();
        }
    }
    
    public static <T> void carthesian(Collection<Collection<T>> dimensions, Processor<Collection<T>> processor) {
    	List<Collection<T>> dimensionList = new ArrayList<Collection<T>>(dimensions.size());
    	dimensionList.addAll(dimensions);
    	carthesian(new ArrayList<T>(dimensions.size()), dimensionList, 0, processor);
    }
        
    private static <T> void carthesian(List<T> items, List<Collection<T>> dimensions, int dimensionNum, Processor<Collection<T>> processor) {
    	Collection<T> myDimension = dimensions.get(dimensionNum);
    	for (T item: myDimension) {
    		items.add(item);
    		if (dimensionNum < dimensions.size() - 1) {
    			carthesian(items, dimensions, dimensionNum + 1, processor);
    		} else {
    			processor.process(items);
    		}
    		items.remove(items.size() - 1);
    	}
    }
    
	public static String concat(Collection<String> stringCollection) {
		StringBuilder sb = new StringBuilder();
		for (String s: stringCollection) {
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static boolean isAllNull(Collection<?> collection) {
		for (Object o: collection) {
			if (o != null) {
				return false;
			}
		}
		return true;
	}

	public static String getValueWithClass(Object object) {
		if (object == null) {
			return "null";
		}
		return "("+object.getClass().getSimpleName() + ")"  + object;
	}

}
