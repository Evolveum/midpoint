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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.AbstractConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_INPUT = "textInput";
    private static final String ID_COMBO = "combo";
    private static final String ID_COMBO_INPUT = "comboInput";
    private static final String ID_BROWSER = "browser";
    private static final String ID_BROWSER_INPUT = "browserInput";
    private static final String ID_BROWSE = "browse";
    private static final String ID_BROWSER_POPUP = "browserPopup";

    private LoadableModel<SearchItemPopoverDto> popoverModel;

    public SearchItemPanel(String id, IModel<SearchItem> model) {
        super(id, model);
        initLayout();
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

        initPopover();
        initBrowserPopup();
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

    private void initBrowserPopup() {
        //todo better browser dialog
//        ChooseTypeDialog dialog = new ChooseTypeDialog(ID_BROWSER_POPUP, RoleType.class) {
//
//            @Override
//            protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
//                browserSelectObjectPerformed(target, object);
//            }
//
//            @Override
//            public boolean isSearchEnabled() {
//                return true;
//            }
//
//            @Override
//            public QName getSearchProperty() {
//                return ObjectType.F_NAME;
//            }
//        };
//        add(dialog);
    }

    private void browserSelectObjectPerformed(AjaxRequestTarget target, ObjectType object) {
        SearchItem item = getModelObject();

        PrismReferenceValue ref = PrismReferenceValue.createFromTarget(object.asPrismObject());
        SearchValue value = new SearchValue(ref, object.getName().getOrig());
//        item.setValue(value); //todo fix

//        ChooseTypeDialog dialog = (ChooseTypeDialog) get(ID_BROWSER_POPUP);
//        dialog.close(target);

        updateItemPerformed(target);
    }

    private void referenceDeleted(AjaxRequestTarget target) {

    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer popoverBody = new WebMarkupContainer(ID_POPOVER_BODY);
        popoverBody.setOutputMarkupId(true);
        popover.add(popoverBody);

        ListView values = new ListView<DisplayableValue>(ID_VALUES,
                new PropertyModel<List<DisplayableValue>>(popoverModel, SearchItem.F_VALUES)) {

            @Override
            protected void populateItem(final ListItem<DisplayableValue> item) {
                item.add(AttributeModifier.replace("style", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return item.getIndex() != 0 ? "margin-top: 5px;" : null;
                    }
                }));

                Fragment fragment = createPopoverFragment(item.getModel());
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

    private Fragment createPopoverFragment(IModel<DisplayableValue> data) {
        Fragment fragment;
        SearchItem item = getModelObject();

        IModel<? extends List> choices = null;

        switch (item.getType()) {
            case BROWSER:
                fragment = new BrowserFragment(ID_VALUE, ID_BROWSER, this, data);
                break;
            case BOOLEAN:
                choices = createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new Model((Serializable) item.getAllowedValues());
                }
                fragment = new ComboFragment(ID_VALUE, ID_COMBO, this, data, choices);
                break;
            case TEXT:
            default:
                fragment = new TextFragment(ID_VALUE, ID_TEXT, this, data);
        }

        return fragment;
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

    private static class TextFragment extends SearchFragmentBase {

        public TextFragment(String id, String markupId, MarkupContainer markupProvider,
                            IModel<DisplayableValue> value) {
            super(id, markupId, markupProvider, value);

            IModel data = new PropertyModel(value, SearchValue.F_VALUE);
            final TextField input = new TextField(ID_TEXT_INPUT, data);
            input.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    //nothing to do, just update model data
                }
            });
            input.setOutputMarkupId(true);
            add(input);
        }
    }

    private static class ComboFragment<T extends Serializable> extends SearchFragmentBase {

        public ComboFragment(String id, String markupId, MarkupContainer markupProvider,
                             final IModel<T> value, IModel<List<T>> choices) {
            super(id, markupId, markupProvider, value);

            IModel data = new PropertyModel(value, SearchValue.F_VALUE);

            final DisplayableRenderer renderer = new DisplayableRenderer(choices);
            final DropDownChoice input = new DropDownChoice(ID_COMBO_INPUT, data, choices, renderer) {

                @Override
                public IConverter getConverter(Class type) {
                    return renderer;
                }
            };
            input.setNullValid(true);
            input.setOutputMarkupId(true);
            add(input);
        }
    }

    private static class BrowserFragment<T extends Serializable> extends SearchFragmentBase {

        public BrowserFragment(String id, String markupId, MarkupContainer markupProvider,
                               IModel<T> data) {
            super(id, markupId, markupProvider, data);

            IModel value = new PropertyModel(data, SearchValue.F_LABEL);
            TextField input = new TextField(ID_BROWSER_INPUT, value);
            add(input);

            AjaxLink browse = new AjaxLink(ID_BROWSE) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    browsePerformed(target);
                }
            };
            add(browse);
        }

        private void browsePerformed(AjaxRequestTarget target) {
//            SearchItemPanel panel = findParent(SearchItemPanel.class);
//            ChooseTypeDialog dialog = (ChooseTypeDialog) panel.get(ID_BROWSER_POPUP);
//            dialog.show(target);
        }
    }

    private static class DisplayableRenderer<T extends Serializable> extends AbstractConverter<DisplayableValue>
            implements IChoiceRenderer<DisplayableValue<T>> {

        private IModel<List<DisplayableValue>> allChoices;

        public DisplayableRenderer(IModel<List<DisplayableValue>> allChoices) {
            this.allChoices = allChoices;
        }

        @Override
        protected Class<DisplayableValue> getTargetType() {
            return DisplayableValue.class;
        }

        @Override
        public Object getDisplayValue(DisplayableValue<T> object) {
            if (object == null) {
                return null;
            }

            return object.getLabel();
        }

        @Override
        public String getIdValue(DisplayableValue<T> object, int index) {
            return Integer.toString(index);
        }
        
        @Override
        public DisplayableValue<T> getObject(String id, IModel<? extends List<? extends DisplayableValue<T>>> choices) {
        	return choices.getObject().get(Integer.parseInt(id));
        }

        @Override
        public DisplayableValue<T> convertToObject(String value, Locale locale) throws ConversionException {
            if (value == null) {
                return null;
            }

            List<DisplayableValue> values = allChoices.getObject();
            for (DisplayableValue val : values) {
                if (value.equals(val.getLabel())) {
                    return val;
                }
            }

            return null;
        }
    }
}
