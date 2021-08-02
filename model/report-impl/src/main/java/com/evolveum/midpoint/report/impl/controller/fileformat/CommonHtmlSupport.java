/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.MoreObjects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.evolveum.midpoint.report.impl.controller.fileformat.CsvController.toCharacter;

/**
 * Generally useful methods for dealing with HTML files.
 * To be used in HTML report writer.
 */
class CommonHtmlSupport {

    private static final String REPORT_CSS_STYLE_FILE_NAME = "html-report-style.css";
    static final String REPORT_GENERATED_ON = "Widget.generatedOn";

    private String cssStyle;
    private final Clock clock;
    private CompiledObjectCollectionView compiledView;

    public CommonHtmlSupport(Clock clock, CompiledObjectCollectionView compiledView) {
        this.clock = clock;
        this.compiledView = compiledView;
    }

    String getMultivalueDelimiter() {
        return "<br>";
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

    String getTableName() {
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
}
