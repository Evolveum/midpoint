/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class LivesyncTokenEditorPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements Popupable {

    private static final transient Trace LOGGER = TraceManager.getTrace(LivesyncTokenEditorPanel.class);

    private static final String ID_TOKEN = "token";
    private static final String ID_OK = "ok";
    private static final String ID_CANCEL = "cancel";

    private ItemPath tokenPath = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN);

    public LivesyncTokenEditorPanel(String id, IModel<PrismObjectWrapper<TaskType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private <T> void initLayout() {
        IModel<T> syncTokenModel = new IModel<T>() {

            @Override
            public T getObject() {
                PrismObjectWrapper<TaskType> taskWrapper = getModelObject();
                PrismObject<TaskType> task = taskWrapper.getObject();
                PrismProperty<T> token = task.findProperty(tokenPath);

                if (token == null) {
                    return null;
                }

                return token.getRealValue();
            }

            @Override
            public void setObject(T object) {
                PrismObjectWrapper<TaskType> taskWrapper = getModelObject();
                PrismObject<TaskType> task = taskWrapper.getObject();
                PrismProperty<T> token = task.findProperty(tokenPath);

                if (token == null) {
                    try {
                        token = task.findOrCreateProperty(tokenPath);
                    } catch (SchemaException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Cannot create token property", e);
                        getSession().error("Cannot create token property: " + e.getMessage());
                        return;
                    }
                }

                token.setRealValue(object);
            }
        };

        TextPanel<T> tokenPanel = new TextPanel<>(ID_TOKEN, syncTokenModel);
        tokenPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(tokenPanel);

        AjaxButton okButton = new AjaxButton(ID_OK, createStringResource("button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
                ObjectDelta<TaskType> tokenDelta = getTokenDelta(ajaxRequestTarget);
                if (tokenDelta == null) {
                    getSession().warn("Nothing to save. Token was not changed");
                    return;
                }
                saveTokenPerformed(tokenDelta, ajaxRequestTarget);
            }
        };
        add(okButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL, createStringResource("button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        add(cancelButton);
    }

    private <T> ObjectDelta<TaskType> getTokenDelta(AjaxRequestTarget target) {
        TextPanel<T> tokenPanel = (TextPanel<T>)get(ID_TOKEN);
        T newTokenValue = tokenPanel.getBaseFormComponent().getModelObject();
        try {
            PrismProperty<T> tokenProperty = getModelObject().getObject().findProperty(tokenPath);

            S_ValuesEntry valuesEntry = getPrismContext().deltaFor(TaskType.class).item(tokenPath, tokenProperty.getDefinition());
            if (newTokenValue == null) {
                return valuesEntry.replace().asObjectDelta(getModelObject().getOid());
            }
            return valuesEntry.replace(newTokenValue).asObjectDelta(getModelObject().getOid());
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot modify token", e);
            getSession().error("Cannot modify token: " + e.getMessage());
            target.add(getPageBase().getFeedbackPanel());
            return null;
        }
    }

    protected void saveTokenPerformed(ObjectDelta<TaskType> tokenDelta, AjaxRequestTarget target) {

    }

    @Override
    public int getWidth() {
        return 400;
    }

    @Override
    public int getHeight() {
        return 200;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("LivesyncTokenEditorPanel.manage.token");
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
