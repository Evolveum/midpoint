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
