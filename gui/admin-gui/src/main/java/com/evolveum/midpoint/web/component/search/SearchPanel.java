package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchPanel extends BaseSimplePanel<Search> {

    private static final Trace LOG = TraceManager.getTrace(SearchPanel.class);

    private static final String ID_FORM = "form";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_SEARCH = "search";
    private static final String ID_MORE = "more";
    private static final String ID_POPOVER = "popover";
    private static final String ID_ADD_TEXT = "addText";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";

    private IModel<ItemDefinition> addItemModel;

    public SearchPanel(String id, IModel<Search> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

        ListView items = new ListView<SearchItem>(ID_ITEMS,
                new PropertyModel<List<SearchItem>>(getModel(), Search.F_ITEMS)) {

            @Override
            protected void populateItem(ListItem<SearchItem> item) {
                SearchItemPanel searchItem = new SearchItemPanel(ID_ITEM, item.getModel());
                item.add(searchItem);
            }
        };
        form.add(items);

        WebMarkupContainer more = new WebMarkupContainer(ID_MORE);
        more.setOutputMarkupId(true);
        form.add(more);

        AjaxSubmitLink search = new AjaxSubmitLink(ID_SEARCH) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        form.add(search);

        initPopover();
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        addItemModel = new Model<>();
        AutoCompleteTextField addText = new DefinitionAutoComplete(ID_ADD_TEXT, addItemModel,
                new PropertyModel<List<ItemDefinition>>(getModel(), Search.F_AVAILABLE_DEFINITIONS));
        addText.add(new AjaxFormComponentUpdatingBehavior("onblur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        popover.add(addText);

        AjaxButton add = new AjaxButton(ID_ADD, createStringResource("SearchPanel.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addItemPerformed(target);
            }
        };
        popover.add(add);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchPanel.close")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeMorePopoverPerformed(target);
            }
        };
        popover.add(close);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(initMoreButtonJavascript()));
    }

    private String initMoreButtonJavascript() {
        StringBuilder sb = new StringBuilder();
        String moreId = get(createComponentPath(ID_FORM, ID_MORE)).getMarkupId();
        String popoverId = get(ID_POPOVER).getMarkupId();

        sb.append("initSearchPopover('").append(moreId);
        sb.append("','").append(popoverId).append("', 27);");

        return sb.toString();
    }

    private void addItemPerformed(AjaxRequestTarget target) {
        ItemDefinition def = addItemModel.getObject();
        if (def == null) {
            //todo show error
            return;
        }

        Search search = getModelObject();
        SearchItem item = new SearchItem(search, def);
        search.add(item);

        addItemModel.setObject(null);

        refreshForm(target);
    }

    private void closeMorePopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createObjectQuery();
        searchPerformed(query, target);
    }

    public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
    }

    void refreshForm(AjaxRequestTarget target) {
        target.add(get(ID_FORM), get(ID_POPOVER));
        target.appendJavaScript(initMoreButtonJavascript());
    }

    private ObjectQuery createObjectQuery() {
        Search search = getModelObject();
        List<SearchItem> searchItems = search.getItems();
        if (searchItems.isEmpty()) {
            return null;
        }

        List<ObjectFilter> conditions = new ArrayList<>();
        for (SearchItem item : searchItems) {
            ObjectFilter filter = createFilterFromItem(item);
            if (filter != null) {
                conditions.add(filter);
            }
        }

        if (conditions.size() == 1) {
            return ObjectQuery.createObjectQuery(conditions.get(0));
        }

        AndFilter and = AndFilter.createAnd(conditions);
        return ObjectQuery.createObjectQuery(and);
    }

    private ObjectFilter createFilterFromItem(SearchItem item) {
        if (item.getValue() == null) {
            return null;
        }

        ItemDefinition definition = item.getDefinition();

        if (definition instanceof PrismReferenceDefinition) {
            //todo implement
            return null;
        }

        PrismPropertyDefinition propDef = (PrismPropertyDefinition) definition;
        ItemPath path = new ItemPath(ObjectType.F_NAME); //todo implement
        Object value = item.getValue();

        return EqualFilter.createEqual(path, propDef, value);
    }
}
