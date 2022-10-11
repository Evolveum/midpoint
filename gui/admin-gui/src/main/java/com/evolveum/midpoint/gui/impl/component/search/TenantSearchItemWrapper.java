/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.List;

public class TenantSearchItemWrapper extends AbstractRoleSearchItemWrapper {

    public TenantSearchItemWrapper(SearchConfigurationWrapper searchConfig) {
        super(searchConfig);
    }

    @Override
    public boolean isEnabled() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public boolean isVisible() {
        return !getSearchConfig().isIndirect();
    }

    @Override
    public Class<TenantSearchItemPanel> getSearchItemPanelClass() {
        return TenantSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        return new SearchValue<>(ref);
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getValue() {
        if (getSearchConfig().getTenantRef() == null) {
            return getDefaultValue();
        }
        return new SearchValue<>(getSearchConfig().getTenantRef());
    }

    @Override
    protected String getNameResourceKey() {
        return "abstractRoleMemberPanel.tenant";
    }

    @Override
    protected String getHelpResourceKey() {
        return "";
    }

    @Override
    public String getHelp() {
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        String help = getTenantDefinition().getHelp();
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        return getTenantDefinition().getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    public boolean isApplyFilter() {
        //todo check
        return SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
    }

    public PrismReferenceDefinition getTenantDefinition() {
        return getReferenceDefinition(AssignmentType.F_TENANT_REF);
    }

}
