/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.api.info;

/** AI provider and model information returned by the microservice health endpoint. */
public record AiInfo(String provider, String model) {
}
