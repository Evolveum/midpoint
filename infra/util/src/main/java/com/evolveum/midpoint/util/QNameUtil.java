/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * QName &lt;-&gt; URI conversion.
 * <p>
 * Very simplistic but better than nothing.
 *
 * @author semancik
 */
public class QNameUtil {

    public static final Trace LOGGER = TraceManager.getTrace(QNameUtil.class);

    // TODO consider where to put all this undeclared-prefixes-things
    // Hopefully in 3.2 everything will find its place

    private static final String UNDECLARED_PREFIX_MARK = "__UNDECLARED__";
    public static final char DEFAULT_QNAME_URI_SEPARATOR_CHAR = '#';

    // Whether we want to tolerate undeclared XML prefixes in QNames
    // This is here only for backward compatibility with versions 3.0-3.1.
    // Will be set to false starting with 3.2 (MID-2191)
    private static boolean tolerateUndeclaredPrefixes = false;

    // ThreadLocal "safe mode" override for the above value (MID-2218)
    // This can be set to true for raw reads, allowing to manually fix broken objects
    private static final ThreadLocal<Boolean> TEMPORARILY_TOLERATE_UNDECLARED_PREFIXES = new ThreadLocal<>();

    public static String qNameToUri(QName qname) {
        return qNameToUri(qname, true);
    }

    public static String qNameToUri(QName qname, boolean unqualifiedStartsWithHash) {
        return qNameToUri(qname, unqualifiedStartsWithHash, DEFAULT_QNAME_URI_SEPARATOR_CHAR);
    }

    public static String qNameToUri(QName qname, boolean unqualifiedStartsWithHash, char separatorChar) {
        String nsUri = qname.getNamespaceURI();
        StringBuilder sb = new StringBuilder(nsUri);

        // TODO: Check if there's already a fragment
        // e.g. http://foo/bar#baz

        if (!nsUri.endsWith("#") && !nsUri.endsWith("/")) {
            if (unqualifiedStartsWithHash || !nsUri.isEmpty()) {
                sb.append(separatorChar);
            }
        }
        sb.append(qname.getLocalPart());

        return sb.toString();
    }

    public static QName uriToQName(String uri) {
        return uriToQName(uri, false);
    }

    public static boolean noNamespace(@NotNull QName name) {
        return StringUtils.isEmpty(name.getNamespaceURI());
    }

    public static boolean hasNamespace(@NotNull QName name) {
        return !noNamespace(name);
    }

    public static QName unqualify(QName name) {
        return new QName(name.getLocalPart());
    }

    public static QName qualifyIfNeeded(QName name, String defaultNamespace) {
        return hasNamespace(name)
                ? name
                : new QName(defaultNamespace, name.getLocalPart());

    }

