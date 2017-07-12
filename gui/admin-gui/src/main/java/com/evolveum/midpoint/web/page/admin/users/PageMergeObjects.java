/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.CountableLoadableModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.MergeObjectsPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
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
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MERGE_OBJECTS_URL,
                label = "PageMergeObjects.auth.mergeObjects.label",
                description = "PageMergeObjects.auth.mergeObjects.description") })
public class PageMergeObjects<F extends FocusType> extends PageAdminFocus {
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
        return new FocusMainPanel<UserType>(id, getObjectModel(), new CountableLoadableModel<AssignmentEditorDto>() {
            @Override
            protected List<AssignmentEditorDto> load() {
                return new ArrayList<>();
            }
        },
                new LoadableModel<List<FocusSubwrapperDto<ShadowType>>>() {
                    @Override
                    protected List<FocusSubwrapperDto<ShadowType>> load() {
                        return new ArrayList<>();
                    }
                }, this){
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
    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel(){
        UserSummaryPanel summaryPanel = new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel());
        setSummaryPanelVisibility(summaryPanel);
        return summaryPanel;
    }

    @Override
    protected void setSummaryPanelVisibility(FocusSummaryPanel summaryPanel){
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
    public boolean isEditingFocus() {
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
            result.recordFatalError("Couldn't merge objects.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't merge objects", ex);
            showResult(result);
        }
    }
}
