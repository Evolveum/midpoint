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

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/userHistory", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_HISTORY_URL,
                label = "PageUser.auth.userHistory.label",
                description = "PageUser.auth.userHistory.description")})
public class PageUserHistory extends PageAdminFocus<UserType> {

    private static final String DOT_CLASS = PageUserHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageUserHistory.class);
    private String date = "";

    public PageUserHistory(final PrismObject<UserType> user, String date) {
        this.date = date;
        initialize(user);
    }

    @Override
    protected ObjectWrapper<UserType> loadObjectWrapper(PrismObject<UserType> user, boolean isReadonly) {
        ObjectWrapper<UserType> objectWrapper = super.loadObjectWrapper(user, true);

        for (ContainerWrapper container : objectWrapper.getContainers()) {
            setContainerValuesReadOnlyState(container);
        }

        return objectWrapper;
    }

    private void setContainerValuesReadOnlyState(ContainerWrapper container) {
        container.setReadonly(true);

        container.getValues().forEach(v -> {
            ((ContainerValueWrapper) v).getItems().forEach(item -> {
                if (item instanceof ContainerWrapper) {
                    setContainerValuesReadOnlyState((ContainerWrapper) item);
                } else if (item instanceof PropertyOrReferenceWrapper) {
                    ((PropertyOrReferenceWrapper) item).setReadonly(true);
                }
            });
            ((ContainerValueWrapper) v).setReadonly(true);

        });
    }

    @Override
    protected void setSummaryPanelVisibility(FocusSummaryPanel summaryPanel) {
        summaryPanel.setVisible(true);
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
        return new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel(), this);
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }


    @Override
    protected IModel<String> createPageTitleModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                String name = null;
                if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
                    name = WebComponentUtil.getName(getObjectWrapper().getObject());
                }
                return createStringResource("PageUserHistory.title", name, date).getObject();
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        };
    }

    @Override
    protected UserType createNewObject() {
        return new UserType();
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageUsers.class;
    }

    @Override
    public Class getCompileTimeClass() {
        return UserType.class;
    }

    @Override
    protected AbstractObjectMainPanel<UserType> createMainPanel(String id) {
        return new FocusMainPanel<UserType>(id, getObjectModel(), getProjectionModel(), this) {
            @Override
            protected List<ITab> createTabs(final PageAdminObjectDetails<UserType> parentPage) {
                List<ITab> tabs = new ArrayList<>();
                FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_BASIC_URL);

                tabs.add(
                        new PanelTab(parentPage.createStringResource("pageAdminFocus.basic"), authorization) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return createFocusDetailsTabPanel(panelId, parentPage);
                            }
                        });

                //hidden, will be displayed in future version
//                authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_PROJECTIONS_URL);
//                tabs.add(
//                        new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.projections"), authorization) {
//
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            public WebMarkupContainer createPanel(String panelId) {
//                                return createFocusProjectionsTabPanel(panelId, parentPage);
//                            }
//
//                            @Override
//                            public String getCount() {
//                                return Integer.toString(getProjectionModel().getObject() == null ?
//                                        0 : getProjectionModel().getObject().size());
//                            }
//                        });

                authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_ASSIGNMENTS_URL);
                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.assignments"), authorization) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return createFocusAssignmentsTabPanel(panelId, parentPage);
                            }

                            @Override
                            public String getCount() {
                                return Integer.toString(countAssignments());
                            }
                        });
                authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_DELEGATED_TO_ME_URL);
                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegatedToMe"), authorization)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentTablePanel<UserType>(panelId, 
                                getDelegatedToMeModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item) {
                                DelegationEditorPanel editor = new DelegationEditorPanel(ID_ROW, item.getModel(), true,
                                        new ArrayList<>(), PageUserHistory.this);
                                item.add(editor);
                            }

                            @Override
                            public String getExcludeOid() {
                                return getObject().getOid();
                            }
                            
                            @Override
                            		public IModel<String> getLabel() {
                            			return parentPage.createStringResource("FocusType.delegatedToMe");
                            		}

                            @Override
                            protected List<InlineMenuItem> createAssignmentMenu() {
                                return new ArrayList<>();
                            }

                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(getDelegatedToMeModel().getObject() == null ?
                                0 : getDelegatedToMeModel().getObject().size());
                    }
                });

                return tabs;

            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                return false;
            }
        };
    }
}
