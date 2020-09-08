/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public abstract class AbstractObjectTabPanel<O extends ObjectType> extends Panel {
    private static final long serialVersionUID = 1L;

    protected static final String ID_MAIN_FORM = "mainForm";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectTabPanel.class);

    private final LoadableModel<PrismObjectWrapper<O>> objectWrapperModel;
    private final MidpointForm<PrismObjectWrapper<O>> mainForm;

    public AbstractObjectTabPanel(String id, MidpointForm<PrismObjectWrapper<O>> mainForm, LoadableModel<PrismObjectWrapper<O>> objectWrapperModel) {
        super(id);
        this.objectWrapperModel = objectWrapperModel;
        this.mainForm = mainForm;
    }

    public LoadableModel<PrismObjectWrapper<O>> getObjectWrapperModel() {
        return objectWrapperModel;
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return objectWrapperModel.getObject();
    }

    protected PrismContext getPrismContext() {
        return getPageBase().getPrismContext();
    }

    protected PageParameters getPageParameters() {
        return getPageBase().getPageParameters();
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public MidpointForm<PrismObjectWrapper<O>> getMainForm() {
        return mainForm;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    protected void showResult(OperationResult result) {
        getPageBase().showResult(result);
    }

    protected void showResult(OperationResult result, boolean showSuccess) {
        getPageBase().showResult(result, showSuccess);
    }

    protected WebMarkupContainer getFeedbackPanel() {
        return getPageBase().getFeedbackPanel();
    }

    protected void showModalWindow(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
        target.add(getFeedbackPanel());
    }

    protected Panel addPrismPropertyPanel(MarkupContainer parentComponent, String id, QName typeName, ItemPath propertyPath) {

        try {
            ItemPanelSettingsBuilder settingsBuilder = new ItemPanelSettingsBuilder();
            settingsBuilder.visibilityHandler(wrapper -> ItemVisibility.AUTO);

            Panel panel = getPageBase().initItemPanel(id, typeName, PrismPropertyWrapperModel.fromContainerWrapper(getObjectWrapperModel(), propertyPath), settingsBuilder.build());
            parentComponent.add(panel);
            return panel;
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}", typeName, e);
            getSession().error("Cannot create panel for " + typeName + ", reason: " + e.getMessage());
        }

        return null;
    }
}
