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
 *
 */
public enum GuiFeature {

    ORGTREE_EXPAND_ALL("orgTreeExpandAll"),
    ORGTREE_COLLAPSE_ALL("orgTreeCollapseAll");

    private String uri;

    private GuiFeature(String suffix) {
        this.uri = QNameUtil.qNameToUri(new QName(GuiConstants.NS_UI_FEATURE, suffix));
    }

    public String getUri() {
        return uri;
    }

}
