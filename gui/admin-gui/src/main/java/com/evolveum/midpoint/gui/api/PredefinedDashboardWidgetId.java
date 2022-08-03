/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author semancik
 */
public enum PredefinedDashboardWidgetId {

    SEARCH("search"),
    SHORTCUTS("shortcuts"),
    MY_WORKITEMS("myWorkItems"),
    MY_REQUESTS("myRequests"),
    MY_ASSIGNMENTS("myAssignments"),
    PREVIEW_WIDGETS("previewWidgets"),
    MY_ACCOUNTS("myAccounts");

    private final QName qname;
    private final String uri;

    PredefinedDashboardWidgetId(String localPart) {
        this.qname = new QName(ComponentConstants.NS_DASHBOARD_WIDGET, localPart);
        this.uri = QNameUtil.qNameToUri(qname);
    }

    public QName getQname() {
        return qname;
    }

    public String getUri() {
        return uri;
    }

}
