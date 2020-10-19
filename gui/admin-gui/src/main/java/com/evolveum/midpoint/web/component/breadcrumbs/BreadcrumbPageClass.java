/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.breadcrumbs;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author Viliam Repan (lazyman)
 */
public class BreadcrumbPageClass extends Breadcrumb {
    private static final long serialVersionUID = 1L;

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

    @Override
    public PageParameters getParameters() {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        return parameters;
    }

    public void setParameters(PageParameters parameters) {
        this.parameters = parameters;
    }

    public void setPage(Class<? extends WebPage> page) {
        this.page = page;
    }

    @Override
    public WebPage redirect() {
        IPageFactory pFactory = Session.get().getPageFactory();
        if (parameters == null) {
            return pFactory.newPage(page);
        } else {
            return pFactory.newPage(page, parameters);
        }
    }

    @Override
    public RestartResponseException getRestartResponseException() {
        if (parameters == null) {
            return new RestartResponseException(page);
        } else {
            return new RestartResponseException(page, parameters);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        BreadcrumbPageClass that = (BreadcrumbPageClass) o;

        return Objects.equals(page, that.page)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { page, parameters });
    }

    @Override
    protected void extendsDebugDump(StringBuilder sb, int indent) {
        super.extendsDebugDump(sb, indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "page", page, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "parameters", parameters == null ? null : parameters.toString(), indent + 1);
    }

}
