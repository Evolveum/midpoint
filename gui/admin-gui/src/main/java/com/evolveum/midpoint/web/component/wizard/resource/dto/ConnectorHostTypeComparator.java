/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
