/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class ShadowAssociationsAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {

    private PrismContainer<ShadowAssociationType> associationContainer;
    private ShadowAsserter<R> shadowAsserter;

    public ShadowAssociationsAsserter(ShadowAsserter<R> shadowAsserter) {
        super();
        this.shadowAsserter = shadowAsserter;
    }

    public ShadowAssociationsAsserter(ShadowAsserter<R> shadowAsserter, String details) {
        super(details);
        this.shadowAsserter = shadowAsserter;
    }

    private PrismObject<ShadowType> getShadow() {
        return shadowAsserter.getObject();
    }

    private PrismContainer<ShadowAssociationType> getAssociationContainer() {
        if (associationContainer == null) {
            associationContainer = getShadow().findContainer(ShadowType.F_ASSOCIATION);
        }
        return associationContainer;
    }

    private List<PrismContainerValue<ShadowAssociationType>> getAssociations() {
        return getAssociationContainer().getValues();
    }

    public ShadowAssociationsAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of associations in "+desc(), expected, getAssociations().size());
        return this;
    }

    public ShadowAssociationsAsserter<R> assertAttributes(QName... expectedAssociations) {
        for (QName expectedAssociation: expectedAssociations) {
            List<ShadowAssociationType> associations = findAssociations(expectedAssociation);
            if (associations.isEmpty()) {
                fail("Expected association "+expectedAssociation+" in "+desc()+" but there was none. Association present: "+prettyPrintPresentAssociationNames());
            }
        }
        for (PrismContainerValue<ShadowAssociationType> existingAssociation : getAssociations()) {
            if (!QNameUtil.contains(expectedAssociations, existingAssociation.asContainerable().getName())) {
                fail("Unexpected association "+existingAssociation.asContainerable().getName()+" in "+desc()+". Expected attributes: "+QNameUtil.prettyPrint(expectedAssociations));
            }
        }
        return this;
    }

    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(String associationName) {
        List<ShadowAssociationType> associations = findAssociations(associationName);
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(associations, this, "association "+associationName+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(QName associationQName) {
        List<ShadowAssociationType> associations = findAssociations(associationQName);
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(associations, this, "association "+associationQName+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull List<ShadowAssociationType> findAssociations(QName assocName) {
        List<ShadowAssociationType> assocTypes = new ArrayList<>();
        for (PrismContainerValue<ShadowAssociationType> association : getAssociations()) {
            ShadowAssociationType associationType = association.asContainerable();
            if (QNameUtil.match(assocName, associationType.getName())) {
                assocTypes.add(associationType);
            }
        }
        return assocTypes;
    }


    private @NotNull List<ShadowAssociationType> findAssociations(String assocName) {
        List<ShadowAssociationType> assocTypes = new ArrayList<>();
        for (PrismContainerValue<ShadowAssociationType> association : getAssociations()) {
            ShadowAssociationType associationType = association.asContainerable();
            if (assocName.equals(associationType.getName().getLocalPart())) {
                assocTypes.add(associationType);
            }
        }
        return assocTypes;
    }

    private List<QName> getPresentAssociationNames() {
        List<QName> qnames = new ArrayList<>();
        for (PrismContainerValue<ShadowAssociationType> association : getAssociations()) {
            QName name = association.asContainerable().getName();
            if (!qnames.contains(name)) {
                qnames.add(name);
            }
        }
        return qnames;
    }

    private String prettyPrintPresentAssociationNames() {
        StringBuilder sb = new StringBuilder();
        Iterator<QName> iterator = getPresentAssociationNames().iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public ShadowAssociationsAsserter<R> assertAny() {
        assertNotNull("No association container in "+desc(), getAssociationContainer());
        List<PrismContainerValue<ShadowAssociationType>> associations = getAssociations();
        assertNotNull("No associations in "+desc(), associations);
        assertFalse("No associations in "+desc(), associations.isEmpty());
        return this;
    }

    public <T> ShadowAssociationsAsserter<R> assertNoAssociation(QName attrName) {
        List<ShadowAssociationType> association = findAssociations(attrName);
        assertTrue("Unexpected association "+attrName+" in "+desc()+": "+association, association.isEmpty());
        return this;
    }

    protected String desc() {
        return descWithDetails(getShadow());
    }

    @Override
    public ShadowAsserter<R> end() {
        return shadowAsserter;
    }

}
