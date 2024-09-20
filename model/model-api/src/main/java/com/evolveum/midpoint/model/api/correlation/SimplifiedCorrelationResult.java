/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

import com.evolveum.midpoint.model.api.correlator.CandidateOwner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;

import java.util.Collection;

/**
 * Result of a sub-object correlation.
 *
 * TEMPORARY, just PoC for now
 */
public class SimplifiedCorrelationResult extends AbstractCorrelationResult<Containerable> {

    @Nullable private final Collection<CandidateOwner> uncertainOwners;

    private SimplifiedCorrelationResult(
            @NotNull CorrelationSituationType situation,
            @Nullable Containerable owner,
            @Nullable Collection<CandidateOwner> uncertainOwners) {
        super(situation, owner);
        this.uncertainOwners = uncertainOwners;
    }

    public static SimplifiedCorrelationResult existingOwner(@NotNull Containerable owner) {
        return new SimplifiedCorrelationResult(EXISTING_OWNER, owner, null);
    }

    public static SimplifiedCorrelationResult noOwner() {
        return new SimplifiedCorrelationResult(NO_OWNER, null, null);
    }

    public static SimplifiedCorrelationResult uncertain(Collection<CandidateOwner> eligibleCandidates) {
        return new SimplifiedCorrelationResult(UNCERTAIN, null, eligibleCandidates);
    }

    public boolean isUncertain() {
        return situation == UNCERTAIN;
    }

    public boolean isError() {
        return situation == ERROR;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isExistingOwner() {
        return situation == EXISTING_OWNER;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isNoOwner() {
        return situation == NO_OWNER;
    }

    public boolean isDone() {
        return isExistingOwner() || isNoOwner();
    }

    public @Nullable Collection<CandidateOwner> getUncertainOwners() {
        return uncertainOwners;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "situation", situation, indent + 1);
        if (owner != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "owner", String.valueOf(owner), indent + 1);
        }
        if (uncertainOwners != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "uncertainOwners", uncertainOwners, indent + 1);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "situation=" + situation +
                ", owner=" + owner +
                ", uncertain owners=" + uncertainOwners +
                '}';
    }
}
