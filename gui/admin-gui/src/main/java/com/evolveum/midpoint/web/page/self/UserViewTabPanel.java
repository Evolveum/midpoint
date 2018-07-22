/*
 * Copyright (c) 2016-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.web.component.assignment.GridViewComponent;
import com.evolveum.midpoint.web.component.assignment.UserSelectionButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.github.sommeri.less4j.utils.ArraysUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
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

    private static final String ID_SOURCE_USER_PANEL = "sourceUserPanel";
    private static final String ID_SOURCE_USER_BUTTON = "sourceUserButton";
    private static final String ID_SOURCE_USER_RELATIONS = "sourceUserRelations";
    private static final String ID_RELATION_LINK = "relationLink";
    private static final String ID_RELATION_LINK_LABEL = "relationLinkLabel";

    private QName selectedRelation = null;

    public UserViewTabPanel(String id, RoleManagementConfigurationType roleManagementConfig){
        super(id, roleManagementConfig);
    }

    @Override
    protected void initLeftSidePanel(){
        getRoleCatalogStorage().setAssignmentsUserOwner(getPageBase().loadUserSelf().asObjectable());

        WebMarkupContainer sourceUserPanel = new WebMarkupContainer(ID_SOURCE_USER_PANEL);
        sourceUserPanel.setOutputMarkupId(true);
        add(sourceUserPanel);

        initSourceUserSelectionPanel(sourceUserPanel);
        initRelationsPanel(sourceUserPanel);
    }

    private void initSourceUserSelectionPanel(WebMarkupContainer sourceUserPanel){

        UserSelectionButton sourceUserButton = new UserSelectionButton(ID_SOURCE_USER_BUTTON,
                new AbstractReadOnlyModel<List<UserType>>() {
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

//                searchModel.reset();
                target.add(UserViewTabPanel.this);
            }

            @Override
            protected String getUserButtonLabel(){
                return getSourceUserSelectionButtonLabel();
            }

            @Override
            protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target){
                super.onDeleteSelectedUsersPerformed(target);
                getRoleCatalogStorage().setAssignmentsUserOwner(null);
//                initProvider();
//                searchModel.reset();

//                target.add(getTabbedPanel());
//                target.add(getHeaderPanel());
            }
        };
//        sourceUserPanel.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            public boolean isVisible(){
//                return getRoleCatalogStorage().getShoppingCartConfigurationDto().isUserAssignmentsViewAllowed();
//            }
//        });
        sourceUserButton.setOutputMarkupId(true);
        sourceUserPanel.add(sourceUserButton);
    }

    private String getSourceUserSelectionButtonLabel(){
        UserType user = getRoleCatalogStorage().getAssignmentsUserOwner();
        if (user ==  null){
            return createStringResource("AssignmentCatalogPanel.selectSourceUser").getString();
        } else {
            return createStringResource("AssignmentCatalogPanel.sourceUserSelected", user.getName().getOrig()).getString();
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
        relationsList.add(null);
        for (RelationTypes relation : RelationTypes.values()){
            if (relation.getCategories() != null && ArraysUtils.asList(relation.getCategories()).contains(AreaCategoryType.SELF_SERVICE)){
                relationsList.add(relation.getRelation());
            }
        }
        return relationsList;
    }

    private Component createRelationLink(String id, IModel<QName> model) {
        AjaxLink<QName> button = new AjaxLink<QName>(id, model) {

            @Override
            public IModel<String> getBody() {
                QName relation = model.getObject();
                return relation == null ? createStringResource("RelationTypes.ANY") :
                        createStringResource("RelationTypes." + relation.getLocalPart());
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
    protected void appendItemsPanelStyle(GridViewComponent itemsPanel){
        itemsPanel.add(AttributeAppender.append("class", "col-md-9"));
    }

    @Override
    protected ObjectQuery createContentQuery(ObjectQuery searchQuery) {
        ObjectQuery query = super.createContentQuery(searchQuery);
        if (getRoleCatalogStorage().getAssignmentsUserOwner() != null) {
            UserType assignmentsOwner =  getRoleCatalogStorage().getAssignmentsUserOwner();
            List<String> assignmentTargetObjectOidsList = collectTargetObjectOids(assignmentsOwner.getAssignment());
            ObjectFilter oidsFilter = InOidFilter.createInOid(assignmentTargetObjectOidsList);
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
