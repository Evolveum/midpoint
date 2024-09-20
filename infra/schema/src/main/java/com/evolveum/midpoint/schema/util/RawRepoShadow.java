/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.ShortDumpable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.util.Collection;
import java.util.List;

/**
 * A shadow that was fetched from the repository OR that is going to be (or was) added to the repository.
 *
 * In both cases, the simple attributes are meant to be "in the repository format", i.e. normalized.
 * Reference attributes are placed in `referenceAttributes` container, and are guaranteed to have OIDs.
 *
 * Moreover, in the former case, it can be in a not-very-consistent state,
 * for example, with obsolete (or no) attribute definitions.
 *
 * The primary use of this object is to facilitate updating of the shadow in the repository.
 * Then it can be converted into `RepoShadow` ({@link AbstractShadow}) that is more consistent and generally useful.
 *
 * As far as the attributes are concerned, they are stored in {@link #bean} as they were obtained from the repository
 * (in non-raw mode). Concerning their definitions, they may be:
 *
 * - raw (no definition), if coming from 4.8 and earlier version of midPoint,
 * - with dynamic definitions, coming from xsi:type declarations in the full object in repository,
 * - with dynamic definitions, coming from the "ext item dictionary" for index-only attributes (if requested to be read).
 *
 * FIXME update the above description
 *
 * NOTE: The "raw" does not mean that the shadow was fetched from the repository in raw mode.
 * (Funny enough, if the raw mode was used, the attributes would get their estimated definitions even in 4.8.)
 */
@Experimental
public class RawRepoShadow implements DebugDumpable, ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(RawRepoShadow.class);

    @NotNull private final ShadowType bean;

    private RawRepoShadow(@NotNull ShadowType bean) {
        this.bean = bean;
    }

    public static @Nullable RawRepoShadow selectLiveShadow(@NotNull List<PrismObject<ShadowType>> shadows, Object context) {

        List<PrismObject<ShadowType>> liveShadows = shadows.stream()
                .filter(ShadowUtil::isNotDead)
                .toList();

        if (liveShadows.isEmpty()) {
            return null;
        } else if (liveShadows.size() > 1) {
            LOGGER.error("More than one live shadow found ({} out of {}) {}\n{}",
                    liveShadows.size(), shadows.size(), context, DebugUtil.debugDumpLazily(shadows, 1));
            // TODO: handle "more than one shadow" case for conflicting shadows - MID-4490
            throw new IllegalStateException(
                    "Found more than one live shadow %s: %s".formatted(
                            context, MiscUtil.getDiagInfo(liveShadows, 10, 1000)));
        } else {
            return of(liveShadows.get(0));
        }
    }

    public static @NotNull RawRepoShadow of(@NotNull ShadowType bean) {
        return new RawRepoShadow(bean);
    }

    public static @NotNull RawRepoShadow of(@NotNull PrismObject<ShadowType> shadowObject) {
        return of(shadowObject.asObjectable());
    }

//    public static @Nullable RawRepoShadow selectSingleShadow(
//            @NotNull ProvisioningContext ctx, @NotNull List<PrismObject<ShadowType>> shadows, Object context)
//            throws SchemaException, ConfigurationException {
//        LOGGER.trace("Selecting from {} objects", shadows.size());
//
//        if (shadows.isEmpty()) {
//            return null;
//        } else if (shadows.size() > 1) {
//            LOGGER.error("Too many shadows ({}) for {}", shadows.size(), context);
//            LOGGER.debug("Shadows:\n{}", DebugUtil.debugDumpLazily(shadows));
//            throw new IllegalStateException("More than one shadow for " + context);
//        } else {
//            return RawRepoShadow.of(shadows.get(0));
//        }
//    }

//    public static @Nullable RawRepoShadow selectLiveShadow(
//            @NotNull ProvisioningContext ctx,
//            @NotNull List<PrismObject<ShadowType>> shadows,
//            Object context) throws SchemaException, ConfigurationException {
//        if (shadows.isEmpty()) {
//            return null;
//        }
//
//        List<PrismObject<ShadowType>> liveShadows = shadows.stream()
//                .filter(ShadowUtil::isNotDead)
//                .toList();
//
//        if (liveShadows.isEmpty()) {
//            return null;
//        } else if (liveShadows.size() > 1) {
//            LOGGER.error("More than one live shadow found ({} out of {}) {}\n{}",
//                    liveShadows.size(), shadows.size(), context, DebugUtil.debugDumpLazily(shadows, 1));
//            // TODO: handle "more than one shadow" case for conflicting shadows - MID-4490
//            throw new IllegalStateException(
//                    "Found more than one live shadow %s: %s".formatted(
//                            context, MiscUtil.getDiagInfo(liveShadows, 10, 1000)));
//        } else {
//            return ctx.adoptRepoShadow(liveShadows.get(0));
//        }
//    }

    @Nullable
    public static ShadowType getBean(@Nullable RawRepoShadow repoShadow) {
        return repoShadow != null ? repoShadow.getBean() : null;
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull String getOid() {
        return MiscUtil.stateNonNull(bean.getOid(), "No OID in %s", bean);
    }

    public static String getOid(@Nullable RawRepoShadow repoShadow) {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public RawRepoShadow clone() {
        return new RawRepoShadow(bean.clone());
    }

    // TODO label
    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    @Override
    public String toString() {
        return "%s (raw repo shadow)".formatted(bean);
    }

    public PolyStringType getName() {
        return bean.getName();
    }

    public @NotNull PrismObject<ShadowType> getPrismObject() {
        return bean.asPrismObject();
    }

    public void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(getBean()));
    }

    public @Nullable String getResourceOid() {
        return ShadowUtil.getResourceOid(bean);
    }

    public @NotNull String getResourceOidRequired() {
        return MiscUtil.stateNonNull(
                getResourceOid(),
                "No resource OID in %s", this);
    }

    public Collection<Item<?, ?>> getSimpleAttributes() {
        var container = getPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        return container != null ? List.copyOf(container.getValue().getItems()) : List.of();
    }

    public Collection<Item<?, ?>> getReferenceAttributes() {
        var container = getPrismObject().findContainer(ShadowType.F_REFERENCE_ATTRIBUTES);
        return container != null ? List.copyOf(container.getValue().getItems()) : List.of();
    }
}
