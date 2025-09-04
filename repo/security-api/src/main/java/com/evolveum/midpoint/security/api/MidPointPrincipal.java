/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import java.io.Serial;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.prism.FreezableList;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType;

import org.apache.commons.lang3.LocaleUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Simple midPoint principal. This principal should contain only the concepts that are
 * essential for midPoint core to work. It should not contain user interface concepts
 * (e.g. adminGuiConfig). For that see GuiProfiledPrincipal.
 *
 * @author Radovan Semancik
 */
public class MidPointPrincipal implements UserDetails, DebugDumpable, ShortDumpable {
    @Serial private static final long serialVersionUID = 8299738301872077768L;

    /** Focus should not be final in case of session refresh: we need new focus object there. */
    @NotNull private FocusType focus;

    /** OID of the {@link #focus}. Does not change in the case of session refresh; at least not now. */
    @NotNull private final String focusOid;

    /** Lazily evaluated from the focus. */
    private ActivationStatusType effectiveActivationStatus;

    private Locale preferredLocale;

    /**
     * Current authorizations. Should be modified solely by methods in this class, e.g. {@link #addAuthorization(Authorization)}
     * or {@link #addExtraAuthorizationIfMissing(Authorization, boolean)}.
     *
     * It would be the best if this list was immutable or at least freezable (e.g. using {@link FreezableList}).
     * Unfortunately, it is currently not possible, because it has to be updated when the user session is refreshed.
     * Still, to avoid asynch. calls to cleaned up authorizations list, we wrap it to AtomicReference for now (#10781)
     */
    @NotNull private final AtomicReference<List<Authorization>> authorizations = new AtomicReference<>(new ArrayList<>());

    /**
     * Set if the authorizations may differ from the default ones of {@link #focus} (e.g., when "runPrivileged" is used).
     * Not final because the {@link #authorizations} list is not immutable/freezable either.
     */
    @Nullable private EffectivePrivilegesModificationType effectivePrivilegesModification;

    private SecurityPolicyType applicableSecurityPolicy;

    /** Delegations with privileges limitations; TODO better name */
    @NotNull private final AtomicReference<OtherPrivilegesLimitations> otherPrivilegesLimitations =
            new AtomicReference<>(new OtherPrivilegesLimitations());

    private FocusType attorney;
    private MidPointPrincipal previousPrincipal;

    /** Use static factory methods when calling from the outside. */
    protected MidPointPrincipal(@NotNull FocusType focus) {
        focusOid = MiscUtil.argNonNull(focus.getOid(), "No OID in principal focus object: %s", focus);
        setOrReplaceFocus(focus);
    }

    /** Returns a principal with a single privileged authorization; regardless of what authorizations the focus has. */
    public static @NotNull MidPointPrincipal privileged(@NotNull FocusType focus) {
        var principal = new MidPointPrincipal(focus);
        principal.addExtraAuthorizationIfMissing(
                SecurityUtil.createPrivilegedAuthorization(),
                true);
        return principal;
    }

    /** Returns a principal without authorizations. */
    public static MidPointPrincipal create(@NotNull FocusType focus) {
        return new MidPointPrincipal(focus);
    }

    @Override
    public @NotNull Collection<Authorization> getAuthorities() {
        return Collections.unmodifiableList(authorizations.get());
    }

    /** Use only during "regular" building or updating of a principal. Does NOT set {@link #effectivePrivilegesModification} flag. */
    public void addAuthorization(@NotNull Authorization authorization) {
        List<Authorization> newAuthList = new ArrayList<>(authorizations.get());
        newAuthList.add(authorization);
        resetAuthorizationsList(newAuthList);
    }

    /**
     * Use to add extra authorizations - it sets {@link #effectivePrivilegesModification} flag.
     *
     * The "if missing" will be (most of the time) a false positive match:
     *
     * . The authorization source will most probably differ between role-derived and artificial (runPrivileged) one;
     * . Even if that would not be the case, any minor difference (like in name or description) would count as well.
     *
     * So, the full elevation would be signalled for the majority of cases even if the equivalent authorization was there.
     */
    public void addExtraAuthorizationIfMissing(@NotNull Authorization authorization, boolean full) {
        if (!authorizations.get().contains(authorization)) {
            addAuthorization(authorization);
            if (full) {
                effectivePrivilegesModification = EffectivePrivilegesModificationType.FULL_ELEVATION;
            } else if (effectivePrivilegesModification != EffectivePrivilegesModificationType.REDUCTION) {
                effectivePrivilegesModification = EffectivePrivilegesModificationType.ELEVATION;
            } else {
                effectivePrivilegesModification = EffectivePrivilegesModificationType.OTHER;
            }
        }
    }

