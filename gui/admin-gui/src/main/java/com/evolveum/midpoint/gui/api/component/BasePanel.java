/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Base class for most midPoint GUI panels. It has a constructor and
 * utility methods for convenient handling of the model. It also has
 * other utility methods often used in reusable components.
 * <p>
 * Almost all reusable components should extend this class.
 *
 * @author lazyman
 * @author semancik
 */
public class BasePanel<T> extends Panel {

    private static final long serialVersionUID = 1L;

    private IModel<T> model;

    public BasePanel(String id) {
        super(id);
    }

    public BasePanel(String id, IModel<T> model) {
        super(id);
        this.model = model == null ? createModel() : model;
    }

    public IModel<T> createModel() {
        return null;
    }

    public IModel<T> getModel() {
        return model;
    }

    public T getModelObject() {
        return model != null ? model.getObject() : null;
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public String getString(Enum<?> e) {
        return createStringResource(e).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, IModel<?> model, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(model)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(null)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyString polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            // TODO later: use polystringKey.getKey()
            resourceKey = polystringKey.getOrig();
        }
        return new StringResourceModel(resourceKey).setModel(null)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(PolyStringType polystringKey, Object... objects) {
        String resourceKey = null;
        if (polystringKey != null) {
            // TODO later: use polystringKey.getKey()
            resourceKey = polystringKey.getOrig();
        }
        return new StringResourceModel(resourceKey).setModel(null)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        return createStringResource(e, null);
    }

    public StringResourceModel createStringResource(Enum<?> e, String prefix) {
        return createStringResource(e, prefix, null);
    }

    public StringResourceModel createStringResource(Enum<?> e, String prefix, String nullKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix).append('.');
        }

        if (e == null) {
            if (StringUtils.isNotEmpty(nullKey)) {
                sb.append(nullKey);
            } else {
                sb = new StringBuilder();
            }
        } else {
            sb.append(e.getDeclaringClass().getSimpleName()).append('.');
            sb.append(e.name());
        }

        return createStringResource(sb.toString());
    }

    @Contract(pure = true)
    public PageBase getPageBase() {
        return WebComponentUtil.getPageBase(this);
    }

    protected void showResult(OperationResult result) {
        WebComponentUtil.getPage(this, PageAdminLTE.class).showResult(result);
    }

    public Component getFeedbackPanel() {
        return WebComponentUtil.getPage(this, PageAdminLTE.class).getFeedbackPanel();
    }

    public GuiComponentRegistry getRegistry() {
        return WebComponentUtil.getPage(this, PageAdminLTE.class).getRegistry();
    }

    public MidpointConfiguration getMidpointConfiguration() {
        return WebComponentUtil.getPage(this, PageAdminLTE.class).getMidpointConfiguration();
    }

    public PrismContext getPrismContext() {
        return WebComponentUtil.getPage(BasePanel.this, PageAdminLTE.class).getPrismContext();
    }

    public SchemaService getSchemaService() {
        return WebComponentUtil.getPage(BasePanel.this, PageAdminLTE.class).getSchemaService();
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = MidPointApplication.get();
        return application.getWebApplicationConfiguration();
    }

    @Override
    public MidPointAuthWebSession getSession() {
        return (MidPointAuthWebSession) super.getSession();
    }
}
