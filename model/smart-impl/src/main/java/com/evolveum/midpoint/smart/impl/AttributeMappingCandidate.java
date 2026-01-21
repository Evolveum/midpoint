/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeMappingsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Wrapper record around {@link AttributeMappingsSuggestionType} providing utility methods
 * for comparison, duplicate detection, and path extraction.
 */
record AttributeMappingCandidate(AttributeMappingsSuggestionType suggestion) {

    ItemPath getShadowAttributePath() {
        var definition = suggestion.getDefinition();
        if (definition == null || definition.getRef() == null) {
            return null;
        }
        Object ref = definition.getRef();
        return ref instanceof ItemPathType path ? path.getItemPath() : null;
    }

    ItemPath getFocusPropertyPath() {
        var definition = suggestion.getDefinition();
        if (definition == null) {
            return null;
        }
        List<InboundMappingType> inbounds = definition.getInbound();
        if (inbounds != null && !inbounds.isEmpty()) {
            var inbound = inbounds.get(0);
            if (inbound.getTarget() != null && inbound.getTarget().getPath() != null) {
                Object path = inbound.getTarget().getPath();
                if (path instanceof ItemPathType itemPath) {
                    return itemPath.getItemPath();
                }
            }
        }
        var outbound = definition.getOutbound();
        if (outbound != null && outbound.getSource() != null && !outbound.getSource().isEmpty()) {
            var source = outbound.getSource().get(0);
            if (source.getPath() != null) {
                Object path = source.getPath();
                if (path instanceof ItemPath itemPath) {
                    return itemPath;
                }
            }
        }
        return null;
    }

    float getQuality() {
        Float quality = suggestion.getExpectedQuality();
        return quality != null ? quality : 0.0f;
    }

    boolean isDuplicateOf(AttributeMappingCandidate other) {
        ItemPath thisShadowPath = getShadowAttributePath();
        ItemPath thisFocusPath = getFocusPropertyPath();
        ItemPath otherShadowPath = other.getShadowAttributePath();
        ItemPath otherFocusPath = other.getFocusPropertyPath();
        if (thisShadowPath == null || thisFocusPath == null || otherShadowPath == null || otherFocusPath == null) {
            return false;
        }
        return thisShadowPath.equivalent(otherShadowPath) && thisFocusPath.equivalent(otherFocusPath);
    }

    boolean isPreferredOver(AttributeMappingCandidate other) {
        float thisQuality = getQuality();
        float otherQuality = other.getQuality();
        if (thisQuality > otherQuality) {
            return true;
        }
        if (thisQuality < otherQuality) {
            return false;
        }
        return SmartMetadataUtil.isMarkedAsSystemProvided(suggestion.asPrismContainerValue());
    }

}
