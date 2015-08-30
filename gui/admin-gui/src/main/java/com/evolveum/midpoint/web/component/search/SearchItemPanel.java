package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.DisplayableValueImpl;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import org.apache.commons.lang.StringUtils;
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
public class SearchItemPanel extends BaseSimplePanel<SearchItem> {

    private static final Trace LOG = TraceManager.getTrace(SearchItemPanel.class);

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";
    private static final String ID_POPOVER = "popover";
    private static final String ID_UPDATE = "update";
    private static final String ID_CLOSE = "close";
    private static final String ID_CONTENT = "content";
    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_INPUT = "textInput";
    private static final String ID_COMBO = "combo";
    private static final String ID_COMBO_INPUT = "comboInput";
    private static final String ID_BROWSER = "browser";
    private static final String ID_BROWSER_INPUT = "browserInput";
    private static final String ID_BROWSE = "browse";
    private static final String ID_DELETE = "delete";

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
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        Fragment fragment = createPopoverFragment();
        fragment.setRenderBodyOnly(true);
        popover.add(fragment);

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

    private Fragment createPopoverFragment() {
        Fragment fragment;
        SearchItem item = getModelObject();

        IModel value = new PropertyModel(getModel(), SearchItem.F_VALUE);
        IModel<? extends List> choices = null;

        switch (item.getType()) {
            case BROWSER:
                fragment = new BrowserFragment(ID_CONTENT, ID_BROWSER, this, value);
                break;
            case BOOLEAN:
                choices = createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new Model((Serializable) item.getAllowedValues());
                }
                fragment = new ComboFragment(ID_CONTENT, ID_COMBO, this, value, choices);
                break;
            case TEXT:
            default:
                fragment = new TextFragment(ID_CONTENT, ID_TEXT, this, value);
        }

        return fragment;
    }

    private IModel<List<DisplayableValue>> createBooleanChoices() {
        return new AbstractReadOnlyModel<List<DisplayableValue>>() {

            @Override
            public List<DisplayableValue> getObject() {
                List<DisplayableValue> list = new ArrayList<>();
                list.add(new DisplayableValueImpl(Boolean.TRUE, getString("Boolean.TRUE"), null));
                list.add(new DisplayableValueImpl(Boolean.FALSE, getString("Boolean.FALSE"), null));

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

                if (StringUtils.isNotEmpty(item.getDisplayValue())) {
                    sb.append('"').append(item.getDisplayValue()).append('"');
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
        LOG.debug("Update item performed, item {} value is {}", item.getName(), item.getValue());

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

    private static class TextFragment<T extends Serializable> extends Fragment {

        public TextFragment(String id, String markupId, MarkupContainer markupProvider,
                            final IModel<T> value) {
            super(id, markupId, markupProvider);

            final TextField input = new TextField(ID_TEXT_INPUT, value);
            input.setOutputMarkupId(true);
            add(input);

            AjaxLink delete = new AjaxLink(ID_DELETE) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    value.setObject(null);
                    target.add(input);
                }
            };
            add(delete);
        }
    }

    private static class ComboFragment<T extends Serializable> extends Fragment {

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

            AjaxLink delete = new AjaxLink(ID_DELETE) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    value.setObject(null);
                    target.add(input);
                }
            };
            add(delete);
        }
    }

    private static class BrowserFragment<T extends Serializable> extends Fragment {

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

            AjaxLink delete = new AjaxLink(ID_DELETE) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    deletePerformed(target);
                }
            };
            add(delete);
        }

        private void browsePerformed(AjaxRequestTarget target) {
            //todo implement
        }

        private void deletePerformed(AjaxRequestTarget target) {
            //todo implement
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
