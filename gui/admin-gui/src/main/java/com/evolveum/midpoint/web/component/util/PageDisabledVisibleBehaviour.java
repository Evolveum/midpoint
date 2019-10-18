/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.markup.html.WebPage;

/**
 * Behavior which disables component if actual page class equals to disabledPage defined by constructor parameter.
 *
 * @author lazyman
 */
public class PageDisabledVisibleBehaviour extends VisibleEnableBehaviour {

    private WebPage page;
    private Class<? extends WebPage> disabledPage;

    public PageDisabledVisibleBehaviour(WebPage page, Class<? extends WebPage> disabledPage) {
        this.disabledPage = disabledPage;
        this.page = page;
    }

    @Override
    public boolean isEnabled() {
        return !disabledPage.equals(page.getClass());
    }
}
