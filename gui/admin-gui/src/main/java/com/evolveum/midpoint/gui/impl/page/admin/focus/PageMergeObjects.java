/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.users.component.MergeObjectsPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/mergeObjects")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                label = "PageAdminUsers.auth.usersAll.label",
                description = "PageAdminUsers.auth.usersAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MERGE_OBJECTS_URL,
                label = "PageMergeObjects.auth.mergeObjects.label",
                description = "PageMergeObjects.auth.mergeObjects.description") })
public class PageMergeObjects extends PageBase {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageMergeObjects.class.getName() + ".";

    private static final String OPERATION_MERGE_OBJECTS = DOT_CLASS + "mergeObjects";

    private static final String ID_FORM = "form";
    private static final String ID_MERGE_PANEL = "mergePanel";
    private static final String ID_SAVE = "save";
    private static final String ID_BACK = "back";

    private static final Trace LOGGER = TraceManager.getTrace(PageMergeObjects.class);
    private UserType mergeObject;
    private IModel<UserType> mergeObjectModel;
    private UserType mergeWithObject;
    private IModel<UserType> mergeWithObjectModel;
    private Class<UserType> type;
    private MergeObjectsPanel mergeObjectsPanel;

    public PageMergeObjects() {
    }

    public PageMergeObjects(UserType mergeObject, UserType mergeWithObject, Class<UserType> type) {
        this.mergeObject = mergeObject;
        this.mergeWithObject = mergeWithObject;
        this.type = type;

        initModels();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, mergeObject.getOid());
        getPageParameters().overwriteWith(parameters);
        initLayout();
    }


    private void initModels() {
        mergeObjectModel = new IModel<>() {
            @Override
            public UserType getObject() {
                return mergeObject;
            }

            @Override
            public void setObject(UserType f) {
                mergeObject = f;
            }

            @Override
            public void detach() {

            }
        };
        mergeWithObjectModel = new IModel<>() {
            @Override
            public UserType getObject() {
                return mergeWithObject;
            }

            @Override
            public void setObject(UserType f) {
                mergeWithObject = f;
            }

            @Override
            public void detach() {

            }
        };
    }

    private void initLayout() {

        //empty assignments model
        CountableLoadableModel<AssignmentType> assignments = new CountableLoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentType> load() {
                return new ArrayList<>();
            }
        };

        //empty policy rules  model
        CountableLoadableModel<AssignmentType> policyRules = new CountableLoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentType> load() {
                return new ArrayList<>();
            }
        };

        //empty projections model
        LoadableModel<List<ShadowWrapper>> shadows = new LoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ShadowWrapper> load() {
                return new ArrayList<>();
            }
        };

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);
        mergeObjectsPanel = new MergeObjectsPanel<>(ID_MERGE_PANEL, mergeObjectModel, mergeWithObjectModel, type, PageMergeObjects.this);
        form.add(mergeObjectsPanel);

        createSaveButton(form);
        createBackButton(form);

    }

    private void createSaveButton(MidpointForm<?> form) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SAVE, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton save = new AjaxCompositedIconSubmitButton(ID_SAVE, iconBuilder.build(),
                createStringResource("PageBase.button.save")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        save.titleAsLabel(true);
        save.setOutputMarkupId(true);
        save.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
        form.add(save);
    }

    private void createBackButton(MidpointForm<?> form) {
        AjaxIconButton back = new AjaxIconButton(ID_BACK, Model.of(GuiStyleConstants.ARROW_LEFT),
                createStringResource("pageAdminFocus.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                redirectBack();
            }
        };

        back.showTitleAsLabel(true);
        back.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        form.add(back);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMergeObjects.title");
    }

    public void savePerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_MERGE_OBJECTS);
        OperationResult result = task.getResult();
        try {
            getModelService().mergeObjects(type, mergeObject.getOid(), mergeWithObject.getOid(),
                    mergeObjectsPanel.getMergeConfigurationName(), task, result);
            result.computeStatusIfUnknown();
            showResult(result);
            redirectBack();

        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(getString("PageMergeObjects.message.saveOrPreviewPerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't merge objects", ex);
            showResult(result);
        }
    }
}
