/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author honchar
 */
public class TypeSearchPanel<C extends Containerable> extends AbstractSearchPanel<C>{
    private static final long serialVersionUID = 1L;

    private static final String ID_TYPE_PANEL = "typePanel";
    private static final String ID_TYPE_SEARCH_FRAGMENT = "typeSearchFragment";

    public TypeSearchPanel(String id, IModel<Search<C>> model) {
        super(id, model);
    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {
        TypeSearchFragment typeSearchFragment = new TypeSearchFragment(searchItemsRepeatingView.newChildId(),
                ID_TYPE_SEARCH_FRAGMENT, TypeSearchPanel.this);
        typeSearchFragment.setOutputMarkupId(true);
        searchItemsRepeatingView.add(typeSearchFragment);

        super.initSearchItemsPanel(searchItemsRepeatingView);
    }

    protected void searchPerformed(AjaxRequestTarget target) {

    }

    class TypeSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public TypeSearchFragment(String id, String markupId, AbstractSearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initTypeLayout();
        }

        private void initTypeLayout() {
            PropertyModel<ContainerTypeSearchItem> typeModel = new PropertyModel<>(getModel(), Search.F_TYPE);
            SearchTypePanel typePanel = new SearchTypePanel(ID_TYPE_PANEL, typeModel){
                private static final long serialVersionUID = 1L;

                @Override
                protected void searchPerformed(AjaxRequestTarget target) {
                    TypeSearchPanel.this.getModelObject().getItemsModel().reset();
//                    resetMoreDialogModel();
                    TypeSearchPanel.this.searchPerformed(target);
                }
            };
            add(typePanel);
            typePanel.add(new VisibleBehaviour(() -> typeModel != null && typeModel.getObject() != null && typeModel.getObject().isVisible()
                    && !SearchBoxModeType.OID.equals(getModelObject().getSearchType())));
        }
    }
}
