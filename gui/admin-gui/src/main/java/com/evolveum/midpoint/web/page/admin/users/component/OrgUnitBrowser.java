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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class OrgUnitBrowser extends ModalWindow {

    private static final Trace LOGGER = TraceManager.getTrace(OrgUnitBrowser.class);

    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH = "search";
    private static final String ID_TABLE = "table";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CANCEL = "cancel";

    private boolean initialized;

    private IModel<String> searchModel = new Model<String>();

    public OrgUnitBrowser(String id) {
        super(id);

        setTitle(createStringResource("OrgUnitBrowser.title"));
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(OrgUnitBrowser.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(900);
        setInitialHeight(530);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (initialized) {
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    private void initLayout(WebMarkupContainer container) {
        Form form = new Form(ID_MAIN_FORM);
        container.add(form);

        TextField searchText = new TextField(ID_SEARCH_TEXT, searchModel);
        form.add(searchText);

        AjaxSubmitButton search = new AjaxSubmitButton(ID_SEARCH,
                createStringResource("OrgUnitBrowser.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        form.add(search);

        ObjectDataProvider<OrgTableDto, OrgType> provider = new ObjectDataProvider<OrgTableDto, OrgType>(this, OrgType.class) {

            @Override
            public OrgTableDto createDataObjectWrapper(PrismObject<OrgType> obj) {
                return OrgTableDto.createDto(obj);
            }
        };
        List<IColumn<OrgTableDto, String>> columns = initColumns();
        TablePanel table = new TablePanel(ID_TABLE, provider, columns);
        container.add(table);

        AjaxButton cancel = new AjaxButton(ID_CANCEL,
                createStringResource("OrgUnitBrowser.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        container.add(cancel);
    }

    private List<IColumn<OrgTableDto, String>> initColumns() {
        List<IColumn<OrgTableDto, String>> columns = new ArrayList<IColumn<OrgTableDto, String>>();

        columns.add(new LinkColumn<OrgTableDto>(createStringResource("ObjectType.name"), "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<OrgTableDto> rowModel) {
                rowSelected(target, rowModel);
            }
        });
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.displayName"),
                OrgTableDto.F_DISPLAY_NAME));
        columns.add(new PropertyColumn<OrgTableDto, String>(createStringResource("OrgType.identifier"),
                OrgTableDto.F_IDENTIFIER));

        return columns;
    }

    protected void rowSelected(AjaxRequestTarget target, IModel<OrgTableDto> row) {

    }

    private void searchPerformed(AjaxRequestTarget target) {

    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }
}
