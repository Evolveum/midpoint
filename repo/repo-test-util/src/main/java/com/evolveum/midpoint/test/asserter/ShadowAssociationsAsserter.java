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
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.ShadowAssociationsContainer;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The shadow is assumed to be non-raw.
 *
 * @author semancik
 */
public class ShadowAssociationsAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {

    private final ShadowAssociationsContainer associationsContainer;
    private final ShadowAsserter<R> shadowAsserter;

    ShadowAssociationsAsserter(ShadowAsserter<R> shadowAsserter, String details) {
        super(details);
        this.shadowAsserter = shadowAsserter;
        this.associationsContainer = Objects.requireNonNullElseGet(
                ShadowUtil.getAssociationsContainer(getShadow()),
                () -> ShadowUtil.getResourceObjectDefinition(getShadow().asObjectable())
                        .toShadowAssociationsContainerDefinition()
                        .instantiate());
    }

    private PrismObject<ShadowType> getShadow() {
        return shadowAsserter.getObject();
    }

    public ShadowAssociationsAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of associations in "+desc(), expected, associationsContainer.getAssociations().size());
        return this;
    }

    public ShadowAssociationsAsserter<R> assertValuesCount(int expected) {
        assertEquals("Wrong number of association values in " + desc(), expected, getValuesCount());
        return this;
    }

    private int getValuesCount() {
        return associationsContainer.getAssociations().stream()
                .mapToInt(a -> a.size())
                .sum();
    }

    public ShadowAssociationsAsserter<R> assertAssociations(QName... expectedAssociations) {
        assertThat(getPresentAssociationNames())
                .as("associations in " + desc())
                .containsExactlyInAnyOrder(expectedAssociations);
        return this;
    }

    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(String associationName) {
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(
                getValues(associationName), this, "association " + associationName + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> association(QName associationName) {
        ShadowAssociationAsserter<ShadowAssociationsAsserter<R>> asserter = new ShadowAssociationAsserter<>(
                getValues(associationName), this, "association " + associationName + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    private Collection<ShadowReferenceAttributeValue> toNonRaw(Collection<ShadowAssociationValueType> values) {
        return values.stream()
                .map(v -> (ShadowReferenceAttributeValue) v.asPrismContainerValue())
                .toList();
    }

    private @NotNull Collection<ShadowAssociationValue> getValues(QName assocName) {
        return List.copyOf(associationsContainer.getAssociationValues(assocName));
    }

    private @NotNull Collection<ShadowAssociationValue> getValues(String assocName) {
        return getValues(new QName(assocName));
    }

    private Collection<QName> getPresentAssociationNames() {
        return List.copyOf(associationsContainer.getAssociationNames());
    }

    private String prettyPrintPresentAssociationNames() {
        StringBuilder sb = new StringBuilder();
        var iterator = getPresentAssociationNames().iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public ShadowAssociationsAsserter<R> assertAny() {
        assertThat(getPresentAssociationNames()).as("associations in " + desc()).isNotEmpty();
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

    public Collection<ShadowAssociationValue> getValuesForShadow(@NotNull String oid) {
        return associationsContainer.getAssociations().stream()
                .flatMap(sa -> sa.getAssociationValues().stream())
                .filter(sav -> oid.equals(Referencable.getOid(sav.getSingleObjectRefRequired())))
                .toList();
    }
}
