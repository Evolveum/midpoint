/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.api.info;

/**
 * AI provider and model information returned by the microservice health endpoint.
 *
 * This record contains metadata about the AI service being used by the Smart Integration
 * microservice, including the provider name, model identifier, and operational status.
 *
 * provider: The name of the AI provider.
 * model: The specific AI model being used.
 * status: The operational status of the AI provider. Expected values are:
 *     - "OK" - The AI provider is available and functioning normally
 *     - "ERROR" - The AI provider is unavailable or experiencing issues
 */
public record AiInfo(String provider, String model, String status) {
}
