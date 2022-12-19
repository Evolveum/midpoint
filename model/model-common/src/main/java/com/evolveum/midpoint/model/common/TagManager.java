/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.Referencable;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;

/**
 * Manages {@link TagType} objects.
 *
 * Currently does not provide its own cache, but relies on the repository cache instead.
 * (Assumes that the global caching of {@link TagType} objects and queries is enabled.)
 * In the future we may consider adding caching capabilities here, if needed.
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // temporary
@Component
public class TagManager {

    private static final Trace LOGGER = TraceManager.getTrace(TagManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

    /** Gets a tag by OID. */
    public @NotNull TagType getTag(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return cacheRepositoryService
                .getObject(TagType.class, oid, null, result)
                .asObjectable();
    }

    /**
     * Gets a tag by URI. Note that we currently have no repo constraint disallowing multiple tags with the same URI,
     * but check that as a configuration exception. This should be perhaps changed and enforced on DB level.
     */
    public @Nullable TagType getTagByUri(@NotNull String uri, OperationResult result)
            throws SchemaException, ConfigurationException {
        var tags = cacheRepositoryService.searchObjects(
                TagType.class,
                prismContext.queryFor(TagType.class)
                        .item(TagType.F_URI).eq(uri)
                        .build(),
                null,
                result);
        LOGGER.trace("Tag(s) by URI '{}': {}", uri, tags);
        return asObjectable(
                MiscUtil.extractSingleton(
                        tags,
                        () -> new ConfigurationException("Multiple tags with URI of '" + uri + "': " + tags)));
    }

    public @NotNull Collection<TagType> getAllTags(OperationResult result) {
        try {
            return asObjectables(
                    cacheRepositoryService.searchObjects(TagType.class, null, null, result));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when retrieving all tags");
        }
    }

    /**
     * Collects all (global) policy rules from all tags. Adding the `tagRef` in case it does not include a reference to the
     * current tag.
     */
    public @NotNull Collection<GlobalRuleWithId> getAllTagPolicyRules(OperationResult result) {
        List<GlobalRuleWithId> rules = new ArrayList<>();
        for (TagType tag : getAllTags(result)) {
            for (GlobalPolicyRuleType rule : tag.getPolicyRule()) {
                if (!Referencable.getOids(rule.getTagRef()).contains(tag.getOid())) {
                    rule = rule.clone();
                    rule.getTagRef().add(
                            ObjectTypeUtil.createObjectRef(tag));
                }
                rules.add(
                        GlobalRuleWithId.of(rule, tag.getOid()));
            }
        }
        return rules;
    }
}
