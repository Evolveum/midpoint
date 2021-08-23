/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Generally useful methods for dealing with HTML files.
 * To be used in HTML report writer.
 */
class CommonHtmlSupport {

    private static final String REPORT_CSS_STYLE_FILE_NAME = "html-report-style.css";
    static final String REPORT_GENERATED_ON = "Widget.generatedOn";
    static final String REPORT_WIDGET_TABLE_NAME = "Widget.tableName";

    static final String VALUE_CSS_STYLE_TAG = "#css-style";

    static final String LABEL_COLUMN = "label";
    static final String NUMBER_COLUMN = "number";
    static final String STATUS_COLUMN = "status";

    static final List<String> HEADS_OF_WIDGET =
            ImmutableList.of(LABEL_COLUMN, NUMBER_COLUMN, STATUS_COLUMN);

    private String cssStyle;
    private final Clock clock;
    private final CompiledObjectCollectionView compiledView;

    public CommonHtmlSupport(Clock clock, CompiledObjectCollectionView compiledView) {
        this.clock = clock;
        this.compiledView = compiledView;
    }

    String getCssStyle() {
        if (StringUtils.isEmpty(cssStyle)) {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(REPORT_CSS_STYLE_FILE_NAME);
            if (in == null) {
                throw new IllegalStateException("Resource " + REPORT_CSS_STYLE_FILE_NAME + " couldn't be found");
            }
            try {
                byte[] data = IOUtils.toByteArray(in);
                cssStyle = new String(data, Charset.defaultCharset());
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't convert context of resource " + REPORT_CSS_STYLE_FILE_NAME + " to byte.", e);
            }
        }
        return cssStyle;
    }

    String getActualTime() {
        long millis = clock.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.getDefault());
        return dateFormat.format(millis);
    }

    String getTableName(LocalizationService localizationService) {
        DisplayType display = compiledView.getDisplay();
        if (display != null && display.getLabel() != null) {
            return GenericSupport.getMessage(localizationService, display.getLabel());
        }
        return compiledView.getViewIdentifier();
    }

    String getCssClassOfTable() {
        DisplayType display = compiledView.getDisplay();
        return display != null ? display.getCssClass() : null;
    }

    String getCssStyleOfTable() {
        DisplayType display = compiledView.getDisplay();
        return display != null ? display.getCssStyle() : null;
    }

    static List<String> getHeadsOfWidget() {
        return HEADS_OF_WIDGET;
    }

    static int getIndexOfNumberColumn() {
        return HEADS_OF_WIDGET.indexOf(NUMBER_COLUMN);
    }
}
