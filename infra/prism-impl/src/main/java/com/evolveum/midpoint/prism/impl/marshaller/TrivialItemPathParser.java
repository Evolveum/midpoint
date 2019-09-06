/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * TODO: This should do more parsing in the future. But good for now.
 *
 * @author semancik
 */
public class TrivialItemPathParser {
    // This is using regexp to "parse" the declarations. It is not ideal,
    // it does not check the syntax, does not show reasonable errors, etc.
    // But it was all done in like 20minutes. Good value/price ;-)
    private static final String PARSE_REGEX = "(^|;)[\\s\\p{Z}]*declare[\\s\\p{Z}]+(default[\\s\\p{Z}]+)?namespace[\\s\\p{Z}]+((\\w+)[\\s\\p{Z}]*=[\\s\\p{Z}]*)?(['\"])([^'\"]*)\\5[\\s\\p{Z}]*(?=;)";
    private static final Pattern PARSE_PATTERN = Pattern.compile(PARSE_REGEX);

    private static final String NBWS_HEAD_REGEX = "^[\\p{Z}\\s]+";
    private static final Pattern NBSP_HEAD_PATTERN = Pattern.compile(NBWS_HEAD_REGEX);

    private static final String NBWS_TAIL_REGEX = "[\\p{Z}\\s]+$";
    private static final Pattern NBWS_TAIL_PATTERN = Pattern.compile(NBWS_TAIL_REGEX);

    private final Map<String,String> namespaceMap = new HashMap<>();
    private String pureItemPathString;

    private TrivialItemPathParser() {
        pureItemPathString = "";
    }

    public static TrivialItemPathParser parse(String itemPath) {

        TrivialItemPathParser parser = new TrivialItemPathParser();


        Matcher matcher = PARSE_PATTERN.matcher(itemPath);

        int lastEnd = 0;
        while (matcher.find()) {
            String prefix = matcher.group(4);
            String url = matcher.group(6);
            if (matcher.group(2) != null) {
                // default namespace declaration
                prefix = "";
            }
            parser.namespaceMap.put(prefix, url);
            lastEnd = matcher.end();
        }

        parser.pureItemPathString = itemPath;

        if (lastEnd>0) {
            // Skip colon (as it is look-ahead assertion in the pattern) and trim
            parser.pureItemPathString = itemPath.substring(lastEnd+1).trim();
        }

        // Trim whitechars
        // trim() won't do here. it is not trimming non-breakable spaces.
        parser.pureItemPathString = NBWS_TAIL_PATTERN.matcher(NBSP_HEAD_PATTERN.matcher(parser.pureItemPathString).replaceFirst("")).replaceFirst("");

        return parser;
    }

    @NotNull
    public Map<String,String> getNamespaceMap() {
        return namespaceMap;
    }

    public String getPureItemPathString() {
        return pureItemPathString;
    }

}
