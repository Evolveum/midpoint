/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

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
}
