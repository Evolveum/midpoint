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
