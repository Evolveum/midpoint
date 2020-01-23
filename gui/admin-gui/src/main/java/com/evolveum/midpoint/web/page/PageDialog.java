/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page;

import org.apache.wicket.markup.html.WebPage;

import com.evolveum.midpoint.gui.api.page.PageBase;

/**
 * Superclass for all pages that implement modal windows (dialogs).
 */
public class PageDialog extends WebPage {

    // reference to the "parent" ("calling") page that spawned this modal window page
    private PageBase pageBase;

    public PageDialog(PageBase pageBase) {
        this.pageBase = pageBase;
    }

    public PageBase getPageBase() {
        return pageBase;
    }
}
