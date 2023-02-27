/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class Utils {

    private static final Pattern OID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    public static String getPropertyName(String name) {
        if (null == name) {
            return "";
        }
        return StringUtils.lowerCase(name);
    }

    /**
     * Removing non-printable UTF characters from the string.
     * <p>
     * This is not really used now. It was done as a kind of prototype for
     * filters. But may come handy and it in fact tests that the pattern is
     * doing what expected, so it may be useful.
     *
     * @param bad string with bad chars
     * @return string without bad chars
     */
    public static String cleanupUtf(String bad) {

        StringBuilder sb = new StringBuilder(bad.length());

        for (int cp, i = 0; i < bad.length(); i += Character.charCount(cp)) {
            cp = bad.codePointAt(i);
            if (isValidXmlCodepoint(cp)) {
                sb.append(Character.toChars(cp));
            }
        }

        return sb.toString();
    }

    /**
     * According to XML specification, section 2.2:
     * http://www.w3.org/TR/REC-xml/
     */
    public static boolean isValidXmlCodepoint(int cp) {
        return (cp == 0x0009 || cp == 0x000a || cp == 0x000d || (cp >= 0x0020 && cp <= 0xd7ff)
                || (cp >= 0xe000 && cp <= 0xfffd) || (cp >= 0x10000 && cp <= 0x10FFFF));
    }

    public static boolean isPrismObjectOidValid(String oid) {
        if (StringUtils.isEmpty(oid)) {
            return false;
        }

        return OID_PATTERN.matcher(oid).matches();
    }
}
