/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author semancik
 *
 */
public class Authorization implements GrantedAuthority, DebugDumpable {
    private static final long serialVersionUID = 1L;

    @NotNull private final AuthorizationType authorizationBean;
    private String sourceDescription;

    public Authorization(@NotNull AuthorizationType authorizationBean) {
        this.authorizationBean = authorizationBean;
    }

    @Override
    public String getAuthority() {
        // this is complex authority. Just return null
        return null;
    }

    public String getDescription() {
        return authorizationBean.getDescription();
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public @NotNull AuthorizationDecisionType getDecision() {
        return Objects.requireNonNullElse(
                authorizationBean.getDecision(),
                AuthorizationDecisionType.ALLOW);
    }

    public boolean isAllow() {
        return getDecision() == AuthorizationDecisionType.ALLOW;
    }

    public @NotNull List<String> getAction() {
        return authorizationBean.getAction();
    }

    public @Nullable AuthorizationPhaseType getPhase() {
        return authorizationBean.getPhase();
    }

    public boolean matchesPhase(@Nullable AuthorizationPhaseType phase) {
        var autzPhase = getPhase();
        return autzPhase == null || autzPhase == phase;
    }

    public AuthorizationEnforcementStrategyType getEnforcementStrategy() {
        return authorizationBean.getEnforcementStrategy();
    }

    public boolean maySkipOnSearch() {
        return getEnforcementStrategy() == AuthorizationEnforcementStrategyType.MAY_SKIP_ON_SEARCH;
    }

    public boolean keepZoneOfControl() {
        ZoneOfControlType zoneOfControl = authorizationBean.getZoneOfControl();
        return zoneOfControl == null || zoneOfControl == ZoneOfControlType.KEEP;
    }

    public @NotNull List<OwnedObjectSelectorType> getObjectSelectors() {
        return authorizationBean.getObject();
    }

    @NotNull
    public List<ItemPathType> getItem() {
        return authorizationBean.getItem();
    }

    @NotNull
    public List<ItemPathType> getExceptItem() {
        return authorizationBean.getExceptItem();
    }

    @NotNull
    public List<ItemPath> getItems() {
        List<ItemPathType> itemPaths = getItem();
        // TODO: maybe we can cache the itemPaths here?
        List<ItemPath> items = new ArrayList<>(itemPaths.size());
        for (ItemPathType itemPathType: itemPaths) {
            items.add(itemPathType.getItemPath());
        }
        return items;
    }

    @NotNull
    public List<ItemPath> getExceptItems() {
        List<ItemPathType> itemPaths = getExceptItem();
        // TODO: maybe we can cache the itemPaths here?
        List<ItemPath> items = new ArrayList<>(itemPaths.size());
        for (ItemPathType itemPathType: itemPaths) {
            items.add(itemPathType.getItemPath());
        }
        return items;
    }

    public @NotNull List<ItemValueSelectorType> getItemValueSelectors() {
        return authorizationBean.getItemValue();
    }

    public boolean hasItemSpecification() {
        return !getItem().isEmpty()
                || !getExceptItem().isEmpty()
                || !getItemValueSelectors().isEmpty();
    }

    public @NotNull List<OwnedObjectSelectorType> getTargetSelectors() {
        return authorizationBean.getTarget();
    }

    @NotNull
    public List<QName> getRelation() {
        return authorizationBean.getRelation();
    }

    public OrderConstraintsType getOrderConstraints() {
        return authorizationBean.getOrderConstraints();
    }

    public AuthorizationLimitationsType getLimitations() {
        return authorizationBean.getLimitations();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Authorization clone() {
        Authorization clone = new Authorization(authorizationBean.clone());
        clone.sourceDescription = this.sourceDescription;
        return clone;
    }

    public String getHumanReadableDesc() {
        StringBuilder sb = new StringBuilder();
        if (authorizationBean.getName() != null) {
            sb.append("authorization '").append(authorizationBean.getName()).append("'");
        } else {
            sb.append("unnamed authorization");
        }
        if (sourceDescription != null) {
            Long id = authorizationBean.getId();
            if (id != null) {
                sb.append(" (#").append(id).append(")");
            }
            sb.append(" in ");
            sb.append(sourceDescription);
        }
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "Authorization", indent);
        sb.append("\n");
        authorizationBean.asPrismContainerValue().debugDump(indent+1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Authorization(" + authorizationBean.getAction() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Authorization that = (Authorization) o;
        return Objects.equals(authorizationBean, that.authorizationBean)
                && Objects.equals(sourceDescription, that.sourceDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizationBean, sourceDescription);
    }
}
