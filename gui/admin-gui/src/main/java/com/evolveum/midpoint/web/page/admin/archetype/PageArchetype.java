/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.archetype;

import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ContainerOfSystemConfigurationPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractRoleMainPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

@PageDescriptor(
        url  = "/admin/archetype",
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL,
                        label = "PageArchetypes.auth.archetypesAll.label",
                        description = "PageArchetypes.auth.archetypesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPE_URL,
                        label = "PageArchetype.auth.user.label",
                        description = "PageArchetype.auth.archetype.description")
        })
public class PageArchetype extends PageAdminAbstractRole<ArchetypeType> {

    private static final long serialVersionUID = 1L;

    public PageArchetype() {
        super();
    }

    public PageArchetype(PageParameters parameters) {
        super(parameters);
    }

    public PageArchetype(final PrismObject<ArchetypeType> role) {
        super(role);
    }

    public PageArchetype(final PrismObject<ArchetypeType> userToEdit, boolean isNewObject) {
        super(userToEdit, isNewObject);
    }

    public PageArchetype(final PrismObject<ArchetypeType> abstractRole, boolean isNewObject, boolean isReadonly) {
        super(abstractRole, isNewObject, isReadonly);
    }


    @Override
    public Class<ArchetypeType> getCompileTimeClass() {
        return ArchetypeType.class;
    }

    @Override
    protected ArchetypeType createNewObject() {
        return new ArchetypeType(getPrismContext());
    }

    @Override
    protected ObjectSummaryPanel<ArchetypeType> createSummaryPanel(IModel<ArchetypeType> summaryModel) {
        return new ArchetypeSummaryPanel(ID_SUMMARY_PANEL, summaryModel, this);

    }

    @Override
    protected AbstractObjectMainPanel<ArchetypeType> createMainPanel(String id) {
        return new AbstractRoleMainPanel<ArchetypeType>(id, getObjectModel(), getProjectionModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            public AbstractRoleMemberPanel<ArchetypeType> createMemberPanel(String panelId) {
                return new ArchetypeMembersPanel(panelId, new Model<>(getObject().asObjectable()));
            }

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<ArchetypeType> parentPage) {
                List<ITab> tabs =  super.createTabs(parentPage);
                tabs.add(
                        new PanelTab(parentPage.createStringResource("PageArchetype.archetypePolicy"),
                                getTabVisibility(ComponentConstants.UI_ARCHTYPE_TAB_ARCHETYPE_POLICY_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new ContainerOfSystemConfigurationPanel<ArchetypePolicyType>(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ArchetypeType.F_ARCHETYPE_POLICY), ArchetypePolicyType.COMPLEX_TYPE);
                            }
                        });

                return tabs;
            }
        };

    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageArchetypes.class;
    }

}
