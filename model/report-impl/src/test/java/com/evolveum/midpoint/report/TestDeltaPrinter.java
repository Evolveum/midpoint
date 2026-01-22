/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.report.impl.DeltaPrinterOptions;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDeltaPrinter extends AbstractModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/misc");

    private static final File DELTA = new File(TEST_DIR, "delta.xml");

    private static final File DELTA_ADD = new File(TEST_DIR, "delta-add.xml");

    @Test
    public void test010Description() throws Exception {
        assertDeltaOutput(
                DELTA,
                UserType.F_DESCRIPTION,
                createDefaultOptions(),
                List.of(" Replace: my custom description"));
    }

    @Test
    public void test015DescriptionAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_DESCRIPTION),
                createDefaultOptions(),
                List.of(" Add: User description here"));
    }

    @Test
    public void test020MultivalueAssignment() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ASSIGNMENT),
                createDefaultOptions(),
                List.of(
                        "assignment: \n"
                                + " Add: \n"
                                + "  1:\n"
                                + "   description: Default\n"
                                + "   targetRef: 456a49bc-6f5b-4746-8461-2e1a63307456 (RoleType)\n"
                                + " Delete: \n"
                                + "  null:\n"
                                + "   description: Approved\n"
                                + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307123 (RoleType)\n"
                                + "  2:\n"
                                + "   description: Approved Two\n"
                                + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307777 (RoleType)",
                        "assignment/[25]/description: \n"
                                + " Add: simple assignment description"
                ));
    }

    @Test
    public void test020MultivalueAssignmentPartialFalse() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ASSIGNMENT),
                createDefaultOptions()
                        .showPartialDeltas(false),
                List.of(" Add: \n"
                        + "  1:\n"
                        + "   description: Default\n"
                        + "   targetRef: 456a49bc-6f5b-4746-8461-2e1a63307456 (RoleType)\n"
                        + " Delete: \n"
                        + "  null:\n"
                        + "   description: Approved\n"
                        + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307123 (RoleType)\n"
                        + "  2:\n"
                        + "   description: Approved Two\n"
                        + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307777 (RoleType)"));
    }

    @Test
    public void test022MultivalueAssignmentAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_ASSIGNMENT),
                createDefaultOptions(),
                List.of(" Add: \n"
                        + "  null:\n"
                        + "   description: Approved\n"
                        + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307123 (RoleType)\n"
                        + "  2:\n"
                        + "   description: Approved Two\n"
                        + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307777 (RoleType)"));
    }

    @Test
    public void test25ActivationAdministrativeStatus() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                createDefaultOptions(),
                List.of(" Add: DISABLED"));
    }

    @Test
    public void test27ActivationAdministrativeStatusAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                createDefaultOptions(),
                List.of(" Add: DISABLED"));
    }

    @Test
    public void test030ActivationContainer() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ACTIVATION),
                createDefaultOptions(),
                List.of(" Add: \n"
                        + "  administrativeStatus: DISABLED\n"
                        + "  validFrom: 2013-05-21T14:57:50.759+02:00"));
    }

    @Test
    public void test032ActivationContainerAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_ACTIVATION),
                createDefaultOptions(),
                List.of(" Add: \n"
                        + "  administrativeStatus: DISABLED\n"
                        + "  validFrom: 2013-05-21T14:57:50.759+02:00"));
    }

    @Test
    public void test040MultivaluePolyString() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ORGANIZATION),
                createDefaultOptions(),
                List.of(" Replace: org1, org2"));
    }

    @Test
    public void test042MultivaluePolyStringAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_ORGANIZATION),
                createDefaultOptions(),
                List.of(" Add: org1, org2"));
    }

    /**
     * MID-10985 Searching for delta changed item path crashes whole page
     */
    @Test
    public void test050AssignmentDescriptionDelta() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
                createDefaultOptions(),
                List.of("assignment/1/description: \n"
                                + " Add: Default",
                        "assignment/description: \n"
                                + " Delete: Approved",
                        "assignment/2/description: \n"
                                + " Delete: Approved Two",
                        "assignment/[25]/description: \n"
                                + " Add: simple assignment description"));
    }

    @Test
    public void test052AssignmentDescriptionDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
                createDefaultOptions(),
                List.of("assignment/description: \n"
                                + " Add: Approved",
                        "assignment/2/description: \n"
                                + " Add: Approved Two"));
    }

    @Test
    public void test060PrintExtensionDelta() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_EXTENSION),
                createDefaultOptions(),
                List.of(" Replace: \n" +
                        "  {https://example.com}stringProperty: \n" +
                        "    {  \"#value\" : \"value1\"}\n" +
                        "    {  \"#value\" : \"value2\"}"));
    }

    @Test
    public void test062PrintExtensionDeltaAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_EXTENSION),
                createDefaultOptions(),
                List.of(" Add: \n" +
                        "  {https://example.com}stringProperty: \n" +
                        "    value1\n" +
                        "    value2"));
    }

    @Test
    public void test070PrintExtensionItemDelta() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_EXTENSION, "stringProperty"),
                createDefaultOptions(),
                List.of(" Replace: value1, value2"));
    }

    @Test
    public void test072PrintExtensionItemDeltaAddDelta() throws Exception {
        assertDeltaOutput(
                DELTA_ADD,
                ItemPath.create(UserType.F_EXTENSION, "stringProperty"),
                createDefaultOptions(),
                List.of(" Add: value1, value2"));
    }

    @Test
    public void test080OperationalItemOnly() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_LAST_FAILED_LOGIN),
                createDefaultOptions(),
                List.of());
    }

    @Test
    public void test082OperationalItemOnlyAndVisible() throws Exception {
        DeltaPrinterOptions opts = createDefaultOptions();
        opts.prettyPrinterOptions()
                .showOperational(true);

        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_LAST_FAILED_LOGIN),
                opts,
                List.of(" Add: \n"
                        + "  LoginEventType[from=1.1.1.1,timestamp=2013-05-21T14:57:50.759+02:00]"));
    }

    private ObjectDeltaType parseDeltaFromFile(File file) throws SchemaException, IOException {
        PrismContext pc = PrismTestUtil.getPrismContext();

        return pc.parserFor(file)
                .context(pc.createParsingContextForCompatibilityMode())
                .parseRealValue(ObjectDeltaType.class);
    }

    private void assertDeltaOutput(
            File deltaFile, ItemPath path, DeltaPrinterOptions opts, List<String> expectedOutputResult) throws Exception {

        ObjectDeltaType deltaType = parseDeltaFromFile(deltaFile);
        ObjectDeltaOperationType input = new ObjectDeltaOperationType();
        input.setObjectDelta(deltaType);

        List<String> output = ReportUtils.printDelta(input, path, opts);
        Assertions.assertThat(output).containsExactlyElementsOf(expectedOutputResult);
    }

    private DeltaPrinterOptions createDefaultOptions() {
        DeltaPrinterOptions opts = new DeltaPrinterOptions()
                // whether to show delta if search bar doesn't contain "Changed item path" value
                .showFullObjectDelta(true)
                // whether to show also change in subitems of the "Changed item path", e.g. if changed item path is "assignment"
                // whether to display deltas that contain changes for "assignment/activation"
                .showPartialDeltas(true);

        opts.prettyPrinterOptions()
                // whether to show full object (with all attributes) or just the high level info (type, oid, name, etc.)
                .showFullAddObjectDelta(false)
                .showOperational(false)
                .indentation(" ");

        return opts;
    }
}
