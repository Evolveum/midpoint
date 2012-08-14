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

package com.evolveum.midpoint.web.component.util;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebPage;

/**
 * This is simple visible/enable behaviour for use in top menu. It always disable menu
 * link and this menu link is visible only if page class equals defined class (in constructor).
 *
 * @author lazyman
 */
public class PageVisibleDisabledBehaviour extends VisibleEnableBehaviour {

    private WebPage page;
    private Class<? extends WebPage> defaultPage;

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
        if (defaultPage.equals(page.getClass())) {
            return true;
        }
        return false;
    }
}
