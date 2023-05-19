/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.user.component;

import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
@PanelType(name = "userDelegations")
@PanelInstance(identifier = "userDelegations", applicableForType = UserType.class,
        display = @PanelDisplay(label = "FocusType.delegations", order = 90))
@Counter(provider = UserDelegationsCounter.class)
public class UserDelegationsPanel extends AbstractObjectMainPanel<UserType, UserDetailsModel> {
    private static final long serialVersionUID = 1L;

    private static final String ID_DELEGATIONS_CONTAINER = "delegationsContainer";
    private static final String ID_DELEGATIONS_PANEL = "delegationsPanel";

    private static final Trace LOGGER = TraceManager.getTrace(UserDelegationsPanel.class);

    public UserDelegationsPanel(String id, UserDetailsModel userDetailsModel, ContainerPanelConfigurationType config) {
        super(id, userDetailsModel, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer delegations = new WebMarkupContainer(ID_DELEGATIONS_CONTAINER);
        delegations.setOutputMarkupId(true);
        add(delegations);

        AssignmentTablePanel panel = new AssignmentTablePanel<UserType>(ID_DELEGATIONS_PANEL, getObjectDetailsModels().getDelegationsModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item) {
                DelegationEditorPanel editor = new DelegationEditorPanel(ID_ROW, item.getModel(), false,
                        getObjectDetailsModels().getPrivilegesListModel());
                item.add(editor);
            }

            @Override
            public String getExcludeOid() {
                return getObjectWrapper().getOid();
            }

            @Override
            public IModel<String> getLabel() {
                return createStringResource("FocusType.delegations");
            }

            @Override
            protected List<InlineMenuItem> createAssignmentMenu() {
                List<InlineMenuItem> items = new ArrayList<>();

                InlineMenuItem item;
                if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DELEGATE_ACTION_URL)) {
                    item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.addDelegation")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public InlineMenuItemAction initAction() {
                            return new InlineMenuItemAction() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    List<QName> supportedTypes = new ArrayList<>();
                                    supportedTypes.add(UserType.COMPLEX_TYPE);
                                    ObjectFilter filter = getPrismContext().queryFactory().createInOid(getObjectWrapper().getOid());
                                    ObjectFilter notFilter = getPrismContext().queryFactory().createNot(filter);
                                    ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                                            getPageBase().getMainPopupBodyId(), UserType.class,
                                            supportedTypes, false, getPageBase(), notFilter) {
                                        private static final long serialVersionUID = 1L;

                                        @Override
                                        protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                                            getPageBase().hideMainPopup(target);
                                            List<ObjectType> newAssignmentsList = new ArrayList<>();
                                            newAssignmentsList.add(user);
                                            addSelectedAssignablePerformed(target, newAssignmentsList, null, getPageBase().getMainPopup().getId());
                                        }

                                    };
                                    panel.setOutputMarkupId(true);
                                    getPageBase().showMainPopup(panel, target);

                                }
                            };
                        }
                    };
                    items.add(item);
                }
                if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
                    item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.deleteDelegation")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public InlineMenuItemAction initAction() {
                            return new InlineMenuItemAction() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    deleteAssignmentPerformed(target, null);
                                }
                            };
                        }
                    };
                    items.add(item);
                }

                return items;
            }

            @Override
            protected String getNoAssignmentsSelectedMessage() {
                return getString("AssignmentTablePanel.message.noDelegationsSelected");
            }

            @Override
            protected String getAssignmentsDeleteMessage(AssignmentEditorDto dto) {
                return createStringResource("AssignmentTablePanel.modal.message.deleteDelegation",
                        getSelectedAssignments().size()).getString();
            }

            @Override
            protected List<AssignmentEditorDto> getAssignmentListToProcess(AssignmentEditorDto dto){
                if (dto != null){
                    return Collections.singletonList(dto);
                } else {
                    return getSelectedAssignments();
                }
            }

            @Override
            protected void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignments, QName relation,
                                                          String popupId) {
                ModalDialog window = (ModalDialog) get(popupId);
                if (window != null) {
                    window.close(target);
                }
                getPageBase().hideMainPopup(target);
                if (newAssignments.isEmpty()) {
                    warn(getString("AssignmentTablePanel.message.noAssignmentSelected"));
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                for (ObjectType object : newAssignments) {
                    try {
                        AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(
                                getObjectWrapper().getObject().asObjectable(),
                                WebComponentUtil.getDefaultRelationOrFail(RelationKindType.DELEGATION), getPageBase(), (UserType) object);
                        dto.setPrivilegeLimitationList(getPrivilegesListModel().getObject());
                        getDelegationModel().getObject().add(dto);
                    } catch (Exception e) {
                        error(getString("AssignmentTablePanel.message.couldntAssignObject", object.getName(),
                                e.getMessage()));
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign object", e);
                    }
                }
                reloadAssignmentsPanel(target);
            }
        };

        delegations.add(panel);
    }

    private AssignmentTablePanel getDelegationsTablePanel(){
        return (AssignmentTablePanel) get(ID_DELEGATIONS_CONTAINER).get(ID_DELEGATIONS_PANEL);
    }

    private LoadableModel<List<AssignmentEditorDto>> getDelegationModel() {
        return getObjectDetailsModels().getDelegationsModel();
    }

    private LoadableModel<List<AssignmentInfoDto>> getPrivilegesListModel() {
        return getObjectDetailsModels().getPrivilegesListModel();
    }

}
