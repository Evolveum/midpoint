/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.objectcollection;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.objectCollection.ObjectCollectionSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/objectCollection")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL,
                        label = "PageObjectCollection.auth.objectCollectionsAll.label",
                        description = "PageObjectCollection.auth.objectCollectionsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTION_URL,
                        label = "PageObjectCollection.auth.objectCollection.label",
                        description = "PageObjectCollection.auth.objectCollection.description")
        })
public class PageObjectCollection extends PageAssignmentHolderDetails<ObjectCollectionType, AssignmentHolderDetailsModel<ObjectCollectionType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageObjectCollection.class);

    public PageObjectCollection() {
        super();
    }

    public PageObjectCollection(PageParameters parameters) {
        super(parameters);
    }

    public PageObjectCollection(final PrismObject<ObjectCollectionType> unitToEdit) {
        super(unitToEdit);
    }

    @Override
    public Class<ObjectCollectionType> getType() {
        return ObjectCollectionType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ObjectCollectionType> summaryModel) {
        return new ObjectCollectionSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

}
