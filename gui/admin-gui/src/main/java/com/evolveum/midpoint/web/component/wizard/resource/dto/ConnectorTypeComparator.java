/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author lazyman
 */
public class ConnectorTypeComparator implements Comparator<ConnectorType>, Serializable {

    @Override
    public int compare(ConnectorType c1, ConnectorType c2) {
        //todo improve
        return String.CASE_INSENSITIVE_ORDER.compare(WebComponentUtil.getName(c1), WebComponentUtil.getName(c2));
    }
}
