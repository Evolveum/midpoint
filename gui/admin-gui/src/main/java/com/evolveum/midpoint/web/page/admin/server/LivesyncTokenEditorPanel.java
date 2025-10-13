/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ValuesEntry;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
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

public class LivesyncTokenEditorPanel extends BasePanel<PrismObjectWrapper<TaskType>> implements Popupable {

    private static final Trace LOGGER = TraceManager.getTrace(LivesyncTokenEditorPanel.class);

    private static final String ID_TOKEN = "token";
    private static final String ID_OK = "ok";
    private static final String ID_CANCEL = "cancel";

    public static final ItemPath PATH_TOKEN = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY, ActivityStateType.F_WORK_STATE, LiveSyncWorkStateType.F_TOKEN);

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
                PrismProperty<T> token = task.findProperty(PATH_TOKEN);

                if (token == null) {
                    return null;
                }

                return token.getRealValue();
            }

            @Override
            public void setObject(T object) {
                PrismObjectWrapper<TaskType> taskWrapper = getModelObject();
                PrismObject<TaskType> task = taskWrapper.getObject();
                PrismProperty<T> token = task.findProperty(PATH_TOKEN);

                if (token == null) {
                    try {
                        token = task.findOrCreateProperty(PATH_TOKEN);
                    } catch (SchemaException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Cannot create token property", e);
                        getSession().error(getString("LivesyncTokenEditorPanel.create.token.failed", e.getMessage()));
                        return;
                    }
                }

                token.setRealValue(object);
            }
        };

        TextPanel<T> tokenPanel = new TextPanel<>(ID_TOKEN, syncTokenModel);
        tokenPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(tokenPanel);

        AjaxButton okButton = new AjaxButton(ID_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
                ObjectDelta<TaskType> tokenDelta = getTokenDelta(ajaxRequestTarget);
                if (tokenDelta == null) {
                    getSession().warn(getString("LivesyncTokenEditorPanel.token.delta.empty"));
                    return;
                }
                saveTokenPerformed(tokenDelta, ajaxRequestTarget);
            }
        };
        add(okButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL, createStringResource("Button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        add(cancelButton);
    }

    private <T> ObjectDelta<TaskType> getTokenDelta(AjaxRequestTarget target) {
        TextPanel<T> tokenPanel = (TextPanel<T>) get(ID_TOKEN);
        T newTokenValue = tokenPanel.getBaseFormComponent().getModelObject();
        try {
            PrismProperty<T> tokenProperty = getModelObject().getObject().findProperty(PATH_TOKEN);

            if (tokenProperty == null) {
                tokenProperty = getModelObject().getObject().findOrCreateProperty(PATH_TOKEN);
            }

            S_ValuesEntry valuesEntry = getPrismContext().deltaFor(TaskType.class).item(PATH_TOKEN, tokenProperty.getDefinition());
            if (newTokenValue == null) {
                return valuesEntry.replace().asObjectDelta(getModelObject().getOid());
            }
            return valuesEntry.replace(newTokenValue).asObjectDelta(getModelObject().getOid());
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot modify token", e);
            getSession().error(getString("LivesyncTokenEditorPanel.modify.token.failed", e.getMessage()));
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
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("LivesyncTokenEditorPanel.manage.token");
    }
}
