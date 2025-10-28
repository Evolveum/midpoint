package com.evolveum.midpoint.smart.impl.correlation;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.Nullable;

/**
 * Information about whether inbound mapping for given correlator exists. The scoredAttributePath is present if it
 * makes sense to evaluate the correlation score also by looking at values of that attribute.
 */
public record ExistingMapping(@Nullable ItemPath scoredAttributePath) {
}
