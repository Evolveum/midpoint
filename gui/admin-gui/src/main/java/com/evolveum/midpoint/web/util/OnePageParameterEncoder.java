/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.util;

import org.apache.commons.lang.Validate;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author lazyman
 */
public class OnePageParameterEncoder implements IPageParametersEncoder {

    private String parameterName;

    public OnePageParameterEncoder(String parameterName) {
        Validate.notEmpty(parameterName, "Parameter name must not be empty.");
        this.parameterName = parameterName;
    }

    /**
     * Decodes URL like this: /mountpoint/paramValue1
     * Parameter value is in URL with name defined in contructor
     */
    @Override
    public PageParameters decodePageParameters(Url url) {
        PageParameters parameters = new PageParameters();

        List<String> segments = url.getSegments();
        if (!segments.isEmpty()) {
            String value = segments.get(0);
            parameters.add(parameterName, value);
        }

        return parameters.isEmpty() ? null : parameters;
    }

    /**
     * Encodes URL like this: /mountpoint/paramValue1
     */
    @Override
    public Url encodePageParameters(PageParameters pageParameters) {
        Url url = new Url();

        for (PageParameters.NamedPair pair : pageParameters.getAllNamed()) {
            if (!parameterName.equals(pair.getKey())) {
                continue;
            }

            url.getSegments().add(pair.getValue());
            break;
        }

        return url;
    }
}
