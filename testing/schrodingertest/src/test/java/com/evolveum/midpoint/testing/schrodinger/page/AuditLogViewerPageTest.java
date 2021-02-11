package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.page.report.AuditLogViewerDetailsPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.annotations.Test;

public class AuditLogViewerPageTest extends AbstractSchrodingerTest {

    @Test
    public void test00100returnBackToAuditLogDetails() {
        createUser("auditLogViewerTestUser");
        basicPage
                .listUsers()
                    .table()
                        .search()
                            .byName()
                            .inputValue("auditLogViewerTestUser")
                            .updateSearch()
                        .and()
                    .clickByName("auditLogViewerTestUser")
                        .selectTabBasic()
                            .form()
                                .changeAttributeValue("Name", "auditLogViewerTestUser", "auditLogViewerTestUser1")
                                .and()
                            .and()
                        .clickSave();
        AuditLogViewerDetailsPage detailsPage = basicPage
                .auditLogViewer()
                    .table()
                        .search()
                            .referencePanelByItemName("Target", true)
                            .inputRefName("auditLogViewer", "auditLogViewerTestUser1")
                            .updateSearch()
                        .and()
                    .clickByRowColumnNumber(0, 0);
        detailsPage.deltaPanel()
                    .header()
                        .assertIsLink()
                        .clickNameLink()
                    .clickBack();
        detailsPage.assertAuditLogViewerDetailsPageIsOpened();
    }
}
