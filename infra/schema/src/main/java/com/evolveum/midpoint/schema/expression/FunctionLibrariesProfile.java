/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibrariesProfileType;

/**
 * Limitations on calling individual function libraries. Parsed form of.
 */
public class FunctionLibrariesProfile extends AbstractSecurityProfile {

    /** Profiles for individual function libraries, keyed by library OID. Unmodifiable. */
    @NotNull private final Map<String, FunctionLibraryProfile> libraryProfileMap;

    /** "Allow all" profile. */
    private static final FunctionLibrariesProfile FULL = new FunctionLibrariesProfile(
            SchemaConstants.FULL_EXPRESSION_PROFILE_ID,
            AccessDecision.ALLOW,
            Map.of());

    /** "Allow nothing" profile. */
    private static final FunctionLibrariesProfile NONE = new FunctionLibrariesProfile(
            SchemaConstants.NONE_EXPRESSION_PROFILE_ID,
            AccessDecision.DENY,
            Map.of());

    private FunctionLibrariesProfile(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            @NotNull Map<String, FunctionLibraryProfile> libraryProfileMap) {
        super(identifier, defaultDecision);
        this.libraryProfileMap = libraryProfileMap;
    }

    public static @NotNull FunctionLibrariesProfile full() {
        return FULL;
    }

    public static @NotNull FunctionLibrariesProfile none() {
        return NONE;
    }

    public static FunctionLibrariesProfile of(@NotNull FunctionLibrariesProfileType bean) throws ConfigurationException {
        String identifier = MiscUtil.configNonNull(bean.getIdentifier(), "No identifier in libraries profile %s", bean);
        Map<String, FunctionLibraryProfile> libraryProfileMap = new HashMap<>();
        for (var libraryBean : bean.getLibrary()) {
            var actionProfile = FunctionLibraryProfile.of(libraryBean);
            libraryProfileMap.put(actionProfile.libraryOid(), actionProfile);
        }
        return new FunctionLibrariesProfile(
                identifier,
                AccessDecision.translate(
                        MiscUtil.configNonNull(
                                bean.getDecision(), "No decision in libraries profile %s", bean.getIdentifier())),
                Collections.unmodifiableMap(libraryProfileMap));
    }
}
