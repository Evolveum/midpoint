/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * State of the test object shared by the steps of a capability test.
 */
public class TestObjectFlowState implements Serializable {

    /**
     * Wrapper of the currently tested object, holding the attribute values entered by the user.
     */
    private final IModel<ShadowWrapper> testedObjectModel = Model.of();

    /**
     * Source of the tested object chosen by the user; null when the test does not choose among sources.
     */
    private TestObjectSource testedObjectSource;

    /**
     * Tested object as read back from the external system after the operation.
     */
    private final IModel<ShadowType> retrievedObjectModel = Model.of();

    /**
     * Tested object as it was before the Delete test removed it from the external system.
     */
    private final IModel<ShadowType> deletedObjectModel = Model.of();

    public IModel<ShadowWrapper> getTestedObjectModel() {
        return testedObjectModel;
    }

    public TestObjectSource getTestedObjectSource() {
        return testedObjectSource;
    }

    public IModel<ShadowType> getRetrievedObjectModel() {
        return retrievedObjectModel;
    }

    public IModel<ShadowType> getDeletedObjectModel() {
        return deletedObjectModel;
    }

    /**
     * Sets the tested object and drops the results of the test of the previous one.
     *
     * @param testedObject wrapper of the tested object
     * @param source source of the tested object
     */
    public void objectSelected(ShadowWrapper testedObject, TestObjectSource source) {
        testedObjectModel.setObject(testedObject);
        testedObjectSource = source;
        retrievedObjectModel.setObject(null);
        deletedObjectModel.setObject(null);
    }

    /**
     * Forgets the tested object together with everything derived from it.
     */
    public void objectDiscarded() {
        objectSelected(null, null);
    }

    /**
     * Forgets the tested object and remembers it as deleted by the Delete test.
     *
     * @param deletedObject the tested object before its deletion
     */
    public void objectDeleted(ShadowType deletedObject) {
        objectDiscarded();
        deletedObjectModel.setObject(deletedObject);
    }
}
