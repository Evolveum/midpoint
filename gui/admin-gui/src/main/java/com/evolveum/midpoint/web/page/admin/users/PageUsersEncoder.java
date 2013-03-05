/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
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
