/**
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InternalsLoggedInUsersPanel<F extends FocusType> extends BasePanel<F> {

    private static final String ID_TABLE = "table";

    public InternalsLoggedInUsersPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        MainObjectListPanel<F, String> table = new MainObjectListPanel(ID_TABLE, FocusType.class, null, null, getPageBase()) {

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {

            }

            @Override
            protected List<IColumn<SelectableBean<F>, String>> createColumns() {
                return InternalsLoggedInUsersPanel.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
               return initInlineMenu();
            }

            @Override
            protected BaseSortableDataProvider<SelectableBean<F>> initProvider() {
//                List<MidPointUserProfilePrincipal> principals = getPageBase().getModelInteractionService().getLoggedInUsers();
//                List<SelectableBean<F>> users = new ArrayList<>(principals.size());
//                for (MidPointUserProfilePrincipal principal : principals) {
//                    SelectableBean<F> user = new SelectableBean<F>((F) principal.getUser());
//                    user.setActiveSessions(principal.getActiveSessions());
//                    users.add(user);
//                }

                LoadableModel<List<MidPointUserProfilePrincipal>> principals = new LoadableModel<List<MidPointUserProfilePrincipal>>(true) {

                    @Override
                    protected List<MidPointUserProfilePrincipal> load() {
                        return getPageBase().getModelInteractionService().getLoggedInUsers();
                    }
                };

                return new ListDataProvider2<SelectableBean<F>, MidPointUserProfilePrincipal>(InternalsLoggedInUsersPanel.this, principals) {

                    @Override
                    protected SelectableBean<F> createObjectWrapper(MidPointUserProfilePrincipal principal) {
                        SelectableBean<F> user = new SelectableBean<>((F) principal.getUser());
                        user.setActiveSessions(principal.getActiveSessions());
                        return user;
                    }
                };
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return new ArrayList<>();
            }
        };

        add(table);
    }

    private List<IColumn<SelectableBean<F>, String>> initColumns() {
        List<IColumn<SelectableBean<F>, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("ActivationType.effectiveStatus"), "value.activation.effectiveStatus"));
        columns.add(new PropertyColumn<>(createStringResource("InternalsLoggedInUsers.activeSessions"), "activeSessions"));
        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new ButtonInlineMenuItem(createStringResource("InternalsLoggedInUsers.refresh")) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_ICON_SIGN_OUT;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<F>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            expireSessionsFor(target, null);
                        } else {
                            SelectableBean<F> rowDto = getRowModel().getObject();
                            expireSessionsFor(target, rowDto.getValue());
                        }
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return true;
            }
        });
        return menuItems;

    }

    private void expireSessionsFor(AjaxRequestTarget target, F selectedObject) {
        List<String> selected = getSelectedObjects(selectedObject);
        if (CollectionUtils.isEmpty(selected)) {
            getSession().warn("InternalsLoggedInUsersPanel.expireSession.warn");
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        getPageBase().getModelInteractionService().expirePrincipals(selected);
        getSession().success("InternalsLoggedInUsersPanel.expireSession.success");
        target.add(getPageBase().getFeedbackPanel());
    }

    private List<String> getSelectedObjects(F selectedObject) {
        MainObjectListPanel<F, String> table = getTable();

        if (selectedObject != null) {
            return Arrays.asList(selectedObject.getOid());
        }

        List<F> selectedObjects = table.getSelectedObjects();
        if (selectedObject != null || !selectedObjects.isEmpty()) {
            return selectedObjects.stream().map(o -> o.getOid()).collect(Collectors.toList());
        }

        return null;
    }

    private MainObjectListPanel<F, String> getTable() {
        return (MainObjectListPanel) get(ID_TABLE);
    }
}
