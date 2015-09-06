package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
abstract class SearchFragmentBase<T extends Serializable> extends Fragment {

    private static final String ID_REMOVE = "remove";
    private static final String ID_ADD = "add";

    private IModel<T> data;

    SearchFragmentBase(String id, String markupId, MarkupContainer markupProvider, IModel<T> data) {
        super(id, markupId, markupProvider);

        this.data = data;
        initButtons();
    }

    private void initButtons() {
        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target);
            }
        };
        add(remove);

        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add(add);
    }

    protected void addPerformed(AjaxRequestTarget target) {
        SearchItemPanel panel = findParent(SearchItemPanel.class);
        panel.getModelObject().getValues().add(new SearchValue());
        panel.updatePopupBody(target);
    }

    protected void removePerformed(AjaxRequestTarget target) {
        SearchItemPanel panel = findParent(SearchItemPanel.class);
        T val = data.getObject();

        panel.getModelObject().getValues().remove(val);

        panel.updatePopupBody(target);
    }
}
