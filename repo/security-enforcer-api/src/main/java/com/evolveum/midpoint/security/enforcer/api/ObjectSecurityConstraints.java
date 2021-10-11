/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

public interface ObjectSecurityConstraints extends DebugDumpable {

    /**
     * Almost the same as  findAllItemsDecision(String, ...), but in this case there are several equivalent action URLs.
     * E.g. "read" and "get" actions. If any of them is denied, operation is denied. If any of them is allowed, operation is allowed.
     */
    AuthorizationDecisionType findAllItemsDecision(String[] actionUrls, AuthorizationPhaseType phase);

    /**
     * Returns decision for the whole action. This is fact returns a decision that applies to all items - if there is any.
     * If there is no universally-applicable decision then null is returned. In that case there may still be fine-grained
     * decisions for individual items. Use findItemDecision() to get them.
     */
    AuthorizationDecisionType findAllItemsDecision(String actionUrl, AuthorizationPhaseType phase);

    AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath, String[] actionUrls, AuthorizationPhaseType phase);

    AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath, String actionUrl, AuthorizationPhaseType phase);

}
