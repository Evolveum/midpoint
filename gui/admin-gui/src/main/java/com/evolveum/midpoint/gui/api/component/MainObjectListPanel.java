/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katkav
 */
public abstract class MainObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_REFRESH = "refresh";
    private static final String ID_NEW_OBJECT = "newObject";
    private static final String ID_IMPORT_OBJECT = "importObject";
    private static final String ID_EXPORT_DATA = "exportData";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON_REPEATER = "buttonsRepeater";
    private static final String ID_BUTTON = "button";
    private static final Trace LOGGER = TraceManager.getTrace(MainObjectListPanel.class);

    public MainObjectListPanel(String id, Class<O> type, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options, PageBase parentPage) {
        super(id, type, tableId, options, parentPage);
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath) {
        if (StringUtils.isEmpty(itemPath)) {
            return new ObjectNameColumn<O>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    MainObjectListPanel.this.objectDetailsPerformed(target, object);
                }

                @Override
                public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                    return MainObjectListPanel.this.isClickable(rowModel);
                }
            };
        } else {
            return new ObjectNameColumn<O>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                    itemPath) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    MainObjectListPanel.this.objectDetailsPerformed(target, object);
                }

                @Override
                public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                    return MainObjectListPanel.this.isClickable(rowModel);
                }
            };
        }
    }

    protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected abstract void objectDetailsPerformed(AjaxRequestTarget target, O object);

    protected abstract void newObjectPerformed(AjaxRequestTarget target);

    @Override
    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new ButtonBar(id, ID_BUTTON_BAR, this, createToolbarButtonsList(ID_BUTTON));
    }

    protected List<Component> createToolbarButtonsList(String buttonId){
        List<Component> buttonsList = new ArrayList<>();
        // TODO if displaying shadows in the repository (and not from resource) we can afford to count the objects
        boolean canCountBeforeExporting = getType() == null || !ShadowType.class.isAssignableFrom(getType());

        AjaxIconButton newObjectIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_ADD_NEW_OBJECT),
                createStringResource("MainObjectListPanel.newObject")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                newObjectPerformed(target);
            }
        };
        newObjectIcon.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
        buttonsList.add(newObjectIcon);

        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearCache();
                refreshTable((Class<O>) getType(), target);

                target.add((Component) getTable());
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        buttonsList.add(refreshIcon);

        AjaxIconButton importObject = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_UPLOAD),
                createStringResource("MainObjectListPanel.import")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).navigateToNext(PageImportObject.class);
            }
        };
        importObject.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        importObject.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){

                boolean isVisible = false;
                try {
                    isVisible = ((PageBase) getPage()).isAuthorized(ModelAuthorizationAction.IMPORT_OBJECTS.getUrl())
                            && WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL,
                            AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL);
                } catch (Exception ex){
                    LOGGER.error("Failed to check authorization for IMPORT action for " + getType().getSimpleName()
                            + " object, ", ex);
                }
                return isVisible;
            }
        });
        buttonsList.add(importObject);

        CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(buttonId, canCountBeforeExporting) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DataTable<?, ?> getDataTable() {
                return getTable().getDataTable();
            }

            @Override
            protected String getFilename() {
                return getType().getSimpleName() +
                        "_" + createStringResource("MainObjectListPanel.exportFileName").getString();
            }

        };
        exportDataLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI);
            }
        });

        buttonsList.add(exportDataLink);
        return buttonsList;
    }

    private static class ButtonBar extends Fragment {

        private static final long serialVersionUID = 1L;

        public <O extends ObjectType> ButtonBar(String id, String markupId, MainObjectListPanel<O> markupProvider, List<Component> buttonsList) {
            super(id, markupId, markupProvider);

            initLayout(buttonsList);
        }

        private <O extends ObjectType> void initLayout(final List<Component> buttonsList) {
            ListView<Component> buttonsView = new ListView<Component>(ID_BUTTON_REPEATER, Model.ofList(buttonsList)) {
                @Override
                protected void populateItem(ListItem<Component> listItem) {
                    listItem.add(listItem.getModelObject());
                }
            };
            add(buttonsView);
        }
    }
}
