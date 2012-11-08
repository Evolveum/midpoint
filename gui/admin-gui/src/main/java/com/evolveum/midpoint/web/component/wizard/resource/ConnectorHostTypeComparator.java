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
