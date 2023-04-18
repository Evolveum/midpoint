/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */

public class FocusIdentificationAuthenticationContext extends AbstractAuthenticationContext {

    private Map<ItemPath, String> values;
    private List<ModuleItemConfigurationType> config;

    private String username;

    public FocusIdentificationAuthenticationContext(
            Map<ItemPath, String> values, Class<? extends FocusType> principalType, List<ModuleItemConfigurationType> config, List<ObjectReferenceType> requireAssignment) {
        super(null, principalType, requireAssignment);
        this.values = values;
        this.config = config;
    }

    @Override
    public Object getEnteredCredential() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectQuery createFocusQuery() {
        List<ObjectFilter> filters = new ArrayList<>();
        for (Map.Entry<ItemPath, String> entry : values.entrySet()) {
            ItemPath path = entry.getKey();
            S_FilterExit matchingRuleEntry = PrismContext.get()
                    .queryFor(getPrincipalType())
                        .item(path)
                        .eq(entry.getValue());

            ModuleItemConfigurationType moduleItemConfigurationType = findConfigFor(path);
            if (moduleItemConfigurationType != null && moduleItemConfigurationType.getMatchingRule() != null) {
                matchingRuleEntry = ((S_MatchingRuleEntry) matchingRuleEntry).matching(moduleItemConfigurationType.getMatchingRule());
            }
            ObjectFilter objectFilter = matchingRuleEntry.buildFilter();
            filters.add(objectFilter);
        }
        OrFilter orFilter = PrismContext.get().queryFactory().createOr(filters);

        ObjectQuery query = PrismContext.get().queryFor(getPrincipalType()).build();
        query.addFilter(orFilter);

        ObjectQuery simplified = ObjectQueryUtil.simplifyQuery(query);
        if (ObjectQueryUtil.isNoneQuery(simplified)) {
            return null;
        }
        return simplified;
    }

    private ModuleItemConfigurationType findConfigFor(ItemPath path) {
        if (config == null) {
            return null;
        }

        for (ModuleItemConfigurationType itemConfig : config) {
            ItemPathType configuredtPathType = itemConfig.getPath();
            if (configuredtPathType == null) {
                //TODO what to do in such a case?
                continue;
            }
            ItemPath configuredPath = configuredtPathType.getItemPath();
            if (configuredPath.equivalent(path)) {
                return itemConfig;
            }
        }
        return null;
    }

    @Override
    public String getUsername() {
        if (StringUtils.isNotBlank(username)) {
            return username;
        }
        for (String username : values.values()) {
            if (StringUtils.isNotBlank(username)) {
                this.username = username;
                break;
            }
        }
        return username;
    }
}
