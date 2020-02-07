/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNull;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueSetAsserter;

/**
 * @author semancik
 *
 */
public class ContainerDeltaAsserter<C extends Containerable,RA> extends AbstractAsserter<RA> {

    private ContainerDelta<C> containerDelta;

    public ContainerDeltaAsserter(ContainerDelta<C> containerDelta) {
        super();
        this.containerDelta = containerDelta;
    }

    public ContainerDeltaAsserter(ContainerDelta<C> containerDelta, String detail) {
        super(detail);
        this.containerDelta = containerDelta;
    }

    public ContainerDeltaAsserter(ContainerDelta<C> containerDelta, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.containerDelta = containerDelta;
    }

    public static <C extends Containerable> ContainerDeltaAsserter<C,Void> forDelta(ContainerDelta<C> containerDelta) {
        return new ContainerDeltaAsserter<>(containerDelta);
    }

    public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToAdd() {
        Collection<PrismContainerValue<C>> valuesToAdd = containerDelta.getValuesToAdd();
        PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
                valuesToAdd, this, "values to add in "+desc());
        copySetupTo(setAsserter);
        return setAsserter;
    }

    public ContainerDeltaAsserter<C,RA> assertNoValuesToAdd() {
        Collection<PrismContainerValue<C>> valuesToAdd = containerDelta.getValuesToAdd();
        assertNull("Unexpected values to add in "+desc()+": "+valuesToAdd, valuesToAdd);
        return this;
    }

    public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToDelete() {
        Collection<PrismContainerValue<C>> valuesToDelete = containerDelta.getValuesToDelete();
        PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
                valuesToDelete, this, "values to delte in "+desc());
        copySetupTo(setAsserter);
        return setAsserter;
    }

    public ContainerDeltaAsserter<C,RA> assertNoValuesToDelete() {
        Collection<PrismContainerValue<C>> valuesToDelete = containerDelta.getValuesToDelete();
        assertNull("Unexpected values to delete in "+desc()+": "+valuesToDelete, valuesToDelete);
        return this;
    }

    public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToReplace() {
        Collection<PrismContainerValue<C>> valuesToReplace = containerDelta.getValuesToReplace();
        PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
                valuesToReplace, this, "values to replace in "+desc());
        copySetupTo(setAsserter);
        return setAsserter;
    }

    public ContainerDeltaAsserter<C,RA> assertNoValuesToReplace() {
        Collection<PrismContainerValue<C>> valuesToReplace = containerDelta.getValuesToReplace();
        assertNull("Unexpected values to replace in "+desc()+": "+valuesToReplace, valuesToReplace);
        return this;
    }

    protected String desc() {
        return descWithDetails(containerDelta);
    }

}
