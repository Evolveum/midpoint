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

package com.evolveum.midpoint.web.page.admin.users;

import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.*;

import java.util.List;

/**
 * @author lazyman
 */
public class PageUsersEncoder implements IPageParametersEncoder {

    @Override
    public PageParameters decodePageParameters(Url url) {
        PageParameters parameters = new PageParameters();

        List<Url.QueryParameter> queryParams = url.getQueryParameters();
        for (Url.QueryParameter param : queryParams) {
            if (param.getValue() == null) {
                continue;
            }

            parameters.add(param.getName(), param.getValue());
        }

        return parameters.isEmpty() ? null : parameters;
    }

    @Override
    public Url encodePageParameters(PageParameters pageParameters) {
        Url url = new Url();

        for (PageParameters.NamedPair pair : pageParameters.getAllNamed()) {
            url.addQueryParameter(pair.getKey(), pair.getValue());
        }

        return url;
    }
}
