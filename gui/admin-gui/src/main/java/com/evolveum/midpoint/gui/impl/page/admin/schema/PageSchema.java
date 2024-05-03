/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/schema", matchUrlForSecurity = "/admin/schema")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SCHEMAS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SCHEMA_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageSchema extends PageAssignmentHolderDetails<SchemaType, SchemaDetailsModel> {

        private static final long serialVersionUID = 1L;

        public PageSchema(PageParameters pageParameters) {
        super(pageParameters);
    }

        public PageSchema(PrismObject<SchemaType> schema) {
        super(schema);
    }

        public PageSchema() {
            super();
        }

    @Override
    protected SchemaDetailsModel createObjectDetailsModels(PrismObject<SchemaType> object) {
        return new SchemaDetailsModel(createPrismObjectModel(object), this);
    }

    @Override
    public Class<SchemaType> getType() {
        return SchemaType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<SchemaType> summaryModel) {
        return new ObjectSummaryPanel<>(id, summaryModel, getSummaryPanelSpecification()) {

            @Override
            protected String getDefaultIconCssClass() {
                return "fa fa-cog";
            }

            @Override
            protected String getIconBoxAdditionalCssClass() {
                return "summary-panel-icon-box-md"; //todo
            }

            @Override
            protected String getBoxAdditionalCssClass() {
                return "summary-panel-box-md"; //todo
            }
        };
    }
}
