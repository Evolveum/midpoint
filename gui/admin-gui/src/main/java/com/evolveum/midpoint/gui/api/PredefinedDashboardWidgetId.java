/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    private final String identifier;

    PredefinedDashboardWidgetId(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

}
