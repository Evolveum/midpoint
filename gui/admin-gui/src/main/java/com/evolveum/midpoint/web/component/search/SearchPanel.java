package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchPanel extends BaseSimplePanel<Search> {

    private static final String ID_FORM = "form";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_SEARCH = "more";
    private static final String ID_MORE = "search";

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

        AjaxLink more = new AjaxLink(ID_MORE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                morePerformed(target);
            }
        };
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
    }

    private void morePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    public void searchPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
