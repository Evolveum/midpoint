/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.breadcrumbs;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.web.util.NewWindowNotifyingBehavior;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbPageInstance extends Breadcrumb {

    private final WebPage page;

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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        BreadcrumbPageInstance that = (BreadcrumbPageInstance) o;

        return Objects.equals(page, that.page);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { page });
    }
}
