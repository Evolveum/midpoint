/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Referencable;

import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowAssociationsMap;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The shadow is not necessarily non-raw here, although {@link #association(QName)} and {@link #association(String)}
 * methods require that.
 *
 * @author semancik
 */
public class ShadowAssociationsAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {

    private final ShadowAssociationsMap associationsMap;
    private final ShadowAsserter<R> shadowAsserter;

    ShadowAssociationsAsserter(ShadowAsserter<R> shadowAsserter, String details) {
        super(details);
        this.shadowAsserter = shadowAsserter;
        this.associationsMap = ShadowAssociationsMap.of(getShadow());
    }

    private PrismObject<ShadowType> getShadow() {
        return shadowAsserter.getObject();
    }

    public ShadowAssociationsAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of associations in "+desc(), expected, associationsMap.size());
        return this;
    }

    public ShadowAssociationsAsserter<R> assertValuesCount(int expected) {
        assertEquals("Wrong number of association values in " + desc(), expected, associationsMap.valuesCount());
        return this;
    }

    public ShadowAssociationsAsserter<R> assertAssociations(QName... expectedAssociations) {
        assertThat(getPresentAssociationNames())
                .as("associations in " + desc())
                .containsExactlyInAnyOrder(expectedAssociations);
        return this;
    }

    /** The shadow must not be raw. */
    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(String associationName) {
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(
                toNonRaw(getValues(associationName)), this, "association " + associationName + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    /** The shadow must not be raw. */
    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(QName associationName) {
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(
                toNonRaw(getValues(associationName)), this, "association " + associationName + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    private Collection<ShadowAssociationValue> toNonRaw(Collection<ShadowAssociationValueType> values) {
        return values.stream()
                .map(v -> (ShadowAssociationValue) v.asPrismContainerValue())
                .toList();
    }

    private @NotNull Collection<ShadowAssociationValueType> getValues(QName assocName) {
        return associationsMap.getValues(assocName);
    }

    private @NotNull Collection<ShadowAssociationValueType> getValues(String assocName) {
        return getValues(new QName(assocName));
    }

    private Collection<QName> getPresentAssociationNames() {
        return associationsMap.keySet();
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
        assertThat(associationsMap).as("associations in " + desc()).isNotEmpty();
        return this;
    }

    public <T> ShadowAssociationsAsserter<R> assertNoAssociation(QName assocName) {
        var association = getValues(assocName);
        assertThat(association).as("association " + assocName + " in " + desc()).isEmpty();
        return this;
    }

    protected String desc() {
        return descWithDetails(getShadow());
    }

    @Override
    public ShadowAsserter<R> end() {
        return shadowAsserter;
    }

    public ShadowAssociationsAsserter<R> assertExistsForShadow(@NotNull String oid) {
        assertThat(getValuesForShadow(oid))
                .as("associations for shadow " + oid + " in " + desc())
                .isNotEmpty();
        return this;
    }

    public ShadowAssociationsAsserter<R> assertNoneForShadow(@NotNull String oid) {
        assertThat(getValuesForShadow(oid))
                .as("associations for shadow " + oid + " in " + desc())
                .isEmpty();
        return this;
    }

    public Collection<ShadowAssociationValueType> getValuesForShadow(@NotNull String oid) {
        return associationsMap.values().stream()
                .flatMap(a -> a.getValues().stream())
                .filter(v -> oid.equals(Referencable.getOid(v.getShadowRef())))
                .toList();
    }
}
