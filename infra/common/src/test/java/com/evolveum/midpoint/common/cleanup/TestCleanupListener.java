/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestCleanupListener implements CleanupHandler {

    private List<CleanupEvent<Item<?, ?>>> optionalCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismReference>> referenceCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismProperty<ProtectedStringType>>> protectedStringCleanupEvents = new ArrayList<>();

    @Override
    public boolean onConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        optionalCleanupEvents.add(event);
        return true;
    }

    @Override
    public void onReferenceCleanup(CleanupEvent<PrismReference> event) {
        referenceCleanupEvents.add(event);
    }

    @Override
    public void onProtectedStringCleanup(CleanupEvent<PrismProperty<ProtectedStringType>> event) {
        protectedStringCleanupEvents.add(event);
    }

    public List<CleanupEvent<Item<?, ?>>> getOptionalCleanupEvents() {
        return optionalCleanupEvents;
    }

    public List<CleanupEvent<PrismReference>> getReferenceCleanupEvents() {
        return referenceCleanupEvents;
    }

    public List<CleanupEvent<PrismProperty<ProtectedStringType>>> getProtectedStringCleanupEvents() {
        return protectedStringCleanupEvents;
    }
}
