/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import com.evolveum.midpoint.model.api.correlator.CorrelationService;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseRelatedUtils;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Common superclass for all correlation tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractCorrelationTest extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlation");

    static final String NS_EXT = "http://example.com/idmatch";

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final ItemName EXT_DATE_OF_BIRTH = new ItemName(NS_EXT, "dateOfBirth");
    private static final ItemName EXT_NATIONAL_ID = new ItemName(NS_EXT, "nationalId");

    static final ItemPath PATH_DATE_OF_BIRTH = ItemPath.create(UserType.F_EXTENSION, EXT_DATE_OF_BIRTH);
    static final ItemPath PATH_NATIONAL_ID = ItemPath.create(UserType.F_EXTENSION, EXT_NATIONAL_ID);

    @Autowired CorrelationService correlationService;
    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired CaseService caseService;

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) throws FileNotFoundException {
        // We don't need these now.
    }

    // Owner OID may be different from the identifier in URI.
    void resolveCase(@NotNull CaseType aCase, @Nullable String ownerOid, Task task, OperationResult result)
            throws CommonException {
        List<CaseWorkItemType> workItems = CaseRelatedUtils.getOpenWorkItems(aCase);
        assertThat(workItems).as("work items in " + aCase).isNotEmpty();

        caseService.completeWorkItem(
                WorkItemId.of(workItems.get(0)),
                CorrelationCaseUtil.createDefaultOutput(
                        determineOwnerOptionIdentifier(aCase, ownerOid)),
                task,
                result);
    }

    private @NotNull OwnerOptionIdentifier determineOwnerOptionIdentifier(@NotNull CaseType aCase, @Nullable String ownerOid)
            throws SchemaException {
        if (ownerOid == null) {
            return OwnerOptionIdentifier.forNoOwner();
        }
        for (ResourceObjectOwnerOptionType optionBean : CorrelationCaseUtil.getOwnerOptionsList(aCase)) {
            ObjectReferenceType ownerRef = optionBean.getCandidateOwnerRef();
            if (ownerRef != null && ownerOid.equals(ownerRef.getOid())) {
                return OwnerOptionIdentifier.fromStringValue(optionBean.getIdentifier());
            }
        }
        throw new IllegalStateException("Unknown owner OID (not in the options list): " + ownerOid);
    }
}
