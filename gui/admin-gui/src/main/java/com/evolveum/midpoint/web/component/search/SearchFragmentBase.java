package com.evolveum.midpoint.web.component.search;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;

/**
 * @author Viliam Repan (lazyman)
 */
abstract class SearchFragmentBase extends Fragment {

    private static final String ID_REMOVE = "remove";
    private static final String ID_ADD = "add";

    SearchFragmentBase(String id, String markupId, MarkupContainer markupProvider) {
        super(id, markupId, markupProvider);

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

    }

    protected void removePerformed(AjaxRequestTarget target) {

    }
}
