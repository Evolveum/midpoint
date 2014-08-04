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

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class UserBrowserDialog extends ModalWindow {

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CHECK_NAME = "nameCheck";
    private static final String ID_CHECK_FULL_NAME = "fullNameCheck";
    private static final String ID_CHECK_GIVEN_NAME = "givenNameCheck";
    private static final String ID_CHECK_FAMILY_NAME = "familyNameCheck";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_TABLE = "table";

    private static final Trace LOGGER = TraceManager.getTrace(UserBrowserDialog.class);
    private IModel<UserBrowserDto> model;
    private boolean initialized;

    public UserBrowserDialog(String id) {
        super(id);

        setTitle(createStringResource("userBrowserDialog.title"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(UserBrowserDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(900);
        setInitialHeight(500);
        setWidthUnit("px");

        model = new LoadableModel<UserBrowserDto>(false) {

            @Override
            protected UserBrowserDto load() {
                return new UserBrowserDto();
            }
        };

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
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

    private void initLayout(WebMarkupContainer content) {
        Form mainForm = new Form(ID_MAIN_FORM);
        content.add(mainForm);

        Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        content.add(searchForm);

//        TextField<String> search = new TextField<String>("searchText", new PropertyModel<String>(model, "searchText"));
//        mainForm.add(search);

        CheckBox nameCheck = new CheckBox(ID_CHECK_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_NAME));
        searchForm.add(nameCheck);
        CheckBox fullNameCheck = new CheckBox(ID_CHECK_FULL_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_FULL_NAME));
        searchForm.add(fullNameCheck);
        CheckBox givenNameCheck = new CheckBox(ID_CHECK_GIVEN_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_GIVEN_NAME));
        searchForm.add(givenNameCheck);
        CheckBox familyNameCheck = new CheckBox(ID_CHECK_FAMILY_NAME, new PropertyModel<Boolean>(model, UserBrowserDto.F_FAMILY_NAME));
        searchForm.add(familyNameCheck);

        BasicSearchPanel<UserBrowserDto> basicSearch = new BasicSearchPanel<UserBrowserDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(model, UserBrowserDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                UserBrowserDialog.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                UserBrowserDialog.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);

        List<IColumn<SelectableBean<UserType>, String>> columns = initColumns();
        TablePanel table = new TablePanel<>(ID_TABLE,
                new ObjectDataProvider(getPageBase(), UserType.class), columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancelButton);
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private List<IColumn<SelectableBean<UserType>, String>> initColumns() {
        List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<IColumn<SelectableBean<UserType>, String>>();

        columns.add(new IconColumn<SelectableBean<UserType>>(createStringResource("userBrowserDialog.type")) {

            @Override
            protected IModel<String> createIconModel(final IModel<SelectableBean<UserType>> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        UserType user = rowModel.getObject().getValue();
                        return WebMiscUtil.createUserIcon(user.asPrismContainer());
                    }
                };
            }
        });

        IColumn column = new LinkColumn<SelectableBean<UserType>>(createStringResource("userBrowserDialog.name"), UserBrowserDto.F_NAME, "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user);
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.givenName"), UserBrowserDto.F_GIVEN_NAME, SelectableBean.F_VALUE + ".givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.familyName"), UserBrowserDto.F_FAMILY_NAME, SelectableBean.F_VALUE + ".familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("userBrowserDialog.fullName"), UserBrowserDto.F_FULL_NAME, SelectableBean.F_VALUE + ".fullName.orig");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<UserType>, String>(createStringResource("userBrowserDialog.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem, String componentId,
                                     IModel<SelectableBean<UserType>> rowModel) {

                String email = rowModel.getObject().getValue().getEmailAddress();
                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        return columns;
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();
        target.add(getPageBase().getFeedbackPanel());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        table.setCurrentPage(0);

        target.add(panel);
    }

    private TablePanel getTable() {
        return (TablePanel) getContent().get("mainForm:table");
    }

    private ObjectQuery createQuery() {
        UserBrowserDto dto = model.getObject();
        ObjectQuery query = null;
        if (StringUtils.isEmpty(dto.getSearchText())) {
            return null;
        }

        try {
            List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

            PrismContext prismContext = getPageBase().getPrismContext();
            PolyStringNormalizer normalizer = prismContext.getDefaultPolyStringNormalizer();
            if (normalizer == null) {
                normalizer = new PrismDefaultPolyStringNormalizer();
            }

            String normalizedString = normalizer.normalize(dto.getSearchText());

			if (dto.isName()) {
				filters.add(SubstringFilter.createSubstring(UserType.F_NAME, UserType.class, prismContext, 
						normalizedString));
			}

			if (dto.isFamilyName()) {
				filters.add(SubstringFilter.createSubstring(UserType.F_FAMILY_NAME, UserType.class, prismContext,
						normalizedString));
			}
			if (dto.isFullName()) {
				filters.add(SubstringFilter.createSubstring(UserType.F_FULL_NAME, UserType.class, prismContext,
						normalizedString));
			}
			if (dto.isGivenName()) {
				filters.add(SubstringFilter.createSubstring(UserType.F_GIVEN_NAME, UserType.class, prismContext,
						normalizedString));
			}

            if (!filters.isEmpty()) {
                query = new ObjectQuery().createObjectQuery(OrFilter.createOr(filters));
            }
        } catch (Exception ex) {
            error(getString("userBrowserDialog.message.queryError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", ex);
        }

        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        model.setObject(new UserBrowserDto());

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(null);

        target.add(getContent().get(ID_SEARCH_FORM));
        target.add(panel);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    public void userDetailsPerformed(AjaxRequestTarget target, UserType user) {
        close(target);
    }
}
