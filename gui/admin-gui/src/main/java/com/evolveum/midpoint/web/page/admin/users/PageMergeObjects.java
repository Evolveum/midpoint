/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.MergeObjectsPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/mergeObjects", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                label = "PageAdminUsers.auth.usersAll.label",
                description = "PageAdminUsers.auth.usersAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MERGE_OBJECTS_URL,
                label = "PageMergeObjects.auth.mergeObjects.label",
                description = "PageMergeObjects.auth.mergeObjects.description") })
public class PageMergeObjects<F extends FocusType> extends PageAdminFocus {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageMergeObjects.class.getName() + ".";
    private static final String OPERATION_DELETE_USER = DOT_CLASS + "deleteUser";
    private static final String OPERATION_MERGE_OBJECTS = DOT_CLASS + "mergeObjects";
    private static final Trace LOGGER = TraceManager.getTrace(PageMergeObjects.class);
    private F mergeObject;
    private IModel<F> mergeObjectModel;
    private F mergeWithObject;
    private IModel<F> mergeWithObjectModel;
    private Class<F> type;
    private MergeObjectsPanel mergeObjectsPanel;

    public PageMergeObjects(){
    }

    public PageMergeObjects(F mergeObject, F mergeWithObject, Class<F> type){
        this.mergeObject = mergeObject;
        this.mergeWithObject = mergeWithObject;
        this.type = type;

        initModels();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, mergeObject.getOid());
        getPageParameters().overwriteWith(parameters);
        initialize(this.mergeObject.asPrismObject());
    }

    private void initModels(){
        mergeObjectModel = new IModel<F>() {
            @Override
            public F getObject() {
                return mergeObject;
            }

            @Override
            public void setObject(F f) {
                mergeObject = f;
            }

            @Override
            public void detach() {

            }
        };
        mergeWithObjectModel = new IModel<F>() {
            @Override
            public F getObject() {
                return mergeWithObject;
            }

            @Override
            public void setObject(F f) {
                mergeWithObject = f;
            }

            @Override
            public void detach() {

            }
        };
    }

    @Override
    protected AbstractObjectMainPanel<UserType> createMainPanel(String id){

        //empty assignments model
        CountableLoadableModel<AssignmentType> assignemtns = new CountableLoadableModel<AssignmentType>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentType> load() {
                return new ArrayList<>();
            }
        };

        //empty policy rules  model
         CountableLoadableModel<AssignmentType> policyRules = new CountableLoadableModel<AssignmentType>() {
             private static final long serialVersionUID = 1L;

             @Override
             protected List<AssignmentType> load() {
                 return new ArrayList<>();
             }
         };

         //empty projections model
         LoadableModel<List<ShadowWrapper>> shadows = new LoadableModel<List<ShadowWrapper>>() {
             private static final long serialVersionUID = 1L;
                     @Override
                     protected List<ShadowWrapper> load() {
                         return new ArrayList<>();
                     }
                 };

        return new FocusMainPanel<UserType>(id, getObjectModel(), shadows, this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(final PageAdminObjectDetails<UserType> parentPage) {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(
                        new PanelTab(parentPage.createStringResource("PageMergeObjects.tabTitle"), new VisibleEnableBehaviour()){
                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                mergeObjectsPanel =  new MergeObjectsPanel(panelId, mergeObjectModel, mergeWithObjectModel, type, PageMergeObjects.this);
                                return mergeObjectsPanel;
                            }
                        });
                return tabs;
            }

            @Override
            protected boolean isPreviewButtonVisible(){
                return false;
            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                return false;
            }
        };
    }


    //TODO did it work before?
    @Override
    protected ObjectSummaryPanel createSummaryPanel(IModel summaryModel) {
        UserSummaryPanel summaryPanel = new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel(), this);
        return summaryPanel;
    }

    @Override
    protected void setSummaryPanelVisibility(ObjectSummaryPanel summaryPanel){
        summaryPanel.setVisible(false);
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageUsers.class;
    }

    protected UserType createNewObject(){
        return new UserType();
    }

    @Override
    public Class getCompileTimeClass() {
        return type;
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMergeObjects.title");
    }

    @Override
    public boolean isOidParameterExists() {
        return true;
    }

    @Override
    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
        try {
            Task task = createSimpleTask(OPERATION_MERGE_OBJECTS);
            getModelService().mergeObjects(type, mergeObject.getOid(), mergeWithObject.getOid(),
                    mergeObjectsPanel.getMergeConfigurationName(), task, result);
            result.computeStatusIfUnknown();
            showResult(result);
            redirectBack();

        } catch (Exception ex){
            result.recomputeStatus();
            result.recordFatalError(getString("PageMergeObjects.message.saveOrPreviewPerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't merge objects", ex);
            showResult(result);
        }
    }
}
