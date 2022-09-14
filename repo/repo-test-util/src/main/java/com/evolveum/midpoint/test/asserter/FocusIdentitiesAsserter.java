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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public FocusIdentitiesAsserter<RA> assertNumber(int expected) throws SchemaException {
        assertThat(getNumber())
                .as("number of identities")
                .isEqualTo(expected);
        return this;
    }

    private int getNumber() throws SchemaException {
        return getIdentitiesBean().getIdentity().size();
    }

    public FocusNormalizedDataAsserter<FocusIdentitiesAsserter<RA>> normalizedData() throws SchemaException {
        //noinspection unchecked
        FocusNormalizedDataAsserter<FocusIdentitiesAsserter<RA>> asserter =
                new FocusNormalizedDataAsserter<>(
                        (PrismContainerValue<FocusNormalizedDataType>)
                                getPrismValue().asContainerable().getNormalizedData().asPrismContainerValue(),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusIdentityAsserter<FocusIdentitiesAsserter<RA>> fromResource(
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable String tag) throws SchemaException {
        FocusIdentitySourceType source = new FocusIdentitySourceType()
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                .kind(kind)
                .intent(intent)
                .tag(tag);

        //noinspection unchecked
        FocusIdentityAsserter<FocusIdentitiesAsserter<RA>> asserter =
                new FocusIdentityAsserter<>(
                        (PrismContainerValue<FocusIdentityType>) findRequired(source).asPrismContainerValue(),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private FocusIdentityType findRequired(@Nullable FocusIdentitySourceType source) throws SchemaException {
        return requireNonNull(
                find(source),
                () -> new AssertionError("No identity matching '" + source + "' found in " + getDetails()));
    }

    private FocusIdentityType find(@Nullable FocusIdentitySourceType source) throws SchemaException {
        return FocusIdentitiesTypeUtil.getMatchingIdentity(getIdentitiesBean(), source);
    }

    private @NotNull FocusIdentitiesType getIdentitiesBean() throws SchemaException {
        return getRealValueRequired(FocusIdentitiesType.class);
    }
}
