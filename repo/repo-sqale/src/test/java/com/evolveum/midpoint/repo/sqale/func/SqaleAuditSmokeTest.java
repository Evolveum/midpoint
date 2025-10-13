/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.func;

import com.evolveum.midpoint.init.AuditServiceProxy;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditService;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.RepositoryDiag;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SqaleAuditSmokeTest extends SqaleRepoBaseTest {

    private SqaleAuditService sqaleAuditService;

    @BeforeClass
    public void initObjects() {
        sqaleAuditService = ((AuditServiceProxy) auditService).getImplementation(SqaleAuditService.class);
    }

    @Test
    public void test001RepositoryDiag() {
        when("repository diag is called");
        RepositoryDiag diag = repositoryService.getRepositoryDiag();

        expect("diag object with labeled values");
        assertThat(diag).isNotNull();

        List<LabeledString> details = diag.getAdditionalDetails();
        assertThat(details)
                .isNotNull()
                .isNotEmpty();

        LabeledString schemaChangeNumber = details.stream().filter(ls -> SqaleUtils.SCHEMA_AUDIT_CHANGE_NUMBER.equals(ls.getLabel())).findFirst().orElse(null);
        assertThat(schemaChangeNumber)
                .isNotNull();
    }
}
