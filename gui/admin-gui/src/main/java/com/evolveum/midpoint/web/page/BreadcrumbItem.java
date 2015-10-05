/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbItem implements Serializable {

    private Class<WebPage> page;
    private PageParameters pageParameters;
    private IModel<String> icon;
    private IModel<String> name;

    public BreadcrumbItem(IModel<String> name, Class<WebPage> page) {
        this.name = name;
        this.page = page;
    }

    public BreadcrumbItem(IModel<String> name, IModel<String> icon, Class<WebPage> page) {
        this.icon = icon;
        this.name = name;
        this.page = page;
    }

    public IModel<String> getIcon() {
        return icon;
    }

    public IModel<String> getName() {
        return name;
    }

    public Class<WebPage> getPage() {
        return page;
    }

    public PageParameters getPageParameters() {
        return pageParameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BreadcrumbItem{");
        sb.append("icon=").append(icon);
        sb.append(", page=").append(page);
        sb.append(", pageParameters=").append(pageParameters);
        sb.append(", name=").append(name);
        sb.append('}');
        return sb.toString();
    }
}
