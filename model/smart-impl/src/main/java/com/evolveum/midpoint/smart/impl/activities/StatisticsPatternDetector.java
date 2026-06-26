/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StatisticsPatternDetector {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsPatternDetector.class);

    /** Regular expression pattern for token delimiters. */
    private static final String DELIMITERS = "[-_:*#+.()\\[\\]]+";

    /** Compiled pattern for token delimiters (used by Matcher). */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(DELIMITERS);

    /**
     * Regular expression pattern for matching URLs.
     * Matches strings that start with "http://", "https://", or "www.", followed by non-whitespace characters.
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE);

    /**
     * Regular expression pattern for matching email addresses.
     * Matches simple email addresses of the form localpart@domain.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Regular expression pattern for matching phone numbers.
     * Matches phone numbers with optional leading '+', containing digits, spaces, dashes, or parentheses,
     * and with a length between 7 and 20 characters.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[\\d\\s\\-()]{7,20}$");

    /** Attribute local names that should be treated as DN attributes. */
    private static final Set<String> DN_ATTRIBUTE_LOCAL_NAMES = Set.of("dn", "distinguishedname");

    void aggregateDnSuffix(String value, Map<String, Integer> dnSuffixCounts) {
        String suffix = parseDNSuffix(value);
        if (suffix != null) {
            dnSuffixCounts.merge(suffix, 1, Integer::sum);
        }
    }

    /**
     * Extracts first/last tokens (including their adjacent delimiter) and increments their counts.
     * Skips values that look like URLs or phone numbers. For email addresses, splits on "@" and
     * records the domain suffix (e.g. "@example.com") as a last token.
     * Only "inside" delimiters are considered: any leading or trailing delimiter sequences are
     * stripped before processing, so they do not influence the token boundaries.
     * <p>
     * For example, "-prod-server01-" yields firstToken="-prod-" and lastToken="-server01-"
     */
    void aggregateTokenPatterns(
            String value,
            Map<String, Integer> firstTokenCounts,
            Map<String, Integer> lastTokenCounts) {

        if (isUrl(value) || isPhoneNumber(value)) {
            return;
        }

        if (isEmail(value)) {
            int atIndex = value.indexOf('@');
            if (atIndex >= 0) {
                lastTokenCounts.merge(value.substring(atIndex), 1, Integer::sum);
            }
            return;
        }

        // Strip leading and trailing delimiter sequences to find only "inside" delimiters.
        // leadingDelimLength is used as an offset to extract tokens from the original value,
        // so that tokens preserve any leading/trailing delimiters of the original string.
        String withoutLeading = value.replaceAll("^" + DELIMITERS, "");
        int leadingDelimLength = value.length() - withoutLeading.length();
        String inner = withoutLeading.replaceAll(DELIMITERS + "$", "");

        Matcher matcher = DELIMITER_PATTERN.matcher(inner);
        if (!matcher.find()) {
            return;
        }

        int firstDelimStart = matcher.start();
        int firstDelimEnd = matcher.end();

        // Walk to find the last delimiter occurrence
        int lastDelimStart = firstDelimStart;
        while (matcher.find()) {
            lastDelimStart = matcher.start();
        }

        // firstToken: text + trailing delimiter, mapped back to original value (includes leading delimiters)
        if (firstDelimStart > 0) {
            firstTokenCounts.merge(value.substring(0, leadingDelimLength + firstDelimEnd), 1, Integer::sum);
        }

        if (lastDelimStart < inner.length() - 1) {
            lastTokenCounts.merge(value.substring(leadingDelimLength + lastDelimStart), 1, Integer::sum);
        }
    }

    /**
     * Determines whether the given attribute should be treated as a DN attribute
     * based on its local name (case-insensitive).
     */
    boolean isDnAttribute(@NotNull QName attrName) {
        return DN_ATTRIBUTE_LOCAL_NAMES.contains(attrName.getLocalPart().toLowerCase());
    }

    /** Checks whether the given string matches the URL pattern. */
    private boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    /** Checks whether the given string matches the email address pattern. */
    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /** Checks whether the given string matches the phone number pattern. */
    private boolean isPhoneNumber(String value) {
        return value != null && PHONE_PATTERN.matcher(value).matches();
    }

    /**
     * Parses the given distinguished name (DN) string to extract the suffix starting
     * with the first "OU" or "O" (organizational unit) RDN (case-insensitive).
     */
    private String parseDNSuffix(String dn) {
        try {
            LdapName ldapName = new LdapName(dn);
            List<Rdn> rdns = ldapName.getRdns();

            int ouIndex = -1;
            for (int i = rdns.size() - 1; i >= 0; i--) {
                String type = rdns.get(i).getType();
                if (type.equalsIgnoreCase("ou") || type.equalsIgnoreCase("o")) {
                    ouIndex = i;
                    break;
                }
            }

            if (ouIndex == -1) {
                return null;
            }

            return new LdapName(rdns.subList(0, ouIndex + 1)).toString();
        } catch (InvalidNameException e) {
            LOGGER.trace("Failed to parse DN '{}': {}", dn, e.getMessage());
            return null;
        }
    }
}
