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

import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Iterator;

/**
 * @author lazyman
 */
public class DefaultPageParametersEncoder implements IPageParametersEncoder {

    /**
     * Encodes URL like this: /mountpoint/paramName1/paramValue1/paramName2/paramValue2
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
     * Decodes URL like this: /mountpoint/paramName1/paramValue1/paramName2/paramValue2
     */
    @Override
    public PageParameters decodePageParameters(Url url) {
        PageParameters parameters = new PageParameters();

        for (Iterator<String> segment = url.getSegments().iterator(); segment.hasNext(); ) {
            String key = segment.next();
            String value = segment.next();

            parameters.add(key, value);
        }

        return parameters.isEmpty() ? null : parameters;
    }
}
