/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.breadcrumbs;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbPageInstance extends Breadcrumb {

    private WebPage page;

    public BreadcrumbPageInstance(IModel<String> label, WebPage page) {
        super(label);

        Validate.notNull(page, "Page must not be null");

        this.page = page;
    }

    public WebPage getPage() {
        return page;
    }

    @Override
    public void redirect(Component component) {
        component.setResponsePage(page);
    }

    @Override
    public boolean isLink() {
        return true;
    }
}
