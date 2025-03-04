/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemSelectorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/** TODO description + better name */
public class OtherPrivilegesLimitations implements DebugDumpable, Serializable {

    @NotNull private final Map<SimpleReference, Limitation> limitationMap = new HashMap<>();

    void copyValuesFrom(OtherPrivilegesLimitations otherPrivilegesLimitations) {
        assert limitationMap.isEmpty();
        otherPrivilegesLimitations.limitationMap.forEach(
                (key, value) -> limitationMap.put(key, value.clone()));
    }

    void addDelegationTarget(PrismObject<? extends AssignmentHolderType> target, @NotNull Limitation limitation) {
        limitationMap.compute(
                SimpleReference.of(target),
                (oid, currentLimitation) -> {
                    if (currentLimitation == null) {
                        currentLimitation = Limitation.allowingNone();
                    }
                    currentLimitation.allow(limitation);
                    return currentLimitation;
                });
    }

    public @Nullable Limitation get(@NotNull Class<?> type, @NotNull String oid) {
        return limitationMap.get(new SimpleReference(type, oid));
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "limitations", limitationMap, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "OtherPrivilegesLimitations{" +
                "limitationMap=" + limitationMap +
                '}';
    }

    @NotNull Set<String> getDelegatorsFor(@Nullable Type limitationType) {
        return limitationMap.entrySet().stream()
                .filter(e -> limitationType == null || e.getValue().allows(limitationType))
                .map(e -> e.getKey())
                .filter(ref -> ref.type.equals(UserType.class))
                .map(ref -> ref.oid)
                .collect(Collectors.toSet());
    }

    @NotNull Set<String> getDelegatedMembershipFor(@Nullable Type limitationType) {
        return limitationMap.entrySet().stream()
                .filter(e -> limitationType == null || e.getValue().allows(limitationType))
                .map(e -> e.getKey().oid)
                .collect(Collectors.toSet());
    }

    public void clear(){
        limitationMap.clear();
    }

    public enum Type {
        CASES, ACCESS_CERTIFICATION
    }

    /** Represents limitation to a given target (i.e. through a given assignment path or paths). Mutable. */
    public record Limitation(
            @NotNull Collection<Type> allowedTypes) implements ShortDumpable, Serializable, Cloneable {

        public static Limitation allowingAll() {
            return new Limitation(new HashSet<>(List.of(Type.values())));
        }

        @SuppressWarnings("WeakerAccess")
        public static Limitation allowingNone() {
            return new Limitation(new HashSet<>());
        }

        public void restrict(@Nullable OtherPrivilegesLimitationType bean) {
            if (bean == null) {
                return;
            }
            if (isDenied(bean.getCertificationWorkItems())) {
                allowedTypes.remove(Type.ACCESS_CERTIFICATION);
            }
            var cases = MiscUtil.getFirstNonNull(bean.getCaseManagementWorkItems(), bean.getApprovalWorkItems());
            if (isDenied(cases)) {
                allowedTypes.remove(Type.CASES);
            }
        }

        private boolean isDenied(WorkItemSelectorType selector) {
            return selector == null || !Boolean.TRUE.equals(selector.isAll());
        }

        @Override
        protected Limitation clone() {
            try {
                return (Limitation) super.clone();
            } catch (CloneNotSupportedException e) {
                throw SystemException.unexpected(e);
            }
        }

        @VisibleForTesting
        public Limitation allow(@NotNull Type limitationType) {
            allowedTypes.add(limitationType);
            return this;
        }

        public Limitation allow(@NotNull Limitation limitation) {
            allowedTypes.addAll(limitation.allowedTypes);
            return this;
        }

        public boolean allows(@NotNull Type limitationType) {
            return allowedTypes.contains(limitationType);
        }

        @Override
        public void shortDump(StringBuilder sb) {
            sb.append("allowed: ").append(allowedTypes);
        }

        @Override
        public String toString() {
            return "Limitation{" +
                    "allowedTypes=" + allowedTypes +
                    '}';
        }
    }

    private record SimpleReference(@NotNull Class<?> type, @NotNull String oid) implements Serializable {
        static @NotNull SimpleReference of(PrismObject<?> object) {
            return new SimpleReference(
                    object.asObjectable().getClass(),
                    MiscUtil.argNonNull(object.getOid(), "No OID of %s", object)
            );
        }
    }
}
