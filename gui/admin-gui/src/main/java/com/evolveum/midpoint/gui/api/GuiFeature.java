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
