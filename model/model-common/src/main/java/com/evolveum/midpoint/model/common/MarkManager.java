/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;

/**
 * Manages {@link MarkType} objects.
 *
 * Currently does not provide its own cache, but relies on the repository cache instead.
 * (Assumes that the global caching of {@link MarkType} objects and queries is enabled.)
 * In the future we may consider adding caching capabilities here, if needed.
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // temporary
@Component
public class MarkManager {

    private static final Trace LOGGER = TraceManager.getTrace(MarkManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

    /** Gets a tag by OID. */
    public @NotNull MarkType getTag(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (!cacheRepositoryService.supportsMarks()) {
            throw new UnsupportedOperationException("The repository does not support tag objects");
        }
        var options = GetOperationOptionsBuilder.create()
                .readOnly()
                .build();
        return cacheRepositoryService
                .getObject(MarkType.class, oid, options, result)
                .asObjectable();
    }

    /** Gets a tag by OID (if exists). */
    public @Nullable MarkType getTagIfExists(String oid, OperationResult result)
            throws SchemaException {
        if (!cacheRepositoryService.supportsMarks()) {
            return null;
        }
        var options = GetOperationOptionsBuilder.create()
                .allowNotFound()
                .readOnly()
                .build();
        try {
            return cacheRepositoryService
                    .getObject(MarkType.class, oid, options, result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets a tag by URI. Note that we currently have no repo constraint disallowing multiple tags with the same URI,
     * but check that as a configuration exception. This should be perhaps changed and enforced on DB level.
     */
    public @Nullable MarkType getTagByUri(@NotNull String uri, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (!cacheRepositoryService.supportsMarks()) {
            return null;
        }
        var tags = cacheRepositoryService.searchObjects(
                MarkType.class,
                prismContext.queryFor(MarkType.class)
                        .item(MarkType.F_URI).eq(uri)
                        .build(),
                null,
                result);
        LOGGER.trace("Tag(s) by URI '{}': {}", uri, tags);
        return asObjectable(
                MiscUtil.extractSingleton(
                        tags,
                        () -> new ConfigurationException("Multiple tags with URI of '" + uri + "': " + tags)));
    }

    public @NotNull Collection<MarkType> getAllMarks(OperationResult result) {
        if (!cacheRepositoryService.supportsMarks()) {
            return List.of();
        }
        try {
            return asObjectables(
                    cacheRepositoryService.searchObjects(MarkType.class, null, null, result));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when retrieving all tags");
        }
    }

    public @NotNull Collection<MarkType> getAllEventMarks(OperationResult result) {
        return getAllMarks(result).stream()
                .filter(tag -> ObjectTypeUtil.hasArchetypeRef(tag, SystemObjectsType.ARCHETYPE_EVENT_MARK.value()))
                .collect(Collectors.toList());
    }

    /**
     * Collects all (global) policy rules from all marks. Adding the `markRef` in case it does not include a reference to the
     * current mark.
     */
    public @NotNull Collection<GlobalRuleWithId> getAllMarkPolicyRules(OperationResult result) {
        List<GlobalRuleWithId> rules = new ArrayList<>();
        for (MarkType mark : getAllMarks(result)) {
            for (GlobalPolicyRuleType rule : mark.getPolicyRule()) {
                if (!Referencable.getOids(rule.getMarkRef()).contains(mark.getOid())) {
                    rule = rule.clone();
                    rule.getMarkRef().add(
                            ObjectTypeUtil.createObjectRef(mark));
                }
                rules.add(
                        GlobalRuleWithId.of(rule, mark.getOid()));
            }
        }
        return rules;
    }

    /** Temporary */
    public Map<String, MarkType> resolveTagNames(Collection<String> tagOids, OperationResult result) {
        Map<String, MarkType> map = new HashMap<>();
        for (String tagOid : tagOids) {
            MarkType tag = null;
            try {
                tag = getTagIfExists(tagOid, result);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve tag {} (ignoring)", e, tagOid);
            }
            map.put(tagOid, tag);
        }
        return map;
    }
}
