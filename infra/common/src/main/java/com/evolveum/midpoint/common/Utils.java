/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 *
 */
public class Utils {

    private static final Trace LOGGER = TraceManager.getTrace(Utils.class);

    public static String getPropertyName(String name) {
        if (null == name) {
            return "";
        }
        return StringUtils.lowerCase(name);
    }

    /**
     * Removing non-printable UTF characters from the string.
     *
     * This is not really used now. It was done as a kind of prototype for
     * filters. But may come handy and it in fact tests that the pattern is
     * doing what expected, so it may be useful.
     *
     * @param bad
     *            string with bad chars
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
     *
     * @param c
     * @return
     */
    public static boolean isValidXmlCodepoint(int cp) {
        return (cp == 0x0009 || cp == 0x000a || cp == 0x000d || (cp >= 0x0020 && cp <= 0xd7ff)
                || (cp >= 0xe000 && cp <= 0xfffd) || (cp >= 0x10000 && cp <= 0x10FFFF));
    }
}
