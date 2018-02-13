/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;
import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Arrays;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbPageInstance extends Breadcrumb {

    private WebPage page;

    public BreadcrumbPageInstance(IModel<String> label, WebPage page) {
        super(label);

        setUseLink(true);

        Validate.notNull(page, "Page must not be null");

        this.page = page;
    }

    public WebPage getPage() {
        return page;
    }

    @Override
    public PageParameters getParameters() {
        return page.getPageParameters();
    }

    @Override
    public WebPage redirect() {
        List<NewWindowNotifyingBehavior> behaviors = page.getBehaviors(NewWindowNotifyingBehavior.class);
        behaviors.forEach(behavior -> page.remove(behavior));

        page.add(new NewWindowNotifyingBehavior());


        return page;
    }

	@Override
	public RestartResponseException getRestartResponseException() {
		return new RestartResponseException(page);
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BreadcrumbPageInstance that = (BreadcrumbPageInstance) o;

        return page != null ? page.equals(that.page) : that.page == null;

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{page});
    }
}
