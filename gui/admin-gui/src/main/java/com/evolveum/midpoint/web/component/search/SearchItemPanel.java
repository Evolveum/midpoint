/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPanel extends BasePanel<SearchItem> {

    private static final Trace LOG = TraceManager.getTrace(SearchItemPanel.class);

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";
    private static final String ID_POPOVER = "popover";
    private static final String ID_POPOVER_BODY = "popoverBody";
    private static final String ID_UPDATE = "update";
    private static final String ID_CLOSE = "close";
    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    private LoadableModel<SearchItemPopoverDto> popoverModel;

    public SearchItemPanel(String id, IModel<SearchItem> model) {
        super(id, model);
        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        SearchItem item = getModelObject();
        if (!item.isEditWhenVisible()) {
            return;
        }

        item.setEditWhenVisible(false);

        //todo show popover for this item somehow [lazyman]
    }

    private void initLayout() {
        popoverModel = new LoadableModel<SearchItemPopoverDto>(false) {

            @Override
            protected SearchItemPopoverDto load() {
                return loadPopoverItems();
            }
        };

        AjaxLink mainButton = new AjaxLink(ID_MAIN_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                editPerformed(target);
            }
        };
        add(mainButton);

        Label label = new Label(ID_LABEL, createLabelModel());
        label.setRenderBodyOnly(true);
        mainButton.add(label);

        AjaxLink deleteButton = new AjaxLink(ID_DELETE_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        mainButton.add(deleteButton);
        deleteButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !getModelObject().isFixed();
            }
        });

        initPopover();
    }

    private SearchItemPopoverDto loadPopoverItems() {
        SearchItemPopoverDto dto = new SearchItemPopoverDto();

        SearchItem item = getModelObject();
        for (DisplayableValue<? extends Serializable> value : (List<DisplayableValue>) item.getValues()) {
            DisplayableValue itemValue = new SearchValue(value.getValue(), value.getLabel());
            dto.getValues().add(itemValue);
        }

        if (dto.getValues().isEmpty()) {
            dto.getValues().add(new SearchValue());
        }

        return dto;
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer popoverBody = new WebMarkupContainer(ID_POPOVER_BODY);
        popoverBody.setOutputMarkupId(true);
        popover.add(popoverBody);

        ListView values = new ListView<DisplayableValue>(ID_VALUES,
            new PropertyModel<>(popoverModel, SearchItem.F_VALUES)) {

            @Override
            protected void populateItem(final ListItem<DisplayableValue> item) {
                item.add(AttributeModifier.replace("style", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return item.getIndex() != 0 ? "margin-top: 5px;" : null;
                    }
                }));

                SearchPopupPanel fragment = createPopoverFragment(item.getModel());
                fragment.setRenderBodyOnly(true);
                item.add(fragment);
            }
        };
        popoverBody.add(values);

        AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE, createStringResource("SearchItemPanel.update")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateItemPerformed(target);
            }
        };
        popoverBody.add(update);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchItemPanel.close")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeEditPopoverPerformed(target);
            }
        };
        popoverBody.add(close);
    }

    private SearchPopupPanel createPopoverFragment(IModel<DisplayableValue> data) {
        SearchPopupPanel popup;
        SearchItem item = getModelObject();

        IModel<? extends List> choices = null;

        switch (item.getType()) {
            case BROWSER:
                popup = new BrowserPopupPanel(ID_VALUE, data);
                break;
            case BOOLEAN:
                choices = createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new Model((Serializable) item.getAllowedValues());
                }
                popup = new ComboPopupPanel(ID_VALUE, data, choices);
                break;
            case TEXT:
            default:
                PrismObject<LookupTableType> lookupTable = findLookupTable(item.getDefinition());
                popup = new TextPopupPanel(ID_VALUE, data, lookupTable);
        }

        return popup;
    }

    private PrismObject<LookupTableType> findLookupTable(ItemDefinition definition) {
        PrismReferenceValue valueEnumerationRef = definition.getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }

        PageBase page = getPageBase();

        String lookupTableUid = valueEnumerationRef.getOid();
        Task task = page.createSimpleTask("loadLookupTable");
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils.createLookupTableRetrieveOptions();
        return WebModelServiceUtils.loadObject(LookupTableType.class, lookupTableUid, options, page, task, result);
    }

    private IModel<List<DisplayableValue>> createBooleanChoices() {
        return new AbstractReadOnlyModel<List<DisplayableValue>>() {

            @Override
            public List<DisplayableValue> getObject() {
                List<DisplayableValue> list = new ArrayList<>();
                list.add(new SearchValue(Boolean.TRUE, getString("Boolean.TRUE")));
                list.add(new SearchValue(Boolean.FALSE, getString("Boolean.FALSE")));

                return list;
            }
        };
    }

    private IModel<String> createLabelModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SearchItem item = getModelObject();

                StringBuilder sb = new StringBuilder();
                sb.append(item.getName());
                sb.append(": ");

                List<String> values = new ArrayList<>();
                for (DisplayableValue value : (List<DisplayableValue>) item.getValues()) {
                    if (StringUtils.isNotEmpty(value.getLabel())) {
                        values.add(value.getLabel());
                    }
                }

                if (!values.isEmpty()) {
                    String or = createStringResource("SearchItemPanel.or").getString();

                    sb.append('"');
                    sb.append(StringUtils.join(values, "\" " + or + " \""));
                    sb.append('"');
                } else {
                    String all = createStringResource("SearchItemPanel.all").getString();
                    sb.append(all);
                }

                return sb.toString();
            }
        };
    }

    private void updateItemPerformed(AjaxRequestTarget target) {
        SearchItem item = getModelObject();
        item.getValues().clear();

        SearchItemPopoverDto dto = popoverModel.getObject();
        for (DisplayableValue value : dto.getValues()) {
            item.getValues().add(value);
        }

        LOG.debug("Update item performed, item {} value is {}", item.getName(), item.getValues());

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    public LoadableModel<SearchItemPopoverDto> getPopoverModel() {
        return popoverModel;
    }

    private void closeEditPopoverPerformed(AjaxRequestTarget target) {
        togglePopover(target);
    }

    private void editPerformed(AjaxRequestTarget target) {
        LOG.debug("Edit performed");

        popoverModel.reset();
        target.add(get(createComponentPath(ID_POPOVER, ID_POPOVER_BODY)));
        togglePopover(target);
    }

    public void togglePopover(AjaxRequestTarget target) {
        SearchPanel panel = findParent(SearchPanel.class);
        panel.togglePopover(target, get(ID_MAIN_BUTTON), get(ID_POPOVER), 0);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        SearchItem item = getModelObject();
        LOG.debug("Delete of item {} performed", item.getName());

        Search search = item.getSearch();
        search.delete(item);

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    void updatePopupBody(AjaxRequestTarget target) {
        target.add(get(createComponentPath(ID_POPOVER, ID_POPOVER_BODY)));
    }
}
