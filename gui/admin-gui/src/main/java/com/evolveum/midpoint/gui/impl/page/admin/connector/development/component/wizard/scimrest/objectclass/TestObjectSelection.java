/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * State of the steps that let the user choose among the recorded test objects of an object class.
 */
public class TestObjectSelection implements Serializable, IDetachable {

    private static final String OP_LOAD_TEST_OBJECTS = TestObjectSelection.class.getName() + ".loadTestObjects";

    private final ConnectorDevelopmentDetailsModel detailsModel;

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    /**
     * OID of the recorded test object picked in the table.
     */
    private final IModel<String> selectedShadowOidModel = Model.of();

    /**
     * Identifier typed for the object that is looked up in the external system.
     */
    private final IModel<String> primaryIdentifierModel = Model.of();

    /**
     * Object found in the external system for the typed identifier.
     */
    private final IModel<ShadowType> foundObjectModel = Model.of();

    /**
     * Recorded test objects the user marked to keep, so they are not deleted and can be reused for testing another capability.
     */
    private final Set<String> keptObjectOids = new HashSet<>();

    /**
     * Recorded test objects of the object class that still exist in the external system, reloaded on request.
     */
    private final LoadableModel<List<ShadowType>> testObjectsModel = LoadableModel.create(this::loadTestObjects, false);

    /**
     * Chosen source of the test object; null until the user chooses, then the default is used.
     */
    private TestObjectSource objectSource;

    public TestObjectSelection(ConnectorDevelopmentDetailsModel detailsModel,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        this.detailsModel = detailsModel;
        this.valueModel = valueModel;
    }

    public String getObjectClassName() {
        return valueModel.getObject().getRealValue().getName();
    }

    public IModel<String> getSelectedShadowOidModel() {
        return selectedShadowOidModel;
    }

    public IModel<String> getPrimaryIdentifierModel() {
        return primaryIdentifierModel;
    }

    public IModel<ShadowType> getFoundObjectModel() {
        return foundObjectModel;
    }

    public LoadableModel<List<ShadowType>> getTestObjectsModel() {
        return testObjectsModel;
    }

    /**
     * Returns the chosen source of the test object, or the default one when the user has not chosen yet.
     *
     * @return previously created objects by default, or an existing object when there are none
     */
    public TestObjectSource getObjectSource() {
        if (objectSource != null) {
            return objectSource;
        }
        return testObjectsModel.getObject().isEmpty() ? TestObjectSource.EXISTING_OBJECT : TestObjectSource.PREVIOUSLY_CREATED;
    }

    public void setObjectSource(TestObjectSource objectSource) {
        this.objectSource = objectSource;
    }

    public Set<String> getKeptObjectOids() {
        return keptObjectOids;
    }

    /**
     * Tells whether the test object is marked to be kept.
     *
     * @param oid OID of the test object
     * @return true if the object is kept
     */
    public boolean isKept(String oid) {
        return keptObjectOids.contains(oid);
    }

    /**
     * Marks or unmarks the test object to be kept.
     *
     * @param oid OID of the test object
     * @param kept whether the object should be kept
     */
    public void setKept(String oid, boolean kept) {
        if (kept) {
            keptObjectOids.add(oid);
        } else {
            keptObjectOids.remove(oid);
        }
    }

    /**
     * Tells whether all loaded test objects are marked to be kept.
     *
     * @return true if there is at least one test object and all of them are kept
     */
    public boolean isAllKept() {
        return !testObjectsModel.getObject().isEmpty()
                && testObjectsModel.getObject().stream().allMatch(object -> isKept(object.getOid()));
    }

    /**
     * Marks or unmarks all loaded test objects to be kept.
     *
     * @param kept whether the objects should be kept
     */
    public void setAllKept(boolean kept) {
        if (kept) {
            testObjectsModel.getObject().forEach(object -> keptObjectOids.add(object.getOid()));
        } else {
            keptObjectOids.clear();
        }
    }

    @Override
    public void detach() {
        testObjectsModel.detach();
    }

    private List<ShadowType> loadTestObjects() {
        Task task = detailsModel.getServiceLocator().createSimpleTask(OP_LOAD_TEST_OBJECTS);
        return new ArrayList<>(detailsModel.getConnectorDevelopmentOperation()
                .getTestObjects(getObjectClassName(), task, task.getResult()));
    }
}
