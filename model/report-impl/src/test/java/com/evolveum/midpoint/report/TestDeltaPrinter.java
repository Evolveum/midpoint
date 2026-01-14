/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.report.impl.DeltaPrinterOptions;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDeltaPrinter extends AbstractModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/misc");

    private static final File DELTA = new File(TEST_DIR, "delta.xml");

    @Test
    public void test010Description() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_DESCRIPTION),
                createDefaultOptions(),
                List.of(" Replace: my custom description"));
    }

    @Test
    public void test020MultivalueAssignment() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ASSIGNMENT),
                createDefaultOptions(),
                List.of(" Add: \n"
                        + "  0:\n"
                        + "   description: Approved\n"
                        + "   targetRef: 123a49bc-6f5b-4746-8461-2e1a63307123 (RoleType)\n"
                        + "  1:\n"
                        + "   description: Default\n"
                        + "   targetRef: 456a49bc-6f5b-4746-8461-2e1a63307456 (RoleType)"));
    }

    @Test
    public void test020ActivationContainer() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ACTIVATION),
                createDefaultOptions(),
                List.of(" Add: \n"
                        +"  administrativeStatus: DISABLED"));
    }

    @Test(enabled = false)
    public void test030ActivationAdministrativeStatusContainer() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                createDefaultOptions(),
                List.of(" Add: \n"
                        +"  administrativeStatus: DISABLED"));
    }

    @Test
    public void test040MultivaluePolyString() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ORGANIZATION),
                createDefaultOptions(),
                List.of(" Replace: org1, org2"));
    }

    /**
     * MID-10985 Searching for delta changed item path crashes whole page
     */
    @Test(enabled = false)
    public void test050PrintMultivalueContainerChildrenDelta() throws Exception {
        assertDeltaOutput(
                DELTA,
                ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
                createDefaultOptions(),
                List.of());
    }

    private void assertDeltaOutput(File delta, ItemPath path, DeltaPrinterOptions opts, List<String> results) throws Exception {
        ObjectDeltaOperationType input = PrismTestUtil.parseAnyValue(delta);

        List<String> output = ReportUtils.printDelta(input, path, opts);
        Assertions.assertThat(output).containsExactlyElementsOf(results);
    }

    private DeltaPrinterOptions createDefaultOptions() {
        DeltaPrinterOptions opts = new DeltaPrinterOptions()
                // whether to show delta if search bar doesn't contain "Changed item path" value
                .showFullObjectDelta(true)
                // whether to show also change in subitems of the "Changed item path", e.g. if changed item path is assignment
                // whether to display deltas that contain changes for assignment/activation
                .showPartialDeltas(true);

        opts.prettyPrinterOptions()
                // whether to show full object (with all attributes) or just the high level info (type, oid, name, etc.)
                .showFullAddObjectDelta(false)
                .showOperational(false)
                .indentation(" ");

        return opts;
    }
}
