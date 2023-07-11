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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.validator.ObjectUpgradeValidator;
import com.evolveum.midpoint.schema.validator.UpgradeValidationItem;
import com.evolveum.midpoint.schema.validator.UpgradeValidationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles upgrade of single object, filters out items that are not applicable for upgrade based on options selected by user.
 */
public class UpgradeObjectHandler {

    private UpgradeObjectsOptions options;

    private NinjaContext context;

    public UpgradeObjectHandler(UpgradeObjectsOptions options, NinjaContext context) {
        this.options = options;
        this.context = context;
    }

    /**
     * Filters out items that are not applicable for upgrade, applies delta to object.
     *
     * @param object
     * @param <O>
     * @return true if object was changed
     * @throws Exception
     */
    public <O extends ObjectType> boolean execute(PrismObject<O> object) {
        final PrismContext prismContext = context.getPrismContext();

        ObjectUpgradeValidator validator = new ObjectUpgradeValidator(prismContext);
        validator.showAllWarnings();
        UpgradeValidationResult result = validator.validate(object);
        if (!result.hasChanges()) {
            return false;
        }

        List<UpgradeValidationItem> applicableItems = filterApplicableItems(result.getItems());
        if (applicableItems.isEmpty()) {
            return false;
        }

        applicableItems.forEach(item -> {
            try {
                ObjectDelta delta = item.getDelta();
                if (!delta.isEmpty()) {
                    delta.applyTo(object);
                }
            } catch (SchemaException ex) {
                // todo error handling
                ex.printStackTrace();
            }
        });

        return true;
    }

    private List<UpgradeValidationItem> filterApplicableItems(List<UpgradeValidationItem> items) {
        return items.stream().filter(item -> {
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

            return matchesOption(options.getPriorities(), item.getPriority());
        }).collect(Collectors.toList());
    }

    private <T> boolean matchesOption(List<T> options, T option) {
        if (options == null || options.isEmpty()) {
            return true;
        }

        return options.stream().anyMatch(o -> o.equals(option));
    }
}
