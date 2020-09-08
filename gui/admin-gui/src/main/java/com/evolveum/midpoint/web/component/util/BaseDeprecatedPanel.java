/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;

/**
 * @author lazyman
 */
@Deprecated
public abstract class BaseDeprecatedPanel<T> extends Panel {

    private final IModel<T> model;
    private boolean initialized;

    public BaseDeprecatedPanel(String id, IModel<T> model) {
        super(id);

        this.model = model;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (initialized) {
            return;
        }
        initLayout();
        initialized = true;
    }

    protected IModel<T> getModel() {
        return model;
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    protected void initLayout() {
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }
}
