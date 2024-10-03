/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.breadcrumbs;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Breadcrumb object that is stored in the session. It represents the way "back" to the main menu.
 * <p>
 * We need to be extra careful about the memory references here. This object goes in the session.
 * Therefore we cannot allow models to be stored in the session. The models may have references
 * to (possibly big) pages and other rich objects. The references are there mostly to load the
 * models. But we do not want that. We want to store only the values. Therefore the model values
 * are copied to simple strings on model detach().
 *
 * @author Viliam Repan (lazyman)
 * @author semancik
 */
public class Breadcrumb implements Serializable, DebugDumpable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOG = TraceManager.getTrace(Breadcrumb.class);

    private CachedModel labelModel;

    private CachedModel iconModel;

    private Class<? extends WebPage> pageClass;

    private PageParameters parameters;

    private boolean useLink = false;

    private boolean visible = true;

    public Breadcrumb() {
    }

    public Breadcrumb(IModel<String> labelModel) {
        setLabel(labelModel);
    }

    public Breadcrumb(IModel<String> label, Class<? extends WebPage> pageClass, PageParameters parameters) {
        this(label, null, pageClass, parameters);
    }

    public Breadcrumb(IModel<String> labelModel, IModel<String> iconModel, Class<? extends WebPage> pageClass, PageParameters parameters) {
        setPageClass(pageClass);
        setParameters(parameters);

        setLabel(new CachedModel(labelModel));
        setIcon(new CachedModel(iconModel));

        setUseLink(true);
    }

    public Class<? extends WebPage> getPageClass() {
        return pageClass;
    }

    public PageParameters getParameters() {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        return parameters;
    }

    public void setParameters(PageParameters parameters) {
        this.parameters = parameters;
    }

    public void setPageClass(Class<? extends WebPage> pageClass) {
        Validate.notNull(pageClass, "Page class must not be null");

        this.pageClass = pageClass;
    }

    public IModel<String> getLabel() {
        return labelModel;
    }

    public void setLabel(final IModel<String> label) {
        if (label == null) {
            this.labelModel = new CachedModel((String) null);
            return;
        }

        this.labelModel = new CachedModel(label);
    }

    public IModel<String> getIcon() {
        return iconModel;
    }

    public void setIcon(final IModel<String> icon) {
        if (icon == null) {
            this.iconModel = new CachedModel((String) null);
            return;
        }

        this.iconModel = new CachedModel(icon);
    }

    public boolean isUseLink() {
        return useLink;
    }

    public void setUseLink(boolean useLink) {
        this.useLink = useLink;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public WebPage redirect() {
        if (pageClass == null) {
            return null;
        }

        IPageFactory pFactory = Session.get().getPageFactory();
        if (parameters == null) {
            return pFactory.newPage(pageClass);
        } else {
            return pFactory.newPage(pageClass, parameters);
        }
    }

    public RestartResponseException getRestartResponseException() {
        if (parameters == null) {
            return new RestartResponseException(pageClass);
        } else {
            return new RestartResponseException(pageClass, parameters);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        Breadcrumb that = (Breadcrumb) o;

        return Objects.equals(pageClass, that.pageClass)
                && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { pageClass, parameters });
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "labelModel", labelModel == null ? "" : labelModel.toString(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "iconModel", iconModel == null ? "" : iconModel.toString(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "useLink", useLink, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "visible", visible, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "page", pageClass, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "parameters", parameters == null ? null : parameters.toString(), indent + 1);

        return sb.toString();
    }
}
