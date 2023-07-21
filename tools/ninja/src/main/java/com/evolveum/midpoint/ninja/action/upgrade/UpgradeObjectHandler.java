/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.upgrade.action.UpgradeObjectsOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles upgrade of single object, filters out items that are not applicable for upgrade based on options selected by user.
 */
public class UpgradeObjectHandler {

    private final UpgradeObjectsOptions options;

    private final NinjaContext context;

    private final Map<UUID, Set<SkipUpgradeItem>> skipUpgradeItems;

    public UpgradeObjectHandler(
            UpgradeObjectsOptions options, NinjaContext context, Map<UUID, Set<SkipUpgradeItem>> skipUpgradeItems) {

        this.options = options;
        this.context = context;
        this.skipUpgradeItems = skipUpgradeItems;
    }

    /**
     * Filters out items that are not applicable for upgrade, applies delta to object.
     *
     * @param object to upgrade
     * @param <O> type of object
     * @return true if object was changed
     */
    public <O extends ObjectType> boolean execute(PrismObject<O> object) throws Exception {
        final PrismContext prismContext = context.getPrismContext();

        ObjectUpgradeValidator validator = new ObjectUpgradeValidator(prismContext);
        validator.showAllWarnings();
        UpgradeValidationResult result = validator.validate(object);
        if (!result.hasChanges()) {
            return false;
        }

        List<UpgradeValidationItem> applicableItems = filterApplicableItems(object.getOid(), result.getItems());
        if (applicableItems.isEmpty()) {
            return false;
        }

        // applicable items can't be applied by using delta from each item on object - deltas might
        // collide and replace changes from other items - we use upgrade processor to apply change
        // directly on to object for each item iteratively
        for (UpgradeValidationItem item : applicableItems) {
            String identifier = item.getIdentifier();
            if (identifier == null) {
                continue;
            }

            ItemPath path = item.getItem().getItemPath();

            UpgradeObjectProcessor<O> processor = UpgradeProcessor.getProcessor(identifier);
            if (processor == null) {
                continue;
            }

            processor.process(object, path);
        }

        return true;
    }

    private List<UpgradeValidationItem> filterApplicableItems(String oid, List<UpgradeValidationItem> items) {
        return items.stream()
                .filter(item -> {
                    if (!item.isChanged()) {
                        return false;
                    }

                    if (!matchesOption(options.getIdentifiers(), item.getIdentifier())) {
                        return false;
                    }

                    if (!matchesOption(options.getTypes(), item.getType())) {
                        return false;
                    }

                    if (!matchesOption(options.getPhases(), item.getPhase())) {
                        return false;
                    }

                    if (!matchesOption(options.getPriorities(), item.getPriority())) {
                        return false;
                    }

                    ItemPath path = item.getItem().getItemPath();
                    if (path == null) {
                        return true;
                    }

                    Set<SkipUpgradeItem> skipItems = skipUpgradeItems.getOrDefault(UUID.fromString(oid), new HashSet<>());
                    for (SkipUpgradeItem skipItem : skipItems) {
                        if (Objects.equals(skipItem.getPath(), path.toString())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(Comparator.comparing(UpgradeValidationItem::getIdentifier))
                .collect(Collectors.toList());
    }

    private <T> boolean matchesOption(List<T> options, T option) {
        if (options == null || options.isEmpty()) {
            return true;
        }

        return options.stream().anyMatch(o -> o.equals(option));
    }
}
