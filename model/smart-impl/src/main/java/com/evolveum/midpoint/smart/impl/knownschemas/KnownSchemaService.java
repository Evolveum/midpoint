/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KnownSchemaService {

    private static final Trace LOGGER = TraceManager.getTrace(KnownSchemaService.class);

    private final List<KnownSchemaDetector> detectors;
    private final Map<KnownSchemaType, KnownSchemaMappingProvider> mappingProviders;

    public KnownSchemaService(List<KnownSchemaDetector> detectors, List<KnownSchemaMappingProvider> mappingProviders) {
        this.detectors = detectors;
        this.mappingProviders = mappingProviders.stream()
                .collect(Collectors.toMap(
                        KnownSchemaMappingProvider::getSupportedSchemaType,
                        provider -> provider));
    }

    /**
     * Attempts to detect known schema type for the given resource and object type.
     * Returns the detection with highest confidence if multiple schemas match.
     */
    public Optional<KnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {

        for (KnownSchemaDetector detector : detectors) {
            try {
                Optional<KnownSchemaType> result = detector.detectSchemaType(resource, typeDefinition);
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

    public Optional<KnownSchemaMappingProvider> getProvider(KnownSchemaType schemaType) {
        return Optional.ofNullable(mappingProviders.get(schemaType));
    }

}