    /**
     * Finds value in the map by QName key using {@link #match(QName, QName)}.
     * Fails if multiple matches are found.
     * Returns {@code null} if no match is found.
     */
    public static <V> V getByQName(@NotNull Map<QName, V> map, @NotNull QName key) {
        if (hasNamespace(key)) {
            return map.get(key);
        }
        List<Map.Entry<QName, V>> matching = map.entrySet().stream()
                .filter(e -> match(e.getKey(), key))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            return null;
        } else if (matching.size() == 1) {
            return matching.get(0).getValue();
        } else {
            throw new IllegalStateException("More than one matching value for key " + key + ": " + matching);
        }
    }

    public static boolean matchUri(String uri1, String uri2) {
        if (java.util.Objects.equals(uri1, uri2)) {
            return true;
        } else if (uri1 == null || uri2 == null) {
            return false;
        } else {
            return match(uriToQName(uri1, true), uriToQName(uri2, true));
        }
    }

    public static class QNameInfo {
        @NotNull public final QName name;
        public final boolean explicitEmptyNamespace;

        private QNameInfo(@NotNull QName name, boolean explicitEmptyNamespace) {
            this.name = name;
            this.explicitEmptyNamespace = explicitEmptyNamespace;
        }
    }

    @NotNull
    public static QName uriToQName(@NotNull String uri, boolean allowUnqualified) {
        return uriToQNameInfo(uri, allowUnqualified).name;
    }

    @NotNull
    public static QName uriToQName(String uri, String defaultNamespace) {
        QNameInfo info = uriToQNameInfo(uri, true);
        if (hasNamespace(info.name) || info.explicitEmptyNamespace || StringUtils.isEmpty(defaultNamespace)) {
            return info.name;
        } else {
            return new QName(defaultNamespace, info.name.getLocalPart());
        }
    }

    @NotNull
    public static QNameInfo uriToQNameInfo(@NotNull String uri, boolean allowUnqualified) {
        Validate.notNull(uri, "null URI");
        int index = uri.lastIndexOf("#");
        if (index != -1) {
            String ns = uri.substring(0, index);
            String name = uri.substring(index + 1);
            return new QNameInfo(new QName(ns, name), "".equals(ns));
        }
        index = uri.lastIndexOf("/");
        // TODO check if this is still in the path section, e.g.
        // if the matched slash is not a beginning of authority section
        if (index != -1) {
            String ns = uri.substring(0, index);
            String name = uri.substring(index + 1);
            return new QNameInfo(new QName(ns, name), "".equals(ns));
        }
        if (allowUnqualified) {
            return new QNameInfo(new QName(uri), false);
        } else {
            throw new IllegalArgumentException("The URI (" + uri + ") does not contain slash character");
        }
    }

    public static boolean compareQName(QName qname, Node node) {
        return (qname.getNamespaceURI().equals(node.getNamespaceURI()) && qname.getLocalPart().equals(node.getLocalName()));
    }

    /**
     * Matching with considering wildcard namespace (null).
     */
    public static boolean match(QName a, QName b) {
        return match(a, b, false);
    }

    // case insensitive is related to local parts
    public static boolean match(QName a, QName b, boolean caseInsensitive) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (!caseInsensitive) {
            // traditional comparison
            if (StringUtils.isEmpty(a.getNamespaceURI()) || StringUtils.isEmpty(b.getNamespaceURI())) {
                return a.getLocalPart().equals(b.getLocalPart());
            } else {
                return a.equals(b);
            }
        } else {
            // relaxed (case-insensitive) one
            if (!a.getLocalPart().equalsIgnoreCase(b.getLocalPart())) {
                return false;
            }
            if (StringUtils.isEmpty(a.getNamespaceURI()) || StringUtils.isEmpty(b.getNamespaceURI())) {
                return true;
            } else {
                return a.getNamespaceURI().equals(b.getNamespaceURI());
            }
        }

    }

    public static boolean unorderedCollectionMatch(Collection<QName> a, Collection<QName> b) {
        return MiscUtil.unorderedCollectionCompare(a, b, (o1, o2) -> {
            if (match(o1, o2)) {
                return 0;
            } else {
                return 1;
            }
        });
    }

    /**
     * Matches QName with a URI representation. The URL may in fact be just the local
     * part.
     */
    public static boolean matchWithUri(QName qname, String uri) {
        return match(qname, uriToQName(uri, true));
    }

    public static QName resolveNs(QName a, Collection<QName> col) {
        if (col == null) {
            return null;
        }
        QName found = null;
        for (QName b : col) {
            if (match(a, b)) {
                if (found != null) {
                    throw new IllegalStateException("Found more than one suitable qnames( " + found + b + ") for attribute: " + a);
                }
                found = b;
            }
        }
        return found;
    }

    public static boolean matchAny(QName nameToFind, Collection<? extends QName> names) {
        // we no longer use resolveNs any more here, as the 'names' can contain duplicate qnames (resolveNs would complain on it)
        if (names == null) {
            return false;
        }
        for (QName name : names) {
            if (match(nameToFind, name)) {
                return true;
            }
        }
        return false;
    }

    public static Collection<QName> createCollection(QName... qnames) {
        return Arrays.asList(qnames);
    }

    public static QName nullNamespace(QName qname) {
        return new QName(null, qname.getLocalPart(), qname.getPrefix());
    }

    public static boolean isUnqualified(QName name) {
        return StringUtils.isEmpty(name.getNamespaceURI());
    }

    public static boolean isQualified(QName name) {
        return !isUnqualified(name);
    }

    public static boolean isUnqualified(String uri) {
        // The '/' checking is maybe not quite precise.
        return uri.indexOf('#') < 0 && uri.indexOf('/') < 0;
    }

    public static boolean isTolerateUndeclaredPrefixes() {
        return tolerateUndeclaredPrefixes;
    }

    public static void setTolerateUndeclaredPrefixes(boolean value) {
        tolerateUndeclaredPrefixes = value;
    }

    public static void setTemporarilyTolerateUndeclaredPrefixes(Boolean value) {
        TEMPORARILY_TOLERATE_UNDECLARED_PREFIXES.set(value);
    }

    public static void reportUndeclaredNamespacePrefix(String prefix, String context) {
        if (tolerateUndeclaredPrefixes
                || Boolean.TRUE.equals(TEMPORARILY_TOLERATE_UNDECLARED_PREFIXES.get())) {
            LOGGER.error("Undeclared namespace prefix '" + prefix + "' in '" + context + "'.");
        } else {
            throw new IllegalArgumentException("Undeclared namespace prefix '" + prefix + "' in '" + context + "'");
        }
    }

    // @pre namespacePrefix != null
    public static String markPrefixAsUndeclared(String namespacePrefix) {
        if (namespacePrefix.startsWith(UNDECLARED_PREFIX_MARK)) {
            return namespacePrefix;
        } else {
            return UNDECLARED_PREFIX_MARK + namespacePrefix;
        }
    }

    public static boolean isPrefixUndeclared(String namespacePrefix) {
        return namespacePrefix != null && namespacePrefix.startsWith(UNDECLARED_PREFIX_MARK);
    }

    private static final String WORDS_COLON_REGEX = "^\\w+:.*";
    private static final Pattern WORDS_COLON_PATTERN = Pattern.compile(WORDS_COLON_REGEX);

    public static boolean isUri(String string) {
        if (string == null) {
            return false;
        }
        return WORDS_COLON_PATTERN.matcher(string).matches();
    }

    public static String getLocalPart(QName name) {
        return name != null ? name.getLocalPart() : null;
    }

    public static boolean contains(Collection<? extends QName> col, QName qname) {
        return col != null && col.stream().anyMatch(e -> match(e, qname));
    }

    public static boolean contains(QName[] array, QName qname) {
        if (array == null) {
            return false;
        }
        for (QName element : array) {
            if (match(qname, element)) {
                return true;
            }
        }
        return false;
    }

    public static boolean remove(Collection<? extends QName> col, QName qname) {
        return col != null && col.removeIf(e -> match(e, qname));
    }

    public static String escapeElementName(String name) {
        if (name == null || name.isEmpty()) {
            return name;    // suspicious but that's not our business
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (allowed(ch, i == 0)) {
                sb.append(ch);
            } else {
                sb.append("_x").append(Long.toHexString(ch));
            }
        }
        return sb.toString();
    }

    // TODO fix this method if necessary
    // see https://www.w3.org/TR/REC-xml/#NT-NameChar (JSON and YAML can - very probably - use any characters for "element" names)
    private static boolean allowed(char ch, boolean atStart) {
        return Character.isLetter(ch) || ch == '_'
                || (!atStart && (Character.isDigit(ch) || ch == '.' || ch == '-'));
    }

    public static String prettyPrint(QName... qnames) {
        return PrettyPrinter.prettyPrint(Arrays.asList(qnames));
    }
}
