/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Iterator;


/**
 * @author lazyman
 */
public class MidPointPageParametersEncoder implements IPageParametersEncoder {

    public static final MidPointPageParametersEncoder ENCODER = new MidPointPageParametersEncoder();

    private static final Trace LOGGER = TraceManager.getTrace(MidPointPageParametersEncoder.class);

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

        if (LOGGER.isTraceEnabled() && !pageParameters.isEmpty()) {
            LOGGER.trace("Parameters '{}' encoded to: '{}'", pageParameters, url.toString());
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
    public PageParameters decodePageParameters(Url url) {
        PageParameters parameters = new PageParameters();

        for (Iterator<String> segment = url.getSegments().iterator(); segment.hasNext(); ) {
            String key = segment.next();
            if (segment.hasNext()) {
                String value = segment.next();

                if (value != null) {
                    parameters.add(key, value);
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Parameters '{}' encoded from: '{}'", parameters, url.toString());
        }

        return parameters.isEmpty() ? null : parameters;
    }

}
