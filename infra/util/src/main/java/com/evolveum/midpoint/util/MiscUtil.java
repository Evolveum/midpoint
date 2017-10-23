/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author semancik
 *
 */
public class MiscUtil {

	private static final int BUFFER_SIZE = 2048;

	private static final Trace LOGGER = TraceManager.getTrace(MiscUtil.class);

	private static DatatypeFactory df = null;

    static {
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining Datatype Factory instance", dce);
        }
    }

    @NotNull
	public static <T> Collection<T> union(Collection<T>... sets) {
		Set<T> resultSet = new HashSet<>();
		for (Collection<T> set: sets) {
			if (set != null) {
				resultSet.addAll(set);
			}
		}
		return resultSet;
	}

	public static <T> Collection<? extends T> unionExtends(Collection<? extends T>... sets) {
		Set<T> resultSet = new HashSet<T>();
		for (Collection<? extends T> set: sets) {
			if (set != null) {
				resultSet.addAll(set);
			}
		}
		return resultSet;
	}

	public static <T> boolean listEquals(List<T> a, List<T> b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).equals(b.get(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean unorderedCollectionEquals(Collection a, Collection b) {
		return unorderedCollectionEquals(a, b, (xa, xb) -> xa.equals(xb));
	}

	/**
	 * Only zero vs non-zero value of comparator is important.
	 */
	public static <T> boolean unorderedCollectionCompare(Collection<T> a, Collection<T> b, final Comparator<T> comparator) {
		return unorderedCollectionEquals(a, b, (xa, xb) -> comparator.compare(xa, xb) == 0);
	}

	/**
	 * Only zero vs non-zero value of comparator is important.
	 */
	public static <A,B> boolean unorderedCollectionEquals(Collection<A> a, Collection<B> b, HeteroComparator<A,B> comparator) {
		if (a == null && b == null) {
 			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.size() != b.size()) {
			return false;
		}
		Collection<B> outstanding = new ArrayList<>(b.size());
		outstanding.addAll(b);
		for (A ao: a) {
			boolean found = false;
			Iterator<B> iterator = outstanding.iterator();
			while(iterator.hasNext()) {
				B oo = iterator.next();
				if (comparator.isEquivalent(ao, oo)) {
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

	public static <T> boolean unorderedArrayEquals(T[] a, T[] b) {
		Comparator<T> comparator = new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.equals(o2) ? 0 : 1;
			}
		};
		return unorderedArrayEquals(a, b, comparator);
	}

	/**
	 * Only zero vs non-zero value of comparator is important.
	 */
	public static <T> boolean unorderedArrayEquals(T[] a, T[] b, Comparator<T> comparator) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.length != b.length) {
			return false;
		}
		List<T> outstanding = Arrays.asList(b);
		for (T ao: a) {
			boolean found = false;
			Iterator<T> iterator = outstanding.iterator();
			while(iterator.hasNext()) {
				T oo = iterator.next();
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

	public static <T> int unorderedCollectionHashcode(Collection<T> collection, Predicate<T> filter) {
		// Stupid implmentation, just add all the hashcodes
		int hashcode = 0;
		for (T item: collection) {
			if (filter != null && !filter.test(item)) {
				continue;
			}
			int itemHash = item.hashCode();
			hashcode += itemHash;
		}
		return hashcode;
	}

	public static String readFile(File file) throws IOException {
		StringBuilder fileData = new StringBuilder(BUFFER_SIZE);
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

    /**
     * Copy a directory and its contents.
     *
     * @param src
     *            The name of the directory to copy.
     * @param dst
     *            The name of the destination directory.
     * @throws IOException
     *             If the directory could not be copied.
     */
    public static void copyDirectory(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            // Create the destination directory if it does not exist.
            if (!dst.exists()) {
                dst.mkdirs();
            }

            // Recursively copy sub-directories and files.
            for (String child : src.list()) {
                copyDirectory(new File(src, child), new File(dst, child));
            }
        } else {
            MiscUtil.copyFile(src, dst);
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

    public static XMLGregorianCalendar asXMLGregorianCalendar(Long timeInMilis) {
        if (timeInMilis == null || timeInMilis == 0) {
            return null;
        } else {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(timeInMilis);
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

    public static Long asLong(XMLGregorianCalendar xgc) {
        if (xgc == null) {
            return null;
        } else {
            return xgc.toGregorianCalendar().getTimeInMillis();
        }
    }

    public static java.util.Date asDate(int year, int month, int date, int hrs, int min, int sec) {
    	Calendar cal = Calendar.getInstance();
    	cal.set(year, month - 1, date, hrs, min, sec);
		cal.set(Calendar.MILLISECOND, 0);
    	return cal.getTime();
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

	public static boolean isNoValue(Collection<?> collection) {
		if (collection == null)
			return true;
		if (collection.isEmpty())
			return true;
		for (Object val : collection) {
			if (val == null)
				continue;
			if (val instanceof String && ((String) val).isEmpty())
				continue;
			return false;
		}
		return true;
	}

	public static boolean hasNoValue(Collection<?> collection) {
		if (collection == null)
			return true;
		if (collection.isEmpty())
			return true;
		for (Object val : collection) {
			if (val == null)
				return true;
			if (val instanceof String && ((String) val).isEmpty())
				return true;
		}
		return false;
	}

	/**
	 * Shallow clone
	 */
	public static <K,V> Map<K,V> cloneMap(Map<K, V> orig) {
		if (orig == null) {
			return null;
		}
		Map<K,V> clone = new HashMap<K, V>();
		for (Entry<K, V> origEntry: orig.entrySet()) {
			clone.put(origEntry.getKey(), origEntry.getValue());
		}
		return clone;
	}

	public static String toString(Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString();
	}

	public static List<String> splitLines(String string) {
		List<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(string);
		while (scanner.hasNextLine()) {
		  lines.add(scanner.nextLine());
		}
		return lines;
	}

	public static boolean isBetween(XMLGregorianCalendar date, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		return (date.compare(start) == DatatypeConstants.GREATER || date.compare(start) == DatatypeConstants.EQUAL)
				&& (date.compare(end) == DatatypeConstants.LESSER || date.compare(end) == DatatypeConstants.EQUAL);
	}

	public static <T> boolean contains(T element, T[] array) {
		for (T aElement: array) {
			if (equals(element, aElement)) {
				return true;
			}
		}
		return false;
	}

    public static String stripHtmlMarkup(String htmlString) {
        if (htmlString == null) {
            return null;
        }
        return htmlString.replaceAll("<[^>]*>", "");
    }

    public static <T> Collection<T> getValuesFromDisplayableValues(Collection<? extends DisplayableValue<T>> disps) {
    	if (disps == null) {
    		return null;
    	}
    	List<T> out = new ArrayList<T>(disps.size());
    	for (DisplayableValue<T> disp: disps) {
    		out.add(disp.getValue());
    	}
    	return out;
    }

    public static String binaryToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	public static byte[] hexToBinary(String hex) {
		int l = hex.length();
		byte[] bytes = new byte[l/2];
		for (int i = 0; i < l; i += 2) {
			bytes[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return bytes;
	}

	public static <T> void addAllIfNotPresent(List<T> receivingList, List<T> supplyingList) {
		if (supplyingList == null) {
			return;
		}
		for (T supplyingElement: supplyingList) {
			addIfNotPresent(receivingList, supplyingElement);
		}
	}

	public static <T> void addIfNotPresent(List<T> receivingList, T supplyingElement) {
		if (!receivingList.contains(supplyingElement)) {
			receivingList.add(supplyingElement);
		}
	}

	public static boolean nullableCollectionsEqual(Collection<?> c1, Collection<?> c2) {
		boolean empty1 = c1 == null || c1.isEmpty();
		boolean empty2 = c2 == null || c2.isEmpty();
		if (empty1) {
			return empty2;
		} else if (empty2) {
			return false;
		} else {
			return c1.equals(c2);
		}
	}

	public static String getObjectName(Object o) {
		return o != null ? "an instance of " + o.getClass().getName() : "null value";
	}

	// @pre: at least of o1, o2 is null
	public static Integer compareNullLast(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null) {
			return 1;
		} else if (o2 == null) {
			return -1;
		} else {
			throw new IllegalArgumentException("Both objects are non-null");
		}
	}

	@SafeVarargs
	public static <T> T getFirstNonNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	public static String getFirstNonNullString(Object... values) {
		Object value = getFirstNonNull(values);
		return value != null ? value.toString() : null;
	}

	public static <T> T extractSingleton(Collection<T> collection) {
		if (collection == null || collection.isEmpty()) {
			return null;
		} else if (collection.size() == 1) {
			return collection.iterator().next();
		} else {
			throw new IllegalArgumentException("Expected a collection with at most one item; got the one with " + collection.size() + " items");
		}
	}

	// similar to the above ... todo deduplicate
	public static <T> T getSingleValue(Collection<T> values, T defaultValue, String contextDescription) {
		if (values.size() == 0) {
			return defaultValue;
		} else if (values.size() > 1) {
			throw new IllegalStateException("Expected exactly one value; got "
					+ values.size() + " ones in " + contextDescription);
		} else {
			T value = values.iterator().next();
			return value != null ? value : defaultValue;
		}
	}

	public static boolean isCollectionOf(Object object, @NotNull Class<?> memberClass) {
		return object instanceof Collection
				&& ((Collection<?>) object).stream().allMatch(member -> member != null && memberClass.isAssignableFrom(member.getClass()));
	}

	public static <E> Function<Object, Stream<E>> instancesOf(Class<E> cls) {
		return o -> cls.isInstance(o)
				? Stream.of(cls.cast(o))
				: Stream.empty();
	}

	// CollectionUtils does not provide this
	// @pre: !list.isEmpty()
	public static <T> T last(List<T> list) {
		return list.get(list.size() - 1);
	}

	public static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	public static String nullIfEmpty(String s) {
		return "".equals(s) ? null : s;
	}

	/**
	 * Returns true if the collection contains at least one pair of equals elements.
	 */
	public static <T> boolean hasDuplicates(Collection<T> collection) {
		Set<T> set = new HashSet<>();
		for (T e: collection) {
			if (!set.add(e)) {
				return true;
			}
		}
		return false;
	}

	public static String formatExceptionMessageWithCause(Throwable t) {
		return formatExceptionMessageWithCause(t, 0);
	}

	public static String formatExceptionMessageWithCause(Throwable t, int indent) {
		if (t == null) {
			return null;
		} else {
			String local = StringUtils.repeat("  ", indent) + formatExceptionMessage(t);
			return t.getCause() == null
					? local
					: local + ":\n" + formatExceptionMessageWithCause(t.getCause(), indent + 1);
		}
	}

	private static String formatExceptionMessage(Throwable t) {
		return t != null
				? t.getMessage() + " [" + t.getClass().getSimpleName() + "]"
				: null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwException(Throwable exception) throws T
	{
		throw (T) exception;
	}

	public static void throwExceptionAsUnchecked(Throwable t) {
		MiscUtil.throwException(t);
	}

	@FunctionalInterface
	public interface CheckedSupplier<T> {
		T get() throws Exception;
	}

	public static <T> Supplier<T> exceptionsToRuntime(CheckedSupplier<T> checkedSupplier) {
		return () -> {
			try {
				return checkedSupplier.get();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new SystemException(e);
			}
		};
	}

	public static <T> Collection<T> filter(Collection<T> input, Predicate<? super T> predicate) {
		return input.stream().filter(predicate).collect(Collectors.toList());
	}

	public static <T> Set<T> filter(Set<T> input, Predicate<? super T> predicate) {
		return input.stream().filter(predicate).collect(Collectors.toSet());
	}
}
