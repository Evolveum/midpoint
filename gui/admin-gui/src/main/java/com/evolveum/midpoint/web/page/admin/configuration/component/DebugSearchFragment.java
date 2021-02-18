/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.search.Search;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.ChoiceableChoiceRenderer;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class DebugSearchFragment extends Fragment {

    private static final String ID_SEARCH = "search";
    private static final String ID_ZIP_CHECK = "zipCheck";
    private static final String ID_SHOW_ALL_ITEMS_CHECK = "showAllItemsCheck";
    private static final String ID_SEARCH_FORM = "searchForm";

    public DebugSearchFragment(String id, String markupId, MarkupContainer markupProvider,
            IModel<Search<? extends ObjectType>> model, IModel<Boolean> showAllItemsModel) {
        super(id, markupId, markupProvider, model);

        initLayout(showAllItemsModel);
    }

    private IModel<DebugSearchDto> getModel() {
        //noinspection unchecked
        return (IModel<DebugSearchDto>) getDefaultModel();
    }

    private void initLayout(IModel<Boolean> showAllItemsModel) {

        createSearchForm();

        AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(zipCheck);

        AjaxCheckBox showAllItemsCheck = new AjaxCheckBox(ID_SHOW_ALL_ITEMS_CHECK, showAllItemsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        add(showAllItemsCheck);

    }

    private void createSearchForm() {
        final Form<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);
        searchForm.add(createSearchPanel());
    }

    private WebMarkupContainer createSearchPanel() {
        SearchPanel searchPanel = new SearchPanel(ID_SEARCH, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                DebugSearchFragment.this.searchPerformed(target);
            }
        };
        searchPanel.setOutputMarkupId(true);
        return searchPanel;
    }

    public AjaxCheckBox getZipCheck() {
        return (AjaxCheckBox) get(ID_ZIP_CHECK);
    }

    private IModel<List<ObjectTypes>> createChoiceModel() {
        return new LoadableModel<List<ObjectTypes>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = WebComponentUtil.createObjectTypesList();
                choices.remove(ObjectTypes.OBJECT);
                return choices;
            }
        };
    }

    private IChoiceRenderer<ObjectViewDto<ResourceType>> createResourceRenderer() {
        return new ChoiceableChoiceRenderer<ObjectViewDto<ResourceType>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(ObjectViewDto<ResourceType> object) {
                if (object == null) {
                    return getString("pageDebugList.resource");
                }
                return object.getName();
            }

        };
    }

    protected void searchPerformed(AjaxRequestTarget target) {
    }
}
