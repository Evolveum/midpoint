/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by honchar.
 */
public abstract class UserSelectionButton extends BasePanel<List<UserType>> {

    private static final String ID_USER_SELECTION_BUTTON = "userSelectionButton";
    private static final String ID_DELETE_SELECTED_USER_BUTTON = "deleteSelectedUserButton";
    private static final String ID_USER_SELECTION_BUTTON_LABEL = "userSelectionButtonLabel";
    protected static final int TARGET_USERS_TITLE_ROWS = 30;

    private PageBase pageBase;
    private boolean showUserSelectionPopup = true;
    private boolean isMultiSelection;
    private StringResourceModel titleModel;

    public UserSelectionButton(String id, IModel<List<UserType>> selectedUsersListModel, boolean isMultiSelection,
            StringResourceModel titleModel) {
        super(id, selectedUsersListModel);
        this.isMultiSelection = isMultiSelection;
        this.titleModel = titleModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        pageBase = getPageBase();
        initLayout();
    }

    private void initLayout() {
        AjaxLink<String> userSelectionButton = new AjaxLink<String>(ID_USER_SELECTION_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (showUserSelectionPopup) {
                    initUserSelectionPopup(target);
                }
                showUserSelectionPopup = true;
            }
        };
        userSelectionButton.add(AttributeModifier.append("class", getTargetUserButtonClass()));
        userSelectionButton.setOutputMarkupId(true);
        userSelectionButton.add(new AttributeAppender("title", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getUserSelectionButtonTitle();
            }
        }));
        add(userSelectionButton);

        Label label = new Label(ID_USER_SELECTION_BUTTON_LABEL, new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getUserButtonLabel();
            }
        });
        label.setRenderBodyOnly(true);
        userSelectionButton.add(label);

        AjaxLink<Void> deleteButton = new AjaxLink<Void>(ID_DELETE_SELECTED_USER_BUTTON) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                UserSelectionButton.this.onDeleteSelectedUsersPerformed(target);
            }
        };
        deleteButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return isDeleteButtonVisible();
            }
        });
        userSelectionButton.add(deleteButton);
    }

    protected abstract String getUserButtonLabel();

    protected boolean isDeleteButtonVisible() {
        return getModelObject() != null && getModelObject().size() > 0;
    }

    protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target) {
        showUserSelectionPopup = false;
    }

    protected String getTargetUserButtonClass() {
        return "";
    }

    private void initUserSelectionPopup(AjaxRequestTarget target) {
        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(pageBase.getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class).getTypeName());
        ObjectBrowserPanel<UserType> focusBrowser = new ObjectBrowserPanel<UserType>(pageBase.getMainPopupBodyId(),
                UserType.class, supportedTypes, isMultiSelection, pageBase, getUserQueryFilter(), getModelObject()) {

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, UserType object) {
                super.onSelectPerformed(target, object);
                singleUserSelectionPerformed(target, object);
            }

            @Override
            protected void addPerformed(AjaxRequestTarget target, QName type, List<UserType> selected) {
                super.addPerformed(target, type, selected);
                multipleUsersSelectionPerformed(target, getAllSelectedObjectsList());
            }

            @Override
            public StringResourceModel getTitle() {
                return titleModel;
            }

        };
        pageBase.showMainPopup(focusBrowser, target);
    }

    protected void singleUserSelectionPerformed(AjaxRequestTarget target, UserType user) {
    }

    protected void multipleUsersSelectionPerformed(AjaxRequestTarget target, List<UserType> usersList) {
    }

    protected String getUserSelectionButtonTitle() {
        if (getModelObject().size() > 1) {
            StringBuilder sb = new StringBuilder();
            Collections.sort(getModelObject(), new Comparator<UserType>() {

                @Override
                public int compare(UserType u1, UserType u2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(u1.getName().getOrig(), u2.getName().getOrig());
                }
            });
            int columnsAmount = getModelObject().size() / TARGET_USERS_TITLE_ROWS;
            Iterator<UserType> it = getModelObject().iterator();
            while (it.hasNext()) {
                for (int i = 0; i <= columnsAmount; i++) {
                    if (it.hasNext()) {
                        UserType user = it.next();
                        sb.append(user.getName().getOrig());
                        if (it.hasNext()) {
                            sb.append(",\t");
                        }
                    }
                }
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }
        return titleModel.getString();
    }

    protected ObjectFilter getUserQueryFilter() {
        return null;
    }
}
