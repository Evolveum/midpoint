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
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbPageClass extends Breadcrumb {

    private Class<? extends WebPage> page;
    private PageParameters parameters;

    public BreadcrumbPageClass(IModel<String> label, Class<? extends WebPage> page, PageParameters parameters) {
        super(label);

        Validate.notNull(page, "Page class must not be null");

        this.page = page;
        this.parameters = parameters;

        setUseLink(true);
    }

    public Class<? extends WebPage> getPage() {
        return page;
    }

    public PageParameters getParameters() {
        return parameters;
    }

    public void setParameters(PageParameters parameters) {
        this.parameters = parameters;
    }

    public void setPage(Class<? extends WebPage> page) {
        this.page = page;
    }

    @Override
    public void redirect(Component component) {
        if (parameters == null) {
            component.setResponsePage(page);
        } else {
            component.setResponsePage(page, parameters);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BreadcrumbPageClass that = (BreadcrumbPageClass) o;

        if (page != null ? !page.equals(that.page) : that.page != null) return false;
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{page, parameters});
    }
}
