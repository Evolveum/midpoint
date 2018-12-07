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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author lazyman
 */
public class ConnectorHostTypeComparator implements Comparator<PrismObject<ConnectorHostType>>, Serializable {

    @Override
    public int compare(PrismObject<ConnectorHostType> host1, PrismObject<ConnectorHostType> host2) {
        return String.CASE_INSENSITIVE_ORDER.compare(getUserFriendlyName(host1), getUserFriendlyName(host2));
    }

    public static String getUserFriendlyName(PrismObject<ConnectorHostType> host) {
        if (host == null) {
            return null;
        }

        String name = WebComponentUtil.getName(host);

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(name)) {
            builder.append(name);
            builder.append('(');
        }
        builder.append(getStringProperty(host, ConnectorHostType.F_HOSTNAME));

        String port = getStringProperty(host, ConnectorHostType.F_PORT);
        if (StringUtils.isNotEmpty(port)) {
            builder.append(':');
            builder.append(port);
        }
        if (StringUtils.isNotEmpty(name)) {
            builder.append(')');
        }

        return builder.toString();
    }

    private static String getStringProperty(PrismObject obj, ItemName qname) {
        return (String) obj.getPropertyRealValue(qname, String.class);
    }
}