    public void resetAuthorizationsList(List<Authorization> newAuthList) {
        authorizations.set(Collections.unmodifiableList(newAuthList));
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

    public PrismObject<? extends FocusType> getFocusPrismObject() {
        return focus.asPrismObject();
    }

    /** Must not change focus OID (at least for now). */
    public void setOrReplaceFocus(@NotNull FocusType newFocus) {
        String newOid = newFocus.getOid();
        argCheck(
                focusOid.equals(newOid),
                "An attempt to change focus OID from %s to %s",
                focusOid, newOid);

        focus = newFocus;
        // Effective activation status is derived from focus and it's cached
        effectiveActivationStatus = null;
    }

    public PolyStringType getName() {
        return getFocus().getName();
    }

    public String getOid() {
        return getFocus().getOid();
    }

    public @Nullable EffectivePrivilegesModificationType getEffectivePrivilegesModification() {
        return effectivePrivilegesModification;
    }

    public void clearEffectivePrivilegesModification() {
        this.effectivePrivilegesModification = null;
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
    public @Nullable FocusType getAttorney() {
        return attorney;
    }

    public @Nullable PrismObject<? extends FocusType> getAttorneyPrismObject() {
        return asPrismObject(attorney);
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

    /**
     * Semi-shallow clone.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public MidPointPrincipal clone() {
        MidPointPrincipal clone = new MidPointPrincipal(this.focus);
        copyValues(clone);
        return clone;
    }

    /** Sets {@link #effectivePrivilegesModification} flag if needed. */
    public MidPointPrincipal cloneWithAdditionalAuthorizations(
            @NotNull List<Authorization> additionalAuthorizations, boolean full) {
        MidPointPrincipal clone = clone();
        additionalAuthorizations.forEach(a -> clone.addExtraAuthorizationIfMissing(a, full));
        return clone;
    }

    protected void copyValues(MidPointPrincipal clone) {
        clone.effectivePrivilegesModification = this.effectivePrivilegesModification;
        clone.applicableSecurityPolicy = this.applicableSecurityPolicy;
        clone.authorizations.get().addAll(authorizations.get());
        clone.effectiveActivationStatus = this.effectiveActivationStatus;
        clone.otherPrivilegesLimitations.get().copyValuesFrom(this.otherPrivilegesLimitations.get());
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
        DebugUtil.debugDumpWithLabelLn(sb, "Authorizations", authorizations.get(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Other privilege limitations", otherPrivilegesLimitations.get(), indent + 1);
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

    public @NotNull ObjectReferenceType toObjectReference() {
        assert focus.getOid() != null;
        return ObjectTypeUtil.createObjectRef(focus, SchemaConstants.ORG_DEFAULT);
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
    @Nullable
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

        return null;
    }

    public Locale getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(Locale preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public @NotNull OtherPrivilegesLimitations getOtherPrivilegesLimitations() {
        return otherPrivilegesLimitations.get();
    }

    /**
     * Registers an information about "membership delegation", i.e. that this principal is a delegate of given user(s)
     * or - indirectly - it obtains a delegated abstract role membership. The information on other privileges limitations
     * is attached as well.
     */
    public void addDelegationTarget(
            @NotNull PrismObject<? extends AssignmentHolderType> target,
            @NotNull OtherPrivilegesLimitations.Limitation limitation) {
        otherPrivilegesLimitations.get().addDelegationTarget(target, limitation);
    }

    /**
     * TODO (null means we don't care about limitations)
     */
    public Set<String> getDelegatorsFor(
            @Nullable OtherPrivilegesLimitations.Type limitationType) {
        return otherPrivilegesLimitations.get().getDelegatorsFor(limitationType);
    }

    /**
     * Includes the delegators themselves.
     *
     * Later we may extend this to full references (not only OIDs).
     */
    public Set<String> getDelegatedMembershipFor(
            @Nullable OtherPrivilegesLimitations.Type limitationType) {
        return otherPrivilegesLimitations.get().getDelegatedMembershipFor(limitationType);
    }

    /**
     * Clear all registered "membership delegation".
     */
    public void clearOtherPrivilegesLimitations(){
        this.otherPrivilegesLimitations.get().clear();
    }


    /**
     * Checks if the midPoint object behind this principal is enabled. The method is placed here to be easily accessible
     * from various contexts. (Although it is a bit questionable if it isn't just too late to check the object after being
     * "installed" into {@link MidPointPrincipal}.)
     *
     * We assume that the object was recomputed.
     */
    public void checkEnabled() throws SecurityViolationException {
        var effectiveStatus = FocusTypeUtil.getEffectiveStatus(focus);
        if (effectiveStatus == ActivationStatusType.DISABLED || effectiveStatus == ActivationStatusType.ARCHIVED) {
            throw new SecurityViolationException("The principal is disabled");
        }
    }
}
