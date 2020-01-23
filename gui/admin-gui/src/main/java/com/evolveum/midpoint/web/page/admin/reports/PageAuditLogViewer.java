/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditLogViewerPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;

import com.evolveum.midpoint.web.session.AuditLogStorage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewer", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL, label = "PageAuditLogViewer.auth.auditLogViewer.label", description = "PageAuditLogViewer.auth.auditLogViewer.description") })
public class PageAuditLogViewer extends PageBase {

    private static final long serialVersionUID = 1L;
    private static final String ID_PANEL = "auditLogViewerPanel";
    public PageAuditLogViewer() {
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        AuditLogViewerPanel panel = new AuditLogViewerPanel(ID_PANEL, new IModel<AuditSearchDto>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AuditSearchDto getObject() {
                initDefaultSearchDto();
                return getAuditLogStorage().getSearchDto();
            }

            @Override
            public void setObject(AuditSearchDto auditSearchDto) {
                getAuditLogStorage().setSearchDto(auditSearchDto);
            }

            @Override
            public void detach() {

            }
        }, false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAuditSearchStorage(AuditSearchDto searchDto) {
                getAuditLogStorage().setSearchDto(searchDto);
                getAuditLogStorage().setPageNumber(0);
            }

            @Override
            protected void resetAuditSearchStorage() {
                getAuditLogStorage().setSearchDto(new AuditSearchDto());

            }

            @Override
            protected void updateCurrentPage(long current) {
                getAuditLogStorage().setPageNumber(current);

            }

            @Override
            protected long getCurrentPage() {
                return getAuditLogStorage().getPageNumber();
            }

        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initDefaultSearchDto(){
        XMLGregorianCalendar searchFromDate = getSessionStorage().getAuditLog().getSearchDto().getFrom();
        if (searchFromDate == null){
            Date todayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            getSessionStorage().getAuditLog().getSearchDto().setFrom(MiscUtil.asXMLGregorianCalendar(todayDate));
        }
    }

    private AuditLogStorage getAuditLogStorage(){
        return getSessionStorage().getAuditLog();
    }


}
