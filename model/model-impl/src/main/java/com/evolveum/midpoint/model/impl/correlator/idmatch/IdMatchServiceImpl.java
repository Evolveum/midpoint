/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingResult;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class IdMatchServiceImpl implements IdMatchService {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchServiceImpl.class);

    /** URL where the service resides. */
    @NotNull private final String url;

    /** User name used to access the service. */
    @Nullable private final String username;

    /** Password used to access the service. */
    @Nullable private final ProtectedStringType password;

    private IdMatchServiceImpl(@NotNull String url, @Nullable String username, @Nullable ProtectedStringType password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result) {

        LOGGER.trace("Executing match for:\n{}", attributes.debugDumpLazily(1));

        // just a demo
        //noinspection unchecked
        PrismContainerValue<ShadowAttributesType> pcv = attributes.asPrismContainerValue();
        for (Item<?, ?> item : pcv.getItems()) {
            LOGGER.info("Got attribute {} with value(s): {}",
                    item.getElementName().getLocalPart(), item.getRealValues());
        }

        // TODO implement

        return MatchingResult.forUncertain(null, Set.of());
    }

    @Override
    public void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {

        // TODO implement
    }

    /**
     * Creates an instance of ID Match service for given ID Match correlator configuration.
     */
    public static IdMatchServiceImpl instantiate(@NotNull IdMatchCorrelatorType configuration) throws ConfigurationException {
        return new IdMatchServiceImpl(
                MiscUtil.requireNonNull(configuration.getUrl(),
                        () -> new ConfigurationException("No URL in ID Match service configuration")),
                configuration.getUsername(),
                configuration.getPassword());
    }

    /**
     * Creates an instance of ID Match service with explicitly defined parameters.
     */
    @VisibleForTesting
    public static IdMatchServiceImpl instantiate(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password) {
        return new IdMatchServiceImpl(url, username, password);
    }

    private String getPasswordCleartext() throws EncryptionException {
        if (password != null) {
            return PrismContext.get().getDefaultProtector()
                    .decryptString(password);
        } else {
            return null;
        }
    }
}
