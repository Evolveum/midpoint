/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
