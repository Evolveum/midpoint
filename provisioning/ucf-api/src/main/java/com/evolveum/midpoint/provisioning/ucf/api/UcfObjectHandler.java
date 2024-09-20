/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles UCF objects, typically coming from iterative search.
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface UcfObjectHandler extends ObjectHandler<UcfResourceObject> {

    /** Collects all objects into a list. Useful mainly for tests, but sometimes maybe for the production as well. */
    class Collecting implements UcfObjectHandler {

        @NotNull private final List<UcfResourceObject> collectedObjects = new ArrayList<>();

        @Override
        public boolean handle(UcfResourceObject object, OperationResult result) {
            collectedObjects.add(object);
            return true;
        }

        public @NotNull List<UcfResourceObject> getCollectedObjects() {
            return collectedObjects;
        }
    }
}
