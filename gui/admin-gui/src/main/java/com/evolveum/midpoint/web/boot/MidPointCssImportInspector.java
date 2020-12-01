/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot;

import org.apache.commons.lang3.StringUtils;
import ro.isdc.wro.model.resource.processor.support.CssImportInspector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 *     Custom pattern to match @import statements also when (referece) is used
 * </p>
 */
public class MidPointCssImportInspector extends CssImportInspector {

    private static final Pattern PATTERN = Pattern.compile("(?i)(@import)(?:-once|-multiple|reference)?\\b\\s*(?:(?:url)?\\(?\\s*[\"']?)([^)\"']*)[\"']?\\)?\\s?[\"']?([.\\/]*[a-zA-Z0-9-\\/]*)[\"]?;?");

    public MidPointCssImportInspector(String cssContent) {
        super(cssContent);
    }

    @Override
    protected Matcher getMatcher(String cssContent) {
        return PATTERN.matcher(cssContent);
    }

    protected String extractImportUrl(final Matcher matcher) {
        int groupCount = matcher.groupCount();
        String lastGroup = matcher.group(groupCount);
        if (StringUtils.isNotBlank(lastGroup)) {
            return lastGroup;
        }
        return matcher.group(groupCount - 1);
    }
}
