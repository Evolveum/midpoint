/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.AUTZ_ALL_URL;

/**
 * Parsed form of {@link AuthorizationType}.
 *
 * @author semancik
 */
public class Authorization implements GrantedAuthority, DebugDumpable {
    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final AuthorizationType authorizationBean;
    private String sourceDescription;

    @NotNull private final PathSet items;
    @NotNull private final PathSet exceptItems;
    private List<ValueSelector> parsedObjectSelectors;
    private List<ValueSelector> parsedTargetSelectors;

    public Authorization(@NotNull AuthorizationType authorizationBean) {
        this.authorizationBean = authorizationBean;
        items = parseItems(this.authorizationBean.getItem());
        exceptItems = parseItems(this.authorizationBean.getExceptItem());
    }

    public static Authorization create(@NotNull AuthorizationType authorizationBean, String sourceDescription) {
        var autz = new Authorization(authorizationBean);
        autz.setSourceDescription(sourceDescription);
        return autz;
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

    public boolean isDeny() {
        return getDecision() == AuthorizationDecisionType.DENY;
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

    public boolean matchesAnyAction(@NotNull List<String> actionUrls) {
        var authorizedActions = getAction();
        return authorizedActions.contains(AUTZ_ALL_URL)
                || authorizedActions.stream().anyMatch(actionUrls::contains);
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

    private @NotNull List<OwnedObjectSelectorType> getObjectSelectors() {
        return authorizationBean.getObject();
    }

    public @NotNull synchronized List<ValueSelector> getParsedObjectSelectors() throws ConfigurationException {
        var cached = parsedObjectSelectors;
        if (cached != null) {
            return cached;
        } else {
            parsedObjectSelectors = parseSelectors(getObjectSelectors());
            return parsedObjectSelectors;
        }
    }

    public @NotNull synchronized List<ValueSelector> getParsedTargetSelectors() throws ConfigurationException {
        var cached = parsedTargetSelectors;
        if (cached != null) {
            return cached;
        } else {
            parsedTargetSelectors = parseSelectors(getTargetSelectors());
            return parsedTargetSelectors;
        }
    }

    private List<ValueSelector> parseSelectors(List<? extends OwnedObjectSelectorType> selectorBeans)
            throws ConfigurationException {
        List<ValueSelector> parsed = new ArrayList<>();
        for (OwnedObjectSelectorType selectorBean : selectorBeans) {
            parsed.add(ValueSelector.parse(selectorBean));
        }
        return parsed;
    }

    @NotNull
    public List<ItemPathType> getItem() {
        return authorizationBean.getItem();
    }

    @NotNull
    public List<ItemPathType> getExceptItem() {
        return authorizationBean.getExceptItem();
    }

    public @NotNull PathSet getItems() {
        return items;
    }

    public @NotNull PathSet getExceptItems() {
        return exceptItems;
    }

    private @NotNull PathSet parseItems(@NotNull List<ItemPathType> beans) {
        var set = new PathSet();
        for (ItemPathType bean : beans) {
            set.add(bean.getItemPath());
        }
        set.freeze();
        return set;
    }

    public boolean hasItemSpecification() {
        return !getItem().isEmpty()
                || !getExceptItem().isEmpty();
    }

    private @NotNull List<OwnedObjectSelectorType> getTargetSelectors() {
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
