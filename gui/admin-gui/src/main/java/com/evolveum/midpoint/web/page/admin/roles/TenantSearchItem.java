/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public class TenantSearchItem extends SpecialSearchItem {

    private final MemberPanelStorage memberStorage;

    public TenantSearchItem(Search search, MemberPanelStorage memberStorage) {
        super(search);
        this.memberStorage = memberStorage;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
    }

    private UserInterfaceFeatureType getTenantConfig() {
        return memberStorage.getTenantSearchItem();
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id){
        IModel tenantModel = new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_TENANT) {
            @Override
            public void setObject(Object object) {
                if (object == null) {
                    getMemberPanelStorage().resetTenantRef();
                } else {
                    super.setObject(object);
                }
            }
        };
        PrismReferenceDefinition tenantRefDef = getTenantDefinition();
        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, tenantModel) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id, getModelValue(), tenantRefDef) {
                    @Override
                    public Boolean isItemPanelEnabled() {
                        return !getMemberPanelStorage().isIndirect();
                    }

                    @Override
                    protected List<QName> getAllowedRelations() {
                        return Collections.singletonList(RelationTypes.MEMBER.getRelation());
                    }
                };
                return searchItemField;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getTenantConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel() {
                if (getTenantConfig().getDisplay().getHelp() != null){
                    return Model.of(WebComponentUtil.getTranslatedPolyString(getTenantConfig().getDisplay().getHelp()));
                }
                String help = tenantRefDef.getHelp();
                if (StringUtils.isNotEmpty(help)) {
                    return getPageBase().createStringResource(help);
                }
                return Model.of(tenantRefDef.getDocumentation());
            }
        };
        panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                || !getMemberPanelStorage().isIndirect()));
        return panel;
    }

    public MemberPanelStorage getMemberPanelStorage() {
        return memberStorage;
    }

    public PrismReferenceDefinition getTenantDefinition() {
        return null;
    }
}
