/*
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by honchar.
 */
public class TargetUserSelectorComponent extends BasePanel {

    private static final String ID_TARGET_USER_BUTTON = "targetUserButton";
    private static final String ID_DELETE_TARGET_USER_BUTTON = "deleteTargetUserButton";
    private static final String ID_TARGET_USER_LABEL = "targetUserLabel";
    private static final int TARGET_USERS_TITLE_ROWS = 30;

    private PageBase pageBase;
    private boolean showUserSelectionPopup = true;
    private String additionalButtonStyle;

    public TargetUserSelectorComponent(String id, PageBase pageBase){
        this(id, "", pageBase);
    }

    public TargetUserSelectorComponent(String id, String additionalButtonStyle, PageBase pageBase){
        super(id);
        this.additionalButtonStyle = additionalButtonStyle;
        this.pageBase = pageBase;
        initLayout();
    }

    private void initLayout(){
        AjaxLink<String> targetUserButton = new AjaxLink<String>(ID_TARGET_USER_BUTTON) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (showUserSelectionPopup) {
                    initUserSelectionPopup(createStringResource("AssignmentCatalogPanel.selectTargetUser"), true, target);
                }
                showUserSelectionPopup = true;
            }
        };
        targetUserButton.add(new AttributeAppender("class", "btn btn-default"
                + (StringUtils.isEmpty(additionalButtonStyle) ? "" : " " + additionalButtonStyle)));

        targetUserButton.setOutputMarkupId(true);
        targetUserButton.add(new AttributeAppender("title", getTargetUsersButtonTitle()));
        add(targetUserButton);

        Label label = new Label(ID_TARGET_USER_LABEL, createStringResource("AssignmentCatalogPanel.selectTargetUser"));
        label.setRenderBodyOnly(true);
        targetUserButton.add(label);

        AjaxLink deleteButton = new AjaxLink(ID_DELETE_TARGET_USER_BUTTON) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                showUserSelectionPopup = false;
                getRoleCatalogStorage().setTargetUserList(null);
                target.add(pageBase);
            }
        };
        deleteButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                return  getRoleCatalogStorage().getTargetUserList() != null &&
                        getRoleCatalogStorage().getTargetUserList().size() > 0;
            }
        });
        targetUserButton.add(deleteButton);
    }

    private void initUserSelectionPopup(StringResourceModel title, boolean multiselect, AjaxRequestTarget target) {

        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(pageBase.getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class).getTypeName());
        ObjectBrowserPanel<UserType> focusBrowser = new ObjectBrowserPanel<UserType>(pageBase.getMainPopupBodyId(),
                UserType.class, supportedTypes, multiselect, pageBase, null, getSelectedObjects()) {
            @Override
            protected void addPerformed(AjaxRequestTarget target, QName type, List<UserType> selected) {
                super.addPerformed(target, type, selected);
                List<PrismObject<UserType>> userList = new ArrayList<>();
                for (UserType user : selected){
                    userList.add(user.asPrismObject());
                }
                getRoleCatalogStorage().setTargetUserList(userList);
                target.add(pageBase);
            }

            @Override
            public StringResourceModel getTitle() {
                return title;
            }

        };
        pageBase.showMainPopup(focusBrowser, target);
    }

    private List<UserType> getSelectedObjects(){
        List<PrismObject<UserType>> selectedUsers = getRoleCatalogStorage().getTargetUserList();
        List<UserType> users = new ArrayList<>();
        if (selectedUsers != null) {
            for (PrismObject<UserType> user : selectedUsers) {
                users.add(user.asObjectable());
            }
        }
        return users;
    }

    private IModel<String> getTargetUsersButtonTitle() {
        return new LoadableModel<String>(true) {
            public String load() {
                if (getRoleCatalogStorage().isSelfRequest()){
                    return createStringResource("AssignmentCatalogPanel.requestForMe").getString();
                }
                List<PrismObject<UserType>> targetUsersList =  getRoleCatalogStorage().getTargetUserList();
                if (targetUsersList.size() == 1){
                    return createStringResource("AssignmentCatalogPanel.requestFor").getString() +
                            " " + targetUsersList.get(0).getName().getOrig();
                }

                StringBuilder sb = new StringBuilder( createStringResource("AssignmentCatalogPanel.requestForMultiple", targetUsersList.size()).getString());
                sb.append(System.lineSeparator());
                if (getRoleCatalogStorage().isMultiUserRequest()) {
                    List<PrismObject<UserType>> sortedList = getRoleCatalogStorage().getTargetUserList();
                    Collections.sort(sortedList, new Comparator<PrismObject<UserType>>() {

                        @Override
                        public int compare(PrismObject<UserType> u1, PrismObject<UserType> u2) {
                            return String.CASE_INSENSITIVE_ORDER.compare(u1.getName().getOrig(), u2.getName().getOrig());
                        }
                    });
                    int columnsAmount = sortedList.size() / TARGET_USERS_TITLE_ROWS;
                    Iterator<PrismObject<UserType>> it = sortedList.iterator();
                    while (it.hasNext()){
                        for (int i=0; i <= columnsAmount; i++){
                            if (it.hasNext()){
                                PrismObject user = it.next();
                                sb.append(user.getName().getOrig());
                                if (it.hasNext()){
                                    sb.append(",\t");
                                }
                            }
                        }
                        sb.append(System.lineSeparator());
                    }
                }
                return sb.toString();
            }
        };
    }

    private RoleCatalogStorage getRoleCatalogStorage(){
        return pageBase.getSessionStorage().getRoleCatalog();
    }
}
