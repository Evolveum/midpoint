/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple midPoint principal. This principal should contain only the concepts that are
 * essential for midPoint core to work. It should not contain user interface concepts
 * (e.g. adminGuiConfig). For that see GuiProfiledPrincipal.
 *
 * @author Radovan Semancik
 */
public class MidPointPrincipal implements UserDetails, DebugDumpable, ShortDumpable {
    private static final long serialVersionUID = 8299738301872077768L;

    // Focus should not be final in case of session refresh, we need new focus object.
    @NotNull private FocusType focus;
    private Locale preferredLocale;
    @NotNull private Collection<Authorization> authorizations = new ArrayList<>();
    private ActivationStatusType effectiveActivationStatus;
    private SecurityPolicyType applicableSecurityPolicy;
    // TODO: or a set?
    @NotNull private final Collection<DelegatorWithOtherPrivilegesLimitations> delegatorWithOtherPrivilegesLimitationsCollection = new ArrayList<>();
    private FocusType attorney;
    private MidPointPrincipal previousPrincipal;

    public MidPointPrincipal(@NotNull FocusType focus) {
        Validate.notNull(focus, "Focus must not be null.");
        this.focus = focus;
    }

    @Override
    public @NotNull Collection<Authorization> getAuthorities() {
        return authorizations;
    }

    @Override
    public String getPassword() {
        // We won't return password
        return null;
    }

    @Override
    public String getUsername() {
        return getFocus().getName().getOrig();
    }

    @Override
    public boolean isAccountNonExpired() {
        // TODO
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // TODO
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (effectiveActivationStatus == null) {
            ActivationType activation = focus.getActivation();
            if (activation == null) {
                effectiveActivationStatus = ActivationStatusType.ENABLED;
            } else {
                effectiveActivationStatus = activation.getEffectiveStatus();
                if (effectiveActivationStatus == null) {
                    throw new IllegalArgumentException("Null effective activation status in " + focus);
                }
            }
        }
        return effectiveActivationStatus == ActivationStatusType.ENABLED;
    }

    /**
     * Effective identity that is used to execute all actions.
     * Authorizations of this identity will be applied.
     * This is usually the logged-in user. However, this may be the
     * user on behalf who are the actions executed (donor of power)
     * and the real logged-in user may be the attorney.
     */
    @NotNull
    public FocusType getFocus() {
        return focus;
    }

    public void replaceFocus(FocusType newFocus) {
        focus = newFocus;
        // Efective activation status is derived from focus and its cached
        effectiveActivationStatus = null;
    }

    public PolyStringType getName() {
        return getFocus().getName();
    }

    public String getOid() {
        return getFocus().getOid();
    }

    /**
     * Real identity of the logged-in user. Used in cases when there is a
     * difference between logged-in user and the identity that is used to
     * execute actions and evaluate authorizations.
     * This may happen when one user (attorney) has switched identity
     * to another user (donor of power). In that case the identity of the
     * attorney is in this property. The user that was the target of the
     * switch is stored in the "user" property.
     */
    public FocusType getAttorney() {
        return attorney;
    }

    public void setAttorney(FocusType attorney) {
        this.attorney = attorney;
    }

    /**
     * Principal that was used before this principal was active.
     * This is used when principals are chained (e.g. attorney)
     */
    public MidPointPrincipal getPreviousPrincipal() {
        return previousPrincipal;
    }

    public void setPreviousPrincipal(MidPointPrincipal previousPrincipal) {
        this.previousPrincipal = previousPrincipal;
    }

    public SecurityPolicyType getApplicableSecurityPolicy() {
        return applicableSecurityPolicy;
    }

    public void setApplicableSecurityPolicy(SecurityPolicyType applicableSecurityPolicy) {
        this.applicableSecurityPolicy = applicableSecurityPolicy;
    }

    @NotNull
    public Collection<DelegatorWithOtherPrivilegesLimitations> getDelegatorWithOtherPrivilegesLimitationsCollection() {
        return delegatorWithOtherPrivilegesLimitationsCollection;
    }

    public void addDelegatorWithOtherPrivilegesLimitations(DelegatorWithOtherPrivilegesLimitations value) {
        delegatorWithOtherPrivilegesLimitationsCollection.add(value);
    }

    /**
     * Semi-shallow clone.
     */
    @Override
    public MidPointPrincipal clone() {
        MidPointPrincipal clone = new MidPointPrincipal(this.focus);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(MidPointPrincipal clone) {
        clone.applicableSecurityPolicy = this.applicableSecurityPolicy;
        clone.authorizations = new ArrayList<>(authorizations);
        clone.effectiveActivationStatus = this.effectiveActivationStatus;
        clone.delegatorWithOtherPrivilegesLimitationsCollection.addAll(this.delegatorWithOtherPrivilegesLimitationsCollection);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, this.getClass().getSimpleName(), indent);
        debugDumpInternal(sb, indent);
        return sb.toString();
    }

    protected void debugDumpInternal(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "Focus", focus.asPrismObject(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Authorizations", authorizations, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Delegators with other privilege limitations", delegatorWithOtherPrivilegesLimitationsCollection, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Attorney", attorney == null ? null : attorney.asPrismObject(), indent + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("(");
        sb.append(focus);
        if (attorney != null) {
            sb.append(" [").append(attorney).append("]");
        }
        sb.append(", autz=").append(authorizations);
        sb.append(")");
        return sb.toString();
    }

    public ObjectReferenceType toObjectReference() {
        if (focus.getOid() != null) {
            return ObjectTypeUtil.createObjectRef(focus, SchemaConstants.ORG_DEFAULT);
        } else {
            return null;
        }
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(focus);
        if (attorney != null) {
            sb.append("[").append(attorney).append("]");
        }
    }

    /**
     * Search for locale for this principal in multiple locations, returns first non-null item. Order of search:
     *
     * <ol>
     *     <li>{@link MidPointPrincipal#preferredLocale}</li>
     *     <li>{@link FocusType#getPreferredLanguage()}</li>
     *     <li>{@link FocusType#getLocale()}</li>
     *     <li>{@link Locale#getDefault()}</li>
     * </ol>
     */
    @NotNull
    public Locale getLocale() {
        Locale locale = getPreferredLocale();
        if (locale != null) {
            return locale;
        }

        locale = LocaleUtils.toLocale(focus.getPreferredLanguage());
        if (locale != null) {
            return locale;
        }

        locale = LocaleUtils.toLocale(focus.getLocale());
        if (locale != null) {
            return locale;
        }

        return Locale.getDefault();
    }

    public Locale getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(Locale preferredLocale) {
        this.preferredLocale = preferredLocale;
    }
}
