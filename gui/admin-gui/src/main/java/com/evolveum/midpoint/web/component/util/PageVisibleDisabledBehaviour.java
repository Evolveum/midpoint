/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.html.WebPage;

/**
 * This is simple visible/enable behaviour for use in top menu. It always disable menu
 * link and this menu link is visible only if page class equals defined class (in constructor).
 *
 * @author lazyman
 */
public class PageVisibleDisabledBehaviour extends VisibleEnableBehaviour {

    private final WebPage page;
    private final Class<? extends WebPage> defaultPage;

    public PageVisibleDisabledBehaviour(WebPage page, Class<? extends WebPage> defaultPage) {
        Validate.notNull(page, "Page must not be null.");
        Validate.notNull(defaultPage, "Default page class must not be null.");

        this.page = page;
        this.defaultPage = defaultPage;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return defaultPage.equals(page.getClass());
    }
}
