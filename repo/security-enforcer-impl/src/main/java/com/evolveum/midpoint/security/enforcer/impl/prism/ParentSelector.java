/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class ParentSelector {

    @NotNull private final ItemPath path;
    @NotNull private final AuthorizationObjectSelectorType selector;

    private ParentSelector(@NotNull ItemPath path, @NotNull AuthorizationObjectSelectorType selector) {
        this.path = path;
        this.selector = selector;
    }

    static ParentSelector forBean(AuthorizationParentSelectorType bean) throws ConfigurationException {
        var itemPath = MiscUtil.configNonNull(bean.getPath(), () -> "No path in " + bean).getItemPath(); // TODO error location
        return new ParentSelector(itemPath, bean);
    }

    private static ParentSelector forItem(@NotNull QName owningType, @NotNull ItemPath item) {
        return new ParentSelector(item, new AuthorizationObjectSelectorType().type(owningType));
    }

    // Eventually, this will be implemented in more declarative way.
    public static ParentSelector implicit(@Nullable QName type) {
        if (QNameUtil.match(type, CaseWorkItemType.COMPLEX_TYPE)) {
            return forItem(CaseType.COMPLEX_TYPE, CaseType.F_WORK_ITEM);
        } else if (QNameUtil.match(type, AccessCertificationCaseType.COMPLEX_TYPE)) {
            return forItem(AccessCertificationCampaignType.COMPLEX_TYPE, AccessCertificationCampaignType.F_CASE);
        } else if (QNameUtil.match(type, AccessCertificationWorkItemType.COMPLEX_TYPE)) {
            return forItem(AccessCertificationCaseType.COMPLEX_TYPE, AccessCertificationCaseType.F_WORK_ITEM);
        } else {
            return null;
        }
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    public @NotNull AuthorizationObjectSelectorType getSelector() {
        return selector;
    }
}
