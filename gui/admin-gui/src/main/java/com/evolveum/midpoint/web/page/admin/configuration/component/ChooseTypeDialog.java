/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class ChooseTypeDialog<T extends Serializable> extends ModalWindow{

    private static final String DEFAULT_SORTABLE_PROPERTY = null;

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private static final Trace LOGGER = TraceManager.getTrace(ChooseTypeDialog.class);
    Class<T> objectType;
    private boolean initialized;

    public ChooseTypeDialog(String id, Class<T> type){
        super(id);

        objectType = type;

        setTitle(createStringResource("chooseTypeDialog.title"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(ChooseTypeDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(500);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public void initLayout(WebMarkupContainer content){
        List<IColumn<SelectableBean<ObjectType>, String>> columns = initColumns();
        ObjectDataProvider provider = new ObjectDataProvider(getPageBase(), this.objectType);
        provider.setQuery(getDataProviderQuery());
        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        content.add(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("chooseTypeDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        content.add(cancelButton);
    }

    private List<IColumn<SelectableBean<ObjectType>, String>> initColumns(){
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn column = new LinkColumn<SelectableBean<ObjectType>>(createStringResource("chooseTypeDialog.column.name"), getSortableProperty(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ObjectType>> rowModel){
                ObjectType object = rowModel.getObject().getValue();
                chooseOperationPerformed(target, object);
            }

        };
        columns.add(column);

        return columns;
    }

    public void updateTablePerformed(AjaxRequestTarget target, ObjectQuery query){
        TablePanel table = (TablePanel) get(StringUtils.join(new String[]{CONTENT_ID, ID_TABLE}, ":"));
        DataTable dataTable = table.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)dataTable.getDataProvider();
        provider.setQuery(query);

        target.add(get(CONTENT_ID));
        target.add(table);
    }

    protected ObjectQuery getDataProviderQuery(){
        return null;
    }

    public String getSortableProperty(){
        return DEFAULT_SORTABLE_PROPERTY;
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){}

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private PageBase getPageBase() {
         return (PageBase) getPage();
    }
}
