/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This encoder with encode/decode {@link OnePageParameterEncoder#PARAMETER} to path
 * and all other parameters as query parameters.
 * <p>
 * Example: /mountPoint/pathParameterValue?param2=value2&param3=value3
 *
 * @author lazyman
 */
public class OnePageParameterEncoder implements IPageParametersEncoder {

    public static final String PARAMETER = "pathParameter";

    private static final Trace LOGGER = TraceManager.getTrace(OnePageParameterEncoder.class);

    @Override
    public PageParameters decodePageParameters(Url url) {
        PageParameters parameters = new PageParameters();

        List<String> segments = url.getSegments();
        if (!segments.isEmpty()) {
            String value = segments.get(0);
            parameters.add(PARAMETER, value);
        }

        if (url.getQueryParameters() != null) {
            for (Url.QueryParameter qp : url.getQueryParameters()) {
                parameters.add(qp.getName(), qp.getValue());
            }
        }

        return parameters.isEmpty() ? null : parameters;
    }

    @Override
    public Url encodePageParameters(PageParameters pageParameters) {
        Url url = new Url();

        for (PageParameters.NamedPair pair : pageParameters.getAllNamed()) {
            if (PARAMETER.equals(pair.getKey())) {
                url.getSegments().add(pair.getValue());
                continue;
            }

            url.addQueryParameter(pair.getKey(), pair.getValue());
        }

        return url;
    }

    public static String getParameter(@NotNull Page page) {
        PageParameters parameters = page.getPageParameters();
        LOGGER.trace("Page parameters: {}", parameters);

        StringValue value = parameters.get(PARAMETER);
        LOGGER.trace("OID parameter: {}", value);

        if (value == null) {
            return null;
        }

        String str = value.toString();
        if (StringUtils.isBlank(str)) {
            return null;
        }

        return str;
    }
}
