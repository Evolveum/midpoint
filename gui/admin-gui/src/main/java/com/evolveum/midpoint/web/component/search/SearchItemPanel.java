package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypeDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
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

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPanel extends BaseSimplePanel<SearchItem> {

    private static final Trace LOG = TraceManager.getTrace(SearchItemPanel.class);

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";
    private static final String ID_POPOVER = "popover";
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

    public SearchItemPanel(String id, IModel<SearchItem> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
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

    private void initBrowserPopup() {
        //todo better browser dialog
        ChooseTypeDialog dialog = new ChooseTypeDialog(ID_BROWSER_POPUP, RoleType.class) {

            @Override
            protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object) {
                browserSelectObjectPerformed(target, object);
            }

            @Override
            public boolean isSearchEnabled() {
                return true;
            }

            @Override
            public QName getSearchProperty() {
                return ObjectType.F_NAME;
            }
        };
        add(dialog);
    }

    private void browserSelectObjectPerformed(AjaxRequestTarget target, ObjectType object) {
        SearchItem item = getModelObject();

        PrismReferenceValue ref = PrismReferenceValue.createFromTarget(object.asPrismObject());
        SearchValue value = new SearchValue(ref, object.getName().getOrig());
//        item.setValue(value); //todo fix

        ChooseTypeDialog dialog = (ChooseTypeDialog) get(ID_BROWSER_POPUP);
        dialog.close(target);

        updateItemPerformed(target);
    }

    private void referenceDeleted(AjaxRequestTarget target) {

    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        ListView values = new ListView<DisplayableValue>(ID_VALUES,
                new PropertyModel<List<DisplayableValue>>(getModel(), SearchItem.F_VALUES)) {

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
        popover.add(values);


        AjaxSubmitButton add = new AjaxSubmitButton(ID_UPDATE, createStringResource("SearchItemPanel.update")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateItemPerformed(target);
            }
        };
        popover.add(add);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchItemPanel.close")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeEditPopoverPerformed(target);
            }
        };
        popover.add(close);
    }

    private Fragment createPopoverFragment(IModel<DisplayableValue> model) {
        Fragment fragment;
        SearchItem item = getModelObject();

        IModel value = new PropertyModel(model, SearchValue.F_VALUE);
        IModel<? extends List> choices = null;

        switch (item.getType()) {
            case BROWSER:
                value = new PropertyModel(getModel(), SearchValue.F_LABEL);
                fragment = new BrowserFragment(ID_VALUE, ID_BROWSER, this, value);
                break;
            case BOOLEAN:
                choices = createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new Model((Serializable) item.getAllowedValues());
                }
                fragment = new ComboFragment(ID_VALUE, ID_COMBO, this, value, choices);
                break;
            case TEXT:
            default:
                fragment = new TextFragment(ID_VALUE, ID_TEXT, this, value);
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

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(initButtonJavascript()));
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

    private String initButtonJavascript() {
        StringBuilder sb = new StringBuilder();
        String moreId = get(ID_MAIN_BUTTON).getMarkupId();
        String popoverId = get(ID_POPOVER).getMarkupId();

        sb.append("initSearchPopover('").append(moreId);
        sb.append("','").append(popoverId).append("', 0);");

        return sb.toString();
    }

    private void updateItemPerformed(AjaxRequestTarget target) {
        SearchItem item = getModelObject();
        LOG.debug("Update item performed, item {} value is {}", item.getName(), item.getValues());

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    private void closeEditPopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    private void editPerformed(AjaxRequestTarget target) {
        LOG.debug("Edit performed");
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

    private static class TextFragment extends SearchFragmentBase {

        public TextFragment(String id, String markupId, MarkupContainer markupProvider,
                            IModel<DisplayableValue> value) {
            super(id, markupId, markupProvider);

            final TextField input = new TextField(ID_TEXT_INPUT, value);
            input.setOutputMarkupId(true);
            add(input);
        }

        @Override
        protected void addPerformed(AjaxRequestTarget target) {
            // todo implement
        }

        @Override
        protected void removePerformed(AjaxRequestTarget target) {
            // value.setObject(null);
            // target.add(input);
            // todo implement
        }
    }

    private static class ComboFragment<T extends Serializable> extends SearchFragmentBase {

        public ComboFragment(String id, String markupId, MarkupContainer markupProvider,
                             final IModel<T> value, IModel<List<T>> choices) {
            super(id, markupId, markupProvider);

            final DisplayableRenderer renderer = new DisplayableRenderer(choices);
            final DropDownChoice input = new DropDownChoice(ID_COMBO_INPUT, value, choices, renderer) {

                @Override
                public IConverter getConverter(Class type) {
                    return renderer;
                }
            };
            input.setNullValid(true);
            input.setOutputMarkupId(true);
            add(input);
        }

        @Override
        protected void addPerformed(AjaxRequestTarget target) {
            // todo implement
        }

        @Override
        protected void removePerformed(AjaxRequestTarget target) {
            // value.setObject(null);
            // target.add(input);
            // todo implement
        }
    }

    private static class BrowserFragment<T extends Serializable> extends SearchFragmentBase {

        public BrowserFragment(String id, String markupId, MarkupContainer markupProvider,
                               IModel<T> value) {
            super(id, markupId, markupProvider);

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
            SearchItemPanel panel = findParent(SearchItemPanel.class);
            ChooseTypeDialog dialog = (ChooseTypeDialog) panel.get(ID_BROWSER_POPUP);
            dialog.show(target);
        }

        @Override
        protected void addPerformed(AjaxRequestTarget target) {
            // todo implement
        }

        @Override
        protected void removePerformed(AjaxRequestTarget target) {
            // value.setObject(null);
            // target.add(input);
            // todo implement
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
