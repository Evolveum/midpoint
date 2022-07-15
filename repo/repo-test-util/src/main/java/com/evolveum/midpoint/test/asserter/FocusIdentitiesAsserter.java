/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.FocusIdentitiesTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;

public class FocusIdentitiesAsserter<RA> extends PrismContainerValueAsserter<FocusIdentitiesType, RA> {

    FocusIdentitiesAsserter(PrismContainerValue<FocusIdentitiesType> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public FocusIdentitiesAsserter<RA> assertSingle() throws SchemaException {
        return assertNumber(1);
    }

    @SuppressWarnings("SameParameterValue")
    private FocusIdentitiesAsserter<RA> assertNumber(int expected) throws SchemaException {
        assertThat(getNumber())
                .as("number of identities")
                .isEqualTo(expected);
        return this;
    }

    private int getNumber() throws SchemaException {
        return getIdentitiesBean().getIdentity().size();
    }

    public FocusIdentityAsserter<FocusIdentitiesAsserter<RA>> own() throws SchemaException {
        //noinspection unchecked
        FocusIdentityAsserter<FocusIdentitiesAsserter<RA>> asserter =
                new FocusIdentityAsserter<>(
                        (PrismContainerValue<FocusIdentityType>) getOwnRequired().asPrismContainerValue(),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private FocusIdentityType getOwnRequired() throws SchemaException {
        return requireNonNull(
                getOwn(),
                () -> new AssertionError("No own identity found in " + getDetails()));
    }

    private FocusIdentityType getOwn() throws SchemaException {
        return FocusIdentitiesTypeUtil.getOwnIdentity(getIdentitiesBean());
    }

    private @NotNull FocusIdentitiesType getIdentitiesBean() throws SchemaException {
        return getRealValueRequired(FocusIdentitiesType.class);
    }
}
