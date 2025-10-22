package com.evolveum.midpoint.smart.impl.correlation;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.jetbrains.annotations.Nullable;

/**
 * The suggestion for correlation may be based on an existing inbound mapping (in which case attributeDefinitionBean is null,
 * and resourceAttrPath is present if and only if the existing mapping is "as-is" mapping; at least for now), or it may
 * involve creation of a new inbound mapping (in which case attributeDefinitionBean is non-null,
 * and resourceAttrPath is always present).
 *
 * The rationale behind this is that when scoring the suggestion, we look at the statistics of both focus item and
 * resource attribute. So we need to know the resource attribute even if it is not being suggested to be created.
 * But only if it makes sense to score it, which is currently not the case for "transforming" mappings.
 */
public record CorrelatorSuggestion(
        ItemPath focusItemPath,
        @Nullable ItemPath resourceAttrPath,
        @Nullable ResourceAttributeDefinitionType attributeDefinitionBean) {
}
