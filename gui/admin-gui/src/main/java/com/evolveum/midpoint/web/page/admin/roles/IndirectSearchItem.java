/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

public class IndirectSearchItem extends SpecialSearchItem {

    private final SearchBoxConfigurationHelper searchBoxConfiguration;

    public IndirectSearchItem(Search search, SearchBoxConfigurationHelper searchBoxConfiguration) {
        super(search);
        this.searchBoxConfiguration = searchBoxConfiguration;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
    }


    private IndirectSearchItemConfigurationType getIndirectConfig() {
        return searchBoxConfiguration.getDefaultIndirectConfiguration();
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id) {
        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, new PropertyModel(searchBoxConfiguration, SearchBoxConfigurationHelper.F_INDIRECT_ITEM + "." + IndirectSearchItemConfigurationType.F_INDIRECT.getLocalPart())) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                List<Boolean> choices = new ArrayList<>();
                choices.add(Boolean.TRUE);
                choices.add(Boolean.FALSE);
                DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.ofList(choices), new ChoiceRenderer<Boolean>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(Boolean val) {
                        if (val) {
                            return getPageBase().createStringResource("Boolean.TRUE").getString();
                        }
                        return getPageBase().createStringResource("Boolean.FALSE").getString();
                    }
                }, false);
                inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 68"
                        + "px; max-width: 400px !important;"));
                inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> searchBoxConfiguration != null && !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE)));
                inputPanel.setOutputMarkupId(true);
                return inputPanel;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getIndirectConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel(){
                return Model.of(WebComponentUtil.getTranslatedPolyString(getIndirectConfig().getDisplay().getHelp()));
            }
        };
        panel.add(new VisibleBehaviour(this::isPanelVisible));
        return panel;
    }

    protected boolean isPanelVisible() {
        return searchBoxConfiguration == null
                || (searchBoxConfiguration.getSupportedRelations() != null
                && !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE));
    }

    @Override
    public boolean isApplyFilter() {
        return !searchBoxConfiguration.isSearchScopeVisible()
                || !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE);
    }
}
