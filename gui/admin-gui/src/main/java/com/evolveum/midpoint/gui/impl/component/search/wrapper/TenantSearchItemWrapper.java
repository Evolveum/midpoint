/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.TenantSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

public class TenantSearchItemWrapper extends AbstractSearchItemWrapper<ObjectReferenceType> {

    private UserInterfaceFeatureType tenantConfig;

    public TenantSearchItemWrapper(UserInterfaceFeatureType tenantConfig) {
        super();
        this.tenantConfig = tenantConfig;
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
    public String getName() {
        return "abstractRoleMemberPanel.tenant";
    }


    @Override
    public String getHelp() {
        String help = tenantConfig.getDisplay() != null ? WebComponentUtil.getTranslatedPolyString(tenantConfig.getDisplay().getHelp()) : null;
        if (help != null) {
            return help;
        }
        help = getTenantDefinition().getHelp();
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        return getTenantDefinition().getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    public PrismReferenceDefinition getTenantDefinition() {
        return getReferenceDefinition(AssignmentType.F_TENANT_REF);
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                .findReferenceDefinition(refName);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
