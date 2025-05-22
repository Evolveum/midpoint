/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class UserFriendlyPrettyPrinterTest extends AbstractUnitTest {

    private static final String SAMPLE_DATE = "2025-04-14T11:12:15.274+02:00";

    private static final String SAMPLE_OID = "c720ca00-15f3-4fd5-a2c1-f5857adc129f";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        SchemaDebugUtil.initialize(); // Make sure the pretty printer is activated
    }

    private String prettyPrintValue(PrismValue value) {
        return new UserFriendlyPrettyPrinter().prettyPrintValue(value, 0);
    }

    private String prettyPrint(Item<?, ?> item) {
        return new UserFriendlyPrettyPrinter().prettyPrintItem(item, 0);
    }

    @Test
    public void testPlainStructuredProperty() {
        RolesOfTeammateType rolesOfTeammate = new RolesOfTeammateType();
        rolesOfTeammate.setEnabled(true);

        AutocompleteSearchConfigurationType autocompleteSearchConfiguration = new AutocompleteSearchConfigurationType();
        autocompleteSearchConfiguration.setAutocompleteMinChars(2);
        autocompleteSearchConfiguration.setId(1L);

        rolesOfTeammate.setAutocompleteConfiguration(autocompleteSearchConfiguration);

        ExpressionType expression = new ExpressionType();
        ScriptExpressionEvaluatorType scriptExpressionEvaluator = new ScriptExpressionEvaluatorType();
        scriptExpressionEvaluator.setCode("return \"Hello world\";");
        expression.getExpressionEvaluator().add(new ObjectFactory().createScript(scriptExpressionEvaluator));

        autocompleteSearchConfiguration.setDisplayExpression(expression);

        PrismPropertyValue<RolesOfTeammateType> value = new PrismPropertyValueImpl<>(rolesOfTeammate);
        String strPropertyValue = prettyPrintValue(value);

        String expected ="RolesOfTeammateType[autocompleteConfiguration=PCV(1):["
                + "PP({.../common/common-3}autocompleteMinChars):[PPV(Integer:2)], "
                + "PP({.../common/common-3}displayExpression):["
                + "PPV(ExpressionType:ExpressionType(variable=[],evaluator=script:com.evolveum.midpoint.xml.ns._public.common."
                + "common_3.ScriptExpressionEvaluatorType@315cf170[code=return \"Hello world\";,language=<null>,"
                + "objectVariableMode=<null>,returnType=<null>,valueVariableMode=<null>,condition=<null>,description=<null>,"
                + "documentation=<null>,includeNullInputs=<null>,relativityMode=<null>,trace=<null>]))]],enabled=true]";

        // todo assertion is not working, also string looks horrible
//        Assertions.assertThat(strPropertyValue)
//                .isEqualTo(expected);
    }

    @Test
    public void testPolyStringProperty() {
        UserType user = new UserType();
        user.setName(PrismTestUtil.createPolyStringType("John"));

        String strPropertyValue = prettyPrintValue(user.asPrismContainerValue().findProperty(UserType.F_NAME).getValue());

        Assertions.assertThat(strPropertyValue).isEqualTo("John");

        String strProperty = prettyPrint(user.asPrismContainerValue().findProperty(UserType.F_NAME));

        Assertions.assertThat(strProperty).isEqualTo("name: " + strPropertyValue);
    }

    @Test
    public void testAssignment() {
        UserType user = new UserType();

        AssignmentType assignment = createAssignment();
        user.getAssignment().add(assignment);

        String strValue = prettyPrintValue(assignment.asPrismContainerValue());

        String expectedValue = "null:\n"
                + "  description: some description is here not very long\n"
                + "  activation: \n"
                + "    administrativeStatus: ENABLED\n"
                + "    validFrom: " + SAMPLE_DATE + "\n"
                + "  targetRef: " + SAMPLE_OID + " (UserType)";

        Assertions.assertThat(strValue)
                .isEqualTo(expectedValue);

        String strContainer = prettyPrint(user.asPrismObject().findContainer(UserType.F_ASSIGNMENT));

        Assertions.assertThat(strContainer)
                .isEqualTo("assignment: \n" + indent(expectedValue, 1));

    }

    private String indent(String value, int i) {
        return Arrays.stream(StringUtils.split(value, '\n'))
                .map(line -> StringUtils.repeat(UserFriendlyPrettyPrinterOptions.DEFAULT_INDENT, i) + line)
                .collect(Collectors.joining("\n"));
    }

    private AssignmentType createAssignment() {
        AssignmentType assignment = new AssignmentType();
        assignment.setDescription("some description is here not very long");
        assignment.setActivation(new ActivationType());
        assignment.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignment.getActivation().setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(SAMPLE_DATE));

        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setType(UserType.COMPLEX_TYPE);
        targetRef.setOid(SAMPLE_OID);
        targetRef.setRelation(SchemaConstants.ORG_DEFAULT);
        assignment.setTargetRef(targetRef);

        return assignment;
    }

    @Test
    public void testItemDeltaSimple() throws Exception {
        String oid = UUID.randomUUID().toString();
        ObjectDelta<UserType> delta = PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ORGANIZATION)
                .delete(PolyString.fromOrig("qwe"))
                .add(PolyString.fromOrig("123"), PolyString.fromOrig("456"))
                .item(UserType.F_ASSIGNMENT)
                .add(createAssignment())
                .asObjectDelta(oid);

        UserFriendlyPrettyPrinterOptions options = new UserFriendlyPrettyPrinterOptions();

        UserFriendlyPrettyPrinter printer = new UserFriendlyPrettyPrinter(options);
        String strDelta = printer.prettyPrintObjectDelta(delta, 0);

        String expected = oid + ", UserType (MODIFY): \n"
                + "  organization: \n"
                + "    Add: 123, 456\n"
                + "    Delete: qwe\n"
                + "  assignment: \n"
                + "    Add: \n"
                + "      null:\n"
                + "        description: some description is here not very long\n"
                + "        activation: \n"
                + "          administrativeStatus: ENABLED\n"
                + "          validFrom: 2025-04-14T11:12:15.274+02:00\n"
                + "        targetRef: c720ca00-15f3-4fd5-a2c1-f5857adc129f (UserType)";

        Assertions.assertThat(strDelta)
                .isEqualTo(expected);
    }
}
