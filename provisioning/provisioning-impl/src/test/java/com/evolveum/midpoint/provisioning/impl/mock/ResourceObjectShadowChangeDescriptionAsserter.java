/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.mock;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ResourceObjectShadowChangeDescriptionAsserter {

    private ResourceObjectShadowChangeDescription changeDesc;

    public ResourceObjectShadowChangeDescriptionAsserter(ResourceObjectShadowChangeDescription changeDesc) {
        this.changeDesc = changeDesc;
    }

    public ResourceObjectShadowChangeDescriptionAsserter display() {
        PrismTestUtil.display("Change notification", changeDesc);
        return this;
    }

    public ObjectDeltaAsserter<ShadowType,ResourceObjectShadowChangeDescriptionAsserter> delta() {
        ObjectDelta<ShadowType> objectDelta = changeDesc.getObjectDelta();
        assertNotNull("No object delta in change notification", objectDelta);
        return new ObjectDeltaAsserter<>(objectDelta, this, "object delta in change notification");
    }

    public ResourceObjectShadowChangeDescriptionAsserter assertNoDelta() {
        assertNull("Unexpected object delta in change notificaiton", changeDesc.getObjectDelta());
        return this;
    }

    public ShadowAsserter<ResourceObjectShadowChangeDescriptionAsserter> currentShadow() {
        PrismObject<ShadowType> currentShadow = changeDesc.getShadowedResourceObject();
        assertNotNull("No current shadow in change notification", currentShadow);
        return new ShadowAsserter<>(currentShadow, this, "currentShadow in change notification");
    }

    public ResourceObjectShadowChangeDescriptionAsserter assertProtected(boolean expected) {
        assertEquals("Wrong protected flag in change notification", expected, changeDesc.isProtected());
        return this;
    }

}
