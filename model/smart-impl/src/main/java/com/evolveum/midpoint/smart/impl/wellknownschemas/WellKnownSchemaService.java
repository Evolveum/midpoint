/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchResultType;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WellKnownSchemaService {

    private static final Trace LOGGER = TraceManager.getTrace(WellKnownSchemaService.class);

    private final List<WellKnownSchemaDetector> detectors;
    private final Map<WellKnownSchemaType, WellKnownSchemaProvider> mappingProviders;

    public WellKnownSchemaService(List<WellKnownSchemaDetector> detectors, List<WellKnownSchemaProvider> mappingProviders) {
        this.detectors = detectors;
        this.mappingProviders = mappingProviders.stream()
                .collect(Collectors.toMap(
                        WellKnownSchemaProvider::getSupportedSchemaType,
                        provider -> provider));
    }

    /**
     * Attempts to detect known schema type for the given resource and object type.
     * Returns the detection with highest confidence if multiple schemas match.
     */
    public Optional<WellKnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {

        for (WellKnownSchemaDetector detector : detectors) {
            try {
                Optional<WellKnownSchemaType> result = detector.detectSchemaType(resource, typeDefinition);
                if (result.isPresent()) {
                    LOGGER.debug("Detected known schema: {}", result.get());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.debug("Error during schema detection with {}: {}",
                        detector.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        LOGGER.trace("No known schema detected");
        return Optional.empty();
    }

    public Optional<WellKnownSchemaProvider> getProvider(WellKnownSchemaType schemaType) {
        return Optional.ofNullable(mappingProviders.get(schemaType));
    }

    public Optional<WellKnownSchemaProvider> getProviderFromSchemaMatch(SchemaMatchResultType schemaMatch) {
        String knownSchemaTypeStr = schemaMatch.getWellKnownSchemaType();
        if (knownSchemaTypeStr == null) {
            LOGGER.trace("No known schema type in schema match result.");
            return Optional.empty();
        }

        try {
            WellKnownSchemaType wellKnownSchemaType = com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.valueOf(knownSchemaTypeStr);
            return getProvider(wellKnownSchemaType);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown schema type string: {}. Ignoring.", knownSchemaTypeStr);
            return Optional.empty();
        }
    }

}
