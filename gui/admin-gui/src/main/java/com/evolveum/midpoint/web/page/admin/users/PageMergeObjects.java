/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.component.MergeObjectsPanel;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
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
    private static final String ID_MERGE_PANEL = "mergePanel";
    private F mergeObject;
    private F mergeWithObject;
    private Class<F> type;
    public PageMergeObjects(){
    }

    public PageMergeObjects(F mergeObject, F mergeWithObject, Class<F> type){
        this.mergeObject = mergeObject;
        this.mergeWithObject = mergeWithObject;
        this.type = type;

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, mergeObject.getOid());
        getPageParameters().overwriteWith(parameters);


        initialize(this.mergeObject.asPrismObject());
//        initLayout();
    }

    @Override
    protected AbstractObjectMainPanel<UserType> createMainPanel(String id){

        return new FocusMainPanel<UserType>(id, getObjectModel(), new LoadableModel<List<AssignmentEditorDto>>() {
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
                                return new MergeObjectsPanel(panelId, mergeObject, mergeWithObject, type, PageMergeObjects.this);
                            }
                        });
                return tabs;
            }
        };
    }
    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel(){
        return new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel());
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageUsers.class;
    }


//    protected void initLayout(){
//        if (mergeObject == null || StringUtils.isEmpty(mergeObject.getOid())
//                || mergeWithObject == null || StringUtils.isEmpty(mergeWithObject.getOid())) {
//            Label warningMessage = new Label(ID_MERGE_PANEL, createStringResource("PageMergeObjects.warningMessage"));
//            warningMessage.setOutputMarkupId(true);
//            add(warningMessage);
//        } else {
//            MergeObjectsPanel mergePanel = new MergeObjectsPanel(ID_MERGE_PANEL, mergeObject, mergeWithObject, type, PageMergeObjects.this);
//            mergePanel.setOutputMarkupId(true);
//            add(mergePanel);
//        }
//    }

    protected UserType createNewObject(){
        return new UserType();
    }

    @Override
    protected Class getCompileTimeClass() {
        return UserType.class;
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMergeObjects.title");
    }

    @Override
    public boolean isEditingFocus() {
        return true;
    }
}
