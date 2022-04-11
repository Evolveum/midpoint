/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.service;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/serviceHistoryNew")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICE_HISTORY_URL,
                label = "PageServiceHistory.auth.service.label",
                description = "PageServiceHistory.auth.service.description") })
public class PageServiceHistory extends PageService {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageServiceHistory.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageServiceHistory.class);
    private String date = "";

    public PageServiceHistory(final PrismObject<ServiceType> service, String date) {
        super(service);
        this.date = date;
    }

    @Override
    protected FocusDetailsModels<ServiceType> createObjectDetailsModels(PrismObject<ServiceType> object) {
        return new FocusDetailsModels<ServiceType>(createPrismObjectModel(object), this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isReadonly() {
                return true;
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                String name = null;
                if (getModelObjectType() != null) {
                    name = WebComponentUtil.getName(getModelObjectType());
                }
                return createStringResource("PageUserHistory.title", name, date).getObject();
            }
        };
    }

}

