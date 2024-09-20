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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

public class TestCleanupListener extends DefaultCleanupListener {

    private List<CleanupEvent<Item<?, ?>>> optionalCleanupEvents = new ArrayList<>();

    private List<CleanupEvent<PrismReference>> referenceCleanupEvents = new ArrayList<>();

    public TestCleanupListener() {
        super(PrismTestUtil.getPrismContext());
    }

    @Override
    protected PrismObject<ConnectorType> resolveConnector(String oid) {
        ConnectorType connector = new ConnectorType();
        connector.setOid(oid);
        connector.setConnectorType("testconnector");
        connector.setConnectorBundle("testconnectorbundle");
        connector.setConnectorVersion("99.0");

        return connector.asPrismObject();
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

    public List<CleanupEvent<Item<?, ?>>> getOptionalCleanupEvents() {
        return optionalCleanupEvents;
    }

    public List<CleanupEvent<PrismReference>> getReferenceCleanupEvents() {
        return referenceCleanupEvents;
    }
}
