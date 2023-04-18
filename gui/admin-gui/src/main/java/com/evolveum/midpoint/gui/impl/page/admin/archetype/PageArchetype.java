/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.archetype;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.archetype.ArchetypeSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/archetype")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL,
                        label = "PageArchetypes.auth.archetypesAll.label",
                        description = "PageArchetypes.auth.archetypesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPE_URL,
                        label = "PageArchetype.auth.user.label",
                        description = "PageArchetype.auth.archetype.description")
        })
public class PageArchetype extends PageAbstractRole<ArchetypeType, FocusDetailsModels<ArchetypeType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentHolderDetails.class);

    private static final long serialVersionUID = 1L;

    public PageArchetype() {
        super();
    }

    public PageArchetype(PageParameters parameters) {
        super(parameters);
    }

    public PageArchetype(final PrismObject<ArchetypeType> obj) {
        super(obj);
    }

    @Override
    public Class<ArchetypeType> getType() {
        return ArchetypeType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ArchetypeType> summaryModel) {
        return new ArchetypeSummaryPanel(id, getModelObjectWithApplicableDelta(), summaryModel, getSummaryPanelSpecification());
    }

    private IModel<ArchetypeType> getModelObjectWithApplicableDelta() {
        return () -> {
            PrismObject<ArchetypeType> object = getObjectForResolvingArchetypePolicyDisplayType();
            return object != null ? object.asObjectable() : null;
        };
    }

    @Override
    protected PrismObject<ArchetypeType> getObjectForResolvingArchetypePolicyDisplayType() {
        try {
            return getModelWrapperObject().getObjectApplyDelta();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't apply deltas for archetypes " + getModelWrapperObject().getObject(), e);
        }
        return null;
    }
}
