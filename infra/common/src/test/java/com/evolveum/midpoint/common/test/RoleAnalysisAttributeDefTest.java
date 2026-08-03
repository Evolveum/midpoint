/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.test;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.Objects;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests role-analysis attribute value handling for typed schema values.
 */
public class RoleAnalysisAttributeDefTest extends AbstractUnitTest {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    /** Verifies that enum attribute values can be extracted as strings and rebuilt into typed queries. */
    @Test
    public void enumAttributeExtractedValueBuildsTypedQuery() {
        RoleAnalysisAttributeDef attributeDef = createEffectiveStatusAttributeDef();
        UserType user = new UserType()
                .activation(new ActivationType()
                        .effectiveStatus(ActivationStatusType.ENABLED));

        String resolvedValue = attributeDef.resolveSingleValueItem(
                user.asPrismObject(),
                attributeDef.getPath());

        assertEquals("enabled", resolvedValue);

        EqualFilter<?> filter = (EqualFilter<?>) attributeDef.getQuery(resolvedValue).getFilter();
        assertEquals(ActivationStatusType.ENABLED, Objects.requireNonNull(filter.getSingleValue()).getRealValue());
    }

    private RoleAnalysisAttributeDef createEffectiveStatusAttributeDef() {
        PrismObjectDefinition<UserType> userDefinition = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        ItemPath itemPath = ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS);
        ItemDefinition<?> definition = userDefinition.findItemDefinition(itemPath);
        return new RoleAnalysisAttributeDef(itemPath, definition, UserType.class);
    }
}
