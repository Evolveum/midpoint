/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RelationSearchItem extends SpecialSearchItem {

    private final SearchBoxConfigurationHelper searchBoxConfiguration;

    public RelationSearchItem(Search<?> search, SearchBoxConfigurationHelper searchBoxConfiguration) {
        super(search);
        this.searchBoxConfiguration = searchBoxConfiguration;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchSpecialItemPanel<?> createSpecialSearchPanel(String id) {

        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(
                id, new PropertyModel<>(searchBoxConfiguration, SearchBoxConfigurationHelper.F_RELATION_ITEM)) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {

                ReadOnlyModel<List<QName>> availableRelations = new ReadOnlyModel<>(() -> {
                    List<QName> choices = new ArrayList<>();
                    IModel<RelationSearchItemConfigurationType> relationItem = getModelValue();
                    List<QName> relations = relationItem.getObject().getSupportedRelations();
                    if (relations != null && relations.size() > 1) {
                        choices.add(PrismConstants.Q_ANY);
                    }
                    if (relations != null) {
                        choices.addAll(relations);
                    }
                    return choices;
                });

                DropDownChoicePanel<?> inputPanel = new DropDownChoicePanel<>(id,
                        new PropertyModel<>(getModelValue(), RelationSearchItemConfigurationType.F_DEFAULT_VALUE.getLocalPart()),
                        availableRelations, new QNameIChoiceRenderer() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(QName relation) {
                        RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(relation);
                        if (relationDef != null) {
                            DisplayType display = relationDef.getDisplay();
                            if (display != null) {
                                PolyStringType label = display.getLabel();
                                if (PolyStringUtils.isNotEmpty(label)) {
                                    return WebComponentUtil.getTranslatedPolyString(label);
                                }
                            }
                        }
                        if (QNameUtil.match(PrismConstants.Q_ANY, relation)) {
                            return new ResourceModel("RelationTypes.ANY", relation.getLocalPart()).getObject();
                        }
                        return super.getDisplayValue(relation);
                    }
                }, false);
                inputPanel.getBaseFormComponent()
                        .add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                inputPanel.getBaseFormComponent()
                        .add(AttributeAppender.append("style", "width: 100px; max-width: 400px !important;"));

                inputPanel.getBaseFormComponent()
                        .add(new EnableBehaviour(() -> (availableRelations.getObject().size() > 1)
                                && (searchBoxConfiguration != null
                                && !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE))));

                inputPanel.setOutputMarkupId(true);
                return inputPanel;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getReltaionConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getReltaionConfig().getDisplay().getHelp()));
            }
        };
        panel.add(new VisibleBehaviour(this::isPanelVisible));
        return panel;
    }

    protected boolean isPanelVisible() {
        return searchBoxConfiguration == null
                || !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE);
    }

    public boolean isApplyFilter() {
        return !searchBoxConfiguration.isSearchScopeVisible()
                || !searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE);
    }

    private RelationSearchItemConfigurationType getReltaionConfig() {
        return searchBoxConfiguration.getDefaultRelationConfiguration();
    }

}
