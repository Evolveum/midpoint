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

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorHostType;

import org.apache.commons.lang.StringUtils;

import java.util.Comparator;

/**
 * @author lazyman
 */
public class ConnectorHostTypeComparator implements Comparator<ConnectorHostType> {

    @Override
    public int compare(ConnectorHostType host1, ConnectorHostType host2) {
        return String.CASE_INSENSITIVE_ORDER.compare(getUserFriendlyName(host1), getUserFriendlyName(host2));
    }

    public static String getUserFriendlyName(ConnectorHostType host) {
        if (host == null) {
            return null;
        }

        String name = WebMiscUtil.getOrigStringFromPoly(host.getName());

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(name)) {
            builder.append(name);
            builder.append('(');
        }
        builder.append(host.getHostname());
        if (StringUtils.isNotEmpty(host.getPort())) {
            builder.append(':');
            builder.append(host.getPort());
        }
        if (StringUtils.isNotEmpty(name)) {
            builder.append(')');
        }

        return builder.toString();
    }
}
