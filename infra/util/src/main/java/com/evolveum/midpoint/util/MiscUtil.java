/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;

import static java.util.Collections.*;

/**
 * @author semancik
 */
public class MiscUtil {

    private static final int BUFFER_SIZE = 2048;

    private static DatatypeFactory df;

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
        for (Collection<T> set : sets) {
            if (set != null) {
                resultSet.addAll(set);
            }
        }
        return resultSet;
    }

    public static <T> Collection<? extends T> unionExtends(Collection<? extends T>... sets) {
        Set<T> resultSet = new HashSet<>();
        for (Collection<? extends T> set : sets) {
            if (set != null) {
                resultSet.addAll(set);
            }
        }
        return resultSet;
    }

    public static <T> boolean unorderedCollectionEquals(Collection<T> a, Collection<T> b) {
        return unorderedCollectionEquals(a, b, (xa, xb) -> xa.equals(xb));
    }

    /**
     * Only zero vs non-zero value of comparator is important.
     */
    public static <T> boolean unorderedCollectionCompare(Collection<T> a, Collection<T> b, Comparator<T> comparator) {
        if (comparator == null) {
            return unorderedCollectionEquals(a, b);
        } else {
            return unorderedCollectionEquals(a, b, (xa, xb) -> comparator.compare(xa, xb) == 0);
        }
    }

    /**
     * Only zero vs non-zero value of comparator is important.
     */
    public static <A, B> boolean unorderedCollectionEquals(Collection<A> a, Collection<B> b, HeteroComparator<A, B> comparator) {
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
        for (A ao : a) {
            boolean found = false;
            Iterator<B> iterator = outstanding.iterator();
            while (iterator.hasNext()) {
                B oo = iterator.next();
                if (comparator.isEquivalent(ao, oo)) {
                    iterator.remove();
                    found = true;
                    break;
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
        Comparator<T> comparator = (o1, o2) -> o1.equals(o2) ? 0 : 1;
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
        for (T ao : a) {
            boolean found = false;
            Iterator<T> iterator = outstanding.iterator();
            while (iterator.hasNext()) {
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
        for (T item : collection) {
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
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
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

        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    /**
     * Copy a directory and its contents.
     *
     * @param src The name of the directory to copy.
     * @param dst The name of the destination directory.
     * @throws IOException If the directory could not be copied.
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

    @SafeVarargs
    public static <T> Collection<T> createCollection(T... items) {
        Collection<T> collection = new ArrayList<>(items.length);
        Collections.addAll(collection, items);
        return collection;
    }

    /**
     * n-ary and that ignores null values.
     */
    public static Boolean and(Boolean... operands) {
        Boolean result = null;
        for (Boolean operand : operands) {
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
     * value in the date parameter. If the date parameter is null then
     * this method will simply return null.
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
     * value in the xgc parameter. If the xgc parameter is null then
     * this method will simply return null.
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
        List<Collection<T>> dimensionList = new ArrayList<>(dimensions.size());
        dimensionList.addAll(dimensions);
        carthesian(new ArrayList<>(dimensions.size()), dimensionList, 0, processor);
    }

    private static <T> void carthesian(List<T> items, List<Collection<T>> dimensions, int dimensionNum, Processor<Collection<T>> processor) {
        Collection<T> myDimension = dimensions.get(dimensionNum);
        for (T item : myDimension) {
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
        for (String s : stringCollection) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static boolean isAllNull(Collection<?> collection) {
        for (Object o : collection) {
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
        return "(" + object.getClass().getSimpleName() + ")" + object;
    }

    public static String getClass(Object object) {
        return object != null ? object.getClass().getName() : "null";
    }

    public static boolean isNoValue(Collection<?> collection) {
        if (collection == null) {
            return true;
        }
        if (collection.isEmpty()) {
            return true;
        }
        for (Object val : collection) {
            if (val == null) {
                continue;
            }
            if (val instanceof String && ((String) val).isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean hasNoValue(Collection<?> collection) {
        if (collection == null) {
            return true;
        }
        if (collection.isEmpty()) {
            return true;
        }
        for (Object val : collection) {
            if (val == null) {
                return true;
            }
            if (val instanceof String && ((String) val).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shallow clone
     */
    public static <K, V> Map<K, V> cloneMap(Map<K, V> orig) {
        if (orig == null) {
            return null;
        }
        Map<K, V> clone = new HashMap<>();
        for (Entry<K, V> origEntry : orig.entrySet()) {
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
        List<String> lines = new ArrayList<>();
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
        for (T aElement : array) {
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
        List<T> out = new ArrayList<>(disps.size());
        for (DisplayableValue<T> disp : disps) {
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
        byte[] bytes = new byte[l / 2];
        for (int i = 0; i < l; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    public static String hexToUtf8String(String hex) {
        return new String(MiscUtil.hexToBinary(hex), StandardCharsets.UTF_8);
    }

    public static <T> void addAllIfNotPresent(List<T> receivingList, List<T> supplyingList) {
        if (supplyingList == null) {
            return;
        }
        for (T supplyingElement : supplyingList) {
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
        return extractSingleton(collection, () -> new IllegalArgumentException("Expected a collection with at most one item; got the one with " + collection.size() + " items"));
    }

    public static <T, E extends Throwable> T extractSingleton(Collection<T> collection, Supplier<E> exceptionSupplier) throws E {
        if (collection == null || collection.isEmpty()) {
            return null;
        } else if (collection.size() == 1) {
            return collection.iterator().next();
        } else {
            throw exceptionSupplier.get();
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

    @NotNull
    public static String emptyIfNull(String s) {
        return s != null ? s : "";
    }

    @NotNull
    public static <T> List<T> emptyIfNull(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    @NotNull
    public static <T> Collection<T> emptyIfNull(Collection<T> collection) {
        return collection != null ? collection : Collections.emptyList();
    }

    @NotNull
    public static <T> Stream<T> streamOf(Collection<T> collection) {
        return collection != null ? collection.stream() : Stream.empty();
    }

    public static String nullIfEmpty(String s) {
        return "".equals(s) ? null : s;
    }

    /**
     * Returns true if the collection contains at least one pair of equals elements.
     */
    public static <T> boolean hasDuplicates(Collection<T> collection) {
        Set<T> set = new HashSet<>();
        for (T e : collection) {
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
    private static <T extends Throwable> void throwException(Throwable exception) throws T {
        throw (T) exception;
    }

    public static void throwExceptionAsUnchecked(Throwable t) {
        MiscUtil.throwException(t);
    }

    public static <T> T runChecked(CheckedFunction<Producer<T>, T> function, CheckedProducer<T> checkedProducer) throws CommonException {
        try {
            return function.apply(() -> {
                try {
                    return checkedProducer.get();
                } catch (CommonException e) {
                    throw new TunnelException(e);
                }
            });
        } catch (TunnelException te) {
            return unwrapTunnelledException(te);        // return is just for formal reasons -- this throws exceptions only
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> T unwrapTunnelledException(TunnelException te) throws CommonException {
        Throwable cause = te.getCause();
        if (cause instanceof CommonException) {
            throw (CommonException) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else {
            throw te;
        }
    }

    public static <T> Collection<T> filter(Collection<T> input, Predicate<? super T> predicate) {
        return input.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T> Set<T> filter(Set<T> input, Predicate<? super T> predicate) {
        return input.stream().filter(predicate).collect(Collectors.toSet());
    }

    @NotNull
    public static <V> Collection<V> nonNullValues(@NotNull Collection<V> values) {
        return values.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static URL toUrlUnchecked(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        }
    }

    public static <T> List<T> join(Collection<T> a, Collection<T> b) {
        if (a == null && b == null) {
            return new ArrayList<>();
        }
        if (a == null) {
            return new ArrayList<>(b);
        }
        if (b == null) {
            return new ArrayList<>(a);
        }
        List<T> list = new ArrayList<>(a.size() + b.size());
        list.addAll(a);
        list.addAll(b);
        return list;
    }

    /**
     * Thanks for this code go to https://crunchify.com/how-to-generate-java-thread-dump-programmatically/
     */
    public static String takeThreadDump(@Nullable Thread thread) {
        StringBuilder dump = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = thread != null ? new long[] { thread.getId() } : threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds, 100);
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append("Thread name: \"");
            dump.append(threadInfo.getThreadName());
            dump.append("\"\n");
            dump.append("Thread state: ");
            dump.append(threadInfo.getThreadState());
            StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\n    at ");
                dump.append(stackTraceElement);
            }
            dump.append("\n\n");
        }
        return dump.toString();
    }

    public static <K, V> Map<K, V> paramsToMap(Object[] params) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            map.put((K) params[i], (V) params[i + 1]);
        }
        return map;
    }

    public static void writeZipFile(File file, String entryName, String content, Charset charset) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(content.getBytes(charset));
        }
    }

    // More serious would be to read XML directly from the input stream -- fixme some day
    // We should probably implement reading from ZIP file directly in PrismContext
    @SuppressWarnings("unused")     // used externally
    public static String readZipFile(File file, Charset charset) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                return String.join("\n", IOUtils.readLines(zis, charset));
            } else {
                return null;
            }
        }
    }

    public static <T> Set<T> singletonOrEmptySet(T value) {
        return value != null ? singleton(value) : emptySet();
    }

    public static <T> List<T> singletonOrEmptyList(T value) {
        return value != null ? singletonList(value) : emptyList();
    }
}
