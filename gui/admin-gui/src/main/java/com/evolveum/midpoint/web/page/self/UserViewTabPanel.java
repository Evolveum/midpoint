/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.UserSelectionButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class UserViewTabPanel extends AbstractShoppingCartTabPanel<AbstractRoleType> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = UserViewTabPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";
    private static final Trace LOGGER = TraceManager.getTrace(UserViewTabPanel.class);

    private static final String ID_SOURCE_USER_PANEL = "sourceUserPanel";
    private static final String ID_SOURCE_USER_BUTTON = "sourceUserButton";
    private static final String ID_SOURCE_USER_RELATIONS = "sourceUserRelations";
    private static final String ID_RELATION_LINK = "relationLink";

    private QName selectedRelation = null;

    public UserViewTabPanel(String id, RoleManagementConfigurationType roleManagementConfig){
        super(id, roleManagementConfig);
    }

    @Override
    protected void initLeftSidePanel(){
        if (getRoleCatalogStorage().getAssignmentsUserOwner() == null) {
            getRoleCatalogStorage().setAssignmentsUserOwner(getPageBase().getPrincipalUser());
        }

        WebMarkupContainer sourceUserPanel = new WebMarkupContainer(ID_SOURCE_USER_PANEL);
        sourceUserPanel.setOutputMarkupId(true);
        add(sourceUserPanel);

        initSourceUserSelectionPanel(sourceUserPanel);
        initRelationsPanel(sourceUserPanel);
    }

    private void initSourceUserSelectionPanel(WebMarkupContainer sourceUserPanel){

        UserSelectionButton sourceUserButton = new UserSelectionButton(ID_SOURCE_USER_BUTTON,
                new IModel<List<UserType>>() {
                    @Override
                    public List<UserType> getObject() {
                        List<UserType> usersList = new ArrayList<>();
                        if (getRoleCatalogStorage().getAssignmentsUserOwner() != null){
                            usersList.add(getRoleCatalogStorage().getAssignmentsUserOwner());
                        }
                        return usersList;
                    }
                }, false, createStringResource("AssignmentCatalogPanel.selectSourceUser")){
            private static final long serialVersionUID = 1L;

            @Override
            protected void singleUserSelectionPerformed(AjaxRequestTarget target, UserType user){
                super.singleUserSelectionPerformed(target, user);
                getRoleCatalogStorage().setAssignmentsUserOwner(user);
                target.add(UserViewTabPanel.this);
            }

            @Override
            protected String getUserButtonLabel(){
                return getSourceUserSelectionButtonLabel();
            }

            @Override
            protected boolean isDeleteButtonVisible(){
                return false;
            }
        };
        sourceUserButton.setOutputMarkupId(true);
        sourceUserPanel.add(sourceUserButton);
    }

    private String getSourceUserSelectionButtonLabel(){
        UserType user = getRoleCatalogStorage().getAssignmentsUserOwner();
        if (user.getOid().equals(getPageBase().getPrincipalUser().getOid())){
            return createStringResource("UserSelectionButton.myAssignmentsLabel").getString();
        } else {
            return createStringResource("UserSelectionButton.userAssignmentsLabel", user.getName().getOrig()).getString();
        }
    }

    private void initRelationsPanel(WebMarkupContainer sourceUserPanel){
        ListView relationsPanel = new ListView<QName>(ID_SOURCE_USER_RELATIONS, new LoadableModel<List<QName>>(false) {
            @Override
            protected List<QName> load() {
                return getRelationsList();
            }
        }){
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<QName> item) {
                item.add(createRelationLink(ID_RELATION_LINK, item.getModel()));
            }
        };
        relationsPanel.setOutputMarkupId(true);

        sourceUserPanel.add(relationsPanel);
    }

    private List<QName> getRelationsList(){
        List<QName> relationsList = new ArrayList<>();
        //null value is needed for ALL relations to be displayed
        relationsList.add(null);

        relationsList.addAll(WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, getPageBase()));
        return relationsList;
    }

    private Component createRelationLink(String id, IModel<QName> model) {
        AjaxLink<QName> button = new AjaxLink<QName>(id, model) {

            @Override
            public IModel<String> getBody() {
                QName relation = model.getObject();
                if (relation == null){
                    return createStringResource("RelationTypes.ANY");
                }
                RelationDefinitionType def = WebComponentUtil.getRelationDefinition(model.getObject());
                if (def != null) {
                    DisplayType display = def.getDisplay();
                    if (display != null) {
                        PolyStringType label = display.getLabel();
                        if (PolyStringUtils.isNotEmpty(label)){
                            PolyStringTranslationType translation = label.getTranslation();
                            if (translation != null && translation.getKey() != null) {
                                return getPageBase().createStringResource(translation.getKey());
                            }
                            return getPageBase().createStringResource(label);
                        }
                    }
                }
                return Model.of(model.getObject().getLocalPart());
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedRelation = model.getObject();
                target.add(UserViewTabPanel.this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                QName relation = model.getObject();
                if (relation == null && selectedRelation == null
                        || relation != null && relation.equals(selectedRelation)) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }

    @Override
    protected void appendItemsPanelStyle(WebMarkupContainer container){
        container.add(AttributeAppender.append("class", "col-md-9"));
    }

    @Override
    protected ObjectQuery createContentQuery() {
        ObjectQuery query = super.createContentQuery();
        if (getRoleCatalogStorage().getAssignmentsUserOwner() != null) {
            UserType assignmentsOwner =  getRoleCatalogStorage().getAssignmentsUserOwner();
            List<String> assignmentTargetObjectOidsList = collectTargetObjectOids(assignmentsOwner.getAssignment());
            ObjectFilter oidsFilter = getPrismContext().queryFactory().createInOid(assignmentTargetObjectOidsList);
            query.addFilter(oidsFilter);
        }
        return query;
    }

    private List<String> collectTargetObjectOids(List<AssignmentType> assignments){
        List<String> oidsList = new ArrayList<>();
        if (assignments == null){
            return oidsList;
        }
        QName relation = getSelectedRelation();
        assignments.forEach(assignment -> {
            if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
                return;
            }
            if (relation != null && !relation.equals(assignment.getTargetRef().getRelation())){
                return;
            }
            if (ArchetypeType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())){
                return;
            }
            oidsList.add(assignment.getTargetRef().getOid());
        });
        return oidsList;
    }

    private QName getSelectedRelation(){
        return selectedRelation;
    }

    @Override
    protected QName getQueryType(){
        return AbstractRoleType.COMPLEX_TYPE;
    }

}
