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

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.ExportToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.IResourceStream;

/**
 * @author katkav
 */
public abstract class MainObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_REFRESH = "refresh";
    private static final String ID_NEW_OBJECT = "newObject";
    private static final String ID_IMPORT_OBJECT = "importObject";
    private static final String ID_BUTTON_BAR = "buttonBar";

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
        return new ButtonBar(id, ID_BUTTON_BAR, this);
    }

    private static class ButtonBar extends Fragment {

    	private static final long serialVersionUID = 1L;

		public <O extends ObjectType> ButtonBar(String id, String markupId, MainObjectListPanel<O> markupProvider) {
            super(id, markupId, markupProvider);

            initLayout(markupProvider);
        }

        private <O extends ObjectType> void initLayout(final MainObjectListPanel<O> mainObjectListPanel) {
            AjaxIconButton refreshIcon = new AjaxIconButton(ID_REFRESH, new Model<>("fa fa-refresh"),
                    mainObjectListPanel.createStringResource("MainObjectListPanel.refresh")) {

             			private static final long serialVersionUID = 1L;

				@Override
                public void onClick(AjaxRequestTarget target) {
                	mainObjectListPanel.clearCache();
                	mainObjectListPanel.refreshTable((Class<O>) mainObjectListPanel.getType(), target);
                    
                    target.add((Component) mainObjectListPanel.getTable());
                }
            };
            add(refreshIcon);

            AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_OBJECT, new Model<>("fa fa-plus"),
                    mainObjectListPanel.createStringResource("MainObjectListPanel.newObject")) {

            	private static final long serialVersionUID = 1L;
            	
                @Override
                public void onClick(AjaxRequestTarget target) {
                    mainObjectListPanel.newObjectPerformed(target);
                }
            };
            add(newObjectIcon);

            AjaxIconButton importObject = new AjaxIconButton(ID_IMPORT_OBJECT, new Model<>("fa fa-upload"),
                    mainObjectListPanel.createStringResource("MainObjectListPanel.import")) {

            	private static final long serialVersionUID = 1L;
            	
                @Override
                public void onClick(AjaxRequestTarget target) {
                    ((PageBase) getPage()).navigateToNext(PageImportObject.class);
                }
            };
            add(importObject);

        }
    }

    @Override
    protected boolean getExportToolbarVisibility(){
        return true;
    }
    }
