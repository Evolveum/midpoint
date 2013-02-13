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

import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Iterator;


/**
 * @author lazyman
 */
public class MidPointPageParametersEncoder implements IPageParametersEncoder {
    /**
     * Encodes a URL in the form:
     * <p/>
     * /mountpoint/paramName1/paramValue1/paramName2/paramValue2
     * <p/>
     * (i.e. a URL using the pre wicket 1.5 Hybrid URL strategy)
     */
    @Override
    public Url encodePageParameters(PageParameters pageParameters) {
        Url url = new Url();

        for (PageParameters.NamedPair pair : pageParameters.getAllNamed()) {
            url.getSegments().add(pair.getKey());
            url.getSegments().add(pair.getValue());
        }

        return url;
    }

    /**
     * Decodes a URL in the form:
     * <p/>
     * /mountpoint/paramName1/paramValue1/paramName2/paramValue2
     * <p/>
     * (i.e. a URL using the pre wicket 1.5 Hybrid URL strategy)
     */
    @Override
    public PageParameters decodePageParameters(Request request) {
        PageParameters parameters = new PageParameters();

        for (Iterator<String> segment = request.getUrl().getSegments().iterator(); segment.hasNext(); ) {
            String key = segment.next();
            if (segment.hasNext()) {
                String value = segment.next();

                if (value != null) {
                    parameters.add(key, value);
                }
            }
        }

        return parameters.isEmpty() ? null : parameters;
    }

}
