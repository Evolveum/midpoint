/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;

public abstract class AbstractObjectMainPanel<O extends ObjectType, M extends ObjectDetailsModels<O>> extends Panel {

    private ContainerPanelConfigurationType panelConfiguration;
    private M objectDetailsModels;

    public AbstractObjectMainPanel(String id, M model, ContainerPanelConfigurationType config) {
        super(id);
        this.objectDetailsModels = model;
        this.panelConfiguration = config;

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected M getObjectDetailsModels() {
        return objectDetailsModels;
    }

    public LoadableModel<PrismObjectWrapper<O>> getObjectWrapperModel() {
        return objectDetailsModels.getObjectWrapperModel();
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return getObjectWrapperModel().getObject();
    }

    protected abstract void initLayout();

    public ContainerPanelConfigurationType getPanelConfiguration() {
        return panelConfiguration;
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getContainerPath());
    }

    private ItemPath getContainerPath() {
        if (panelConfiguration.getPath() == null) {
            return null;
        }
        return panelConfiguration.getPath().getItemPath();
    }

    public <C extends Containerable> Class<C> getTypeClass() {
        return (Class<C>) WebComponentUtil.qnameToClass(getPrismContext(), getType());
    }

    public QName getType() {
        return getPanelConfiguration().getType();
    }

    protected PageBase getPageBase() {
        return WebComponentUtil.getPageBase(this);
    }

    protected PrismContext getPrismContext() {
        return getPageBase().getPrismContext();
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this).setModel(null)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        return createStringResource(e, null);
    }

    private StringResourceModel createStringResource(Enum<?> e, String prefix) {
        return createStringResource(e, prefix, null);
    }

    private StringResourceModel createStringResource(Enum<?> e, String prefix, String nullKey) {
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
}
