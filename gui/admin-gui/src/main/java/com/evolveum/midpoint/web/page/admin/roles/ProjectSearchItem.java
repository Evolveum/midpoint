/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;

public class ProjectSearchItem extends SpecialSearchItem {

    private final SearchBoxConfigurationHelper searchBoxConfiguration;

    public ProjectSearchItem(Search search, SearchBoxConfigurationHelper searchBoxConfiguration) {
        super(search);
        this.searchBoxConfiguration = searchBoxConfiguration;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id) {
        IModel projectModel = new PropertyModel(getMemberPanelStorage(), SearchBoxConfigurationHelper.F_PROJECT) {
            @Override
            public void setObject(Object object) {
                if (object == null) {
                    getMemberPanelStorage().resetProjectRef();
                } else {
                    super.setObject(object);
                }
            }
        };
        PrismReferenceDefinition projectRefDef = getProjectRefDef();
        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, projectModel) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                return new ReferenceValueSearchPanel(id, getModelValue(), projectRefDef) {
                    @Override
                    public Boolean isItemPanelEnabled() {
                        return !(getMemberPanelStorage().isIndirect());
                    }

                    @Override
                    protected List<QName> getAllowedRelations() {
                        return Collections.singletonList(RelationTypes.MEMBER.getRelation());
                    }
                };
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getProjectSearchConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel() {
                if (getProjectSearchConfig().getDisplay().getHelp() != null){
                    return Model.of(WebComponentUtil.getTranslatedPolyString(getProjectSearchConfig().getDisplay().getHelp()));
                }
                String help = projectRefDef.getHelp();
                if (StringUtils.isNotEmpty(help)) {
                    return getPageBase().createStringResource(help);
                }
                return Model.of(projectRefDef.getDocumentation());
            }
        };
        panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                || !getMemberPanelStorage().isIndirect()));
        return panel;
    }

    private UserInterfaceFeatureType getProjectSearchConfig() {
        return searchBoxConfiguration.getDefaultProjectConfiguration();
    }

    public SearchBoxConfigurationHelper getMemberPanelStorage() {
        return searchBoxConfiguration;
    }

    @Override
    public boolean isApplyFilter() {
        return !searchBoxConfiguration.isSearchScopeVisible()
                || (!searchBoxConfiguration.isSearchScope(SearchBoxScopeType.SUBTREE)
                && !searchBoxConfiguration.isRelationVisible()
                && !searchBoxConfiguration.isTenantVisible()
                && !searchBoxConfiguration.isIndirect());
    }

    public PrismReferenceDefinition getProjectRefDef() {
        return getReferenceDefinition(AssignmentType.F_ORG_REF);
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                .findReferenceDefinition(refName);
    }
}
