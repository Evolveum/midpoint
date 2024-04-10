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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestCleanupHandler extends DefaultCleanupHandler {

    private List<CleanupEvent<Item<?, ?>>> optionalCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismReference>> referenceCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismProperty<ProtectedStringType>>> protectedStringCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismContainer<MappingType>>> missingMappingNameCleanupEvents = new ArrayList<>();

    public TestCleanupHandler() {
        super(PrismTestUtil.getPrismContext());
    }

    @Override
    public boolean onConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        boolean result = super.onConfirmOptionalCleanup(event);
        optionalCleanupEvents.add(event);

        return true;
    }

    @Override
    public void onReferenceCleanup(CleanupEvent<PrismReference> event) {
        super.onReferenceCleanup(event);

        referenceCleanupEvents.add(event);
    }

    @Override
    public void onProtectedStringCleanup(CleanupEvent<PrismProperty<ProtectedStringType>> event) {
        super.onProtectedStringCleanup(event);

        protectedStringCleanupEvents.add(event);
    }

    @Override
    public void onMissingMappingNameCleanup(CleanupEvent<PrismContainer<MappingType>> event) {
        super.onMissingMappingNameCleanup(event);

        missingMappingNameCleanupEvents.add(event);
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

    public List<CleanupEvent<PrismContainer<MappingType>>> getMissingMappingNameCleanupEvents() {
        return missingMappingNameCleanupEvents;
    }
}
