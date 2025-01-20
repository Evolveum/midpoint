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
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.CreateComplexOrEnumerationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.basic.SchemaWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class PageSchema extends PageAssignmentHolderDetails<SchemaType, AssignmentHolderDetailsModel<SchemaType>> {

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
    public Class<SchemaType> getType() {
        return SchemaType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<SchemaType> summaryModel) {
        return null;
    }

    @Override
    protected Panel createVerticalSummaryPanel(String id, IModel<SchemaType> summaryModel) {
            return new ObjectVerticalSummaryPanel<>(id, summaryModel) {
                @Override
                protected IModel<String> getTitleForNewObject(SchemaType modelObject) {
                    return createStringResource("PageSchema.new");
                }
            };
    }

    public CreateComplexOrEnumerationWizardPanel showComplexOrEnumerationTypeWizard(AjaxRequestTarget target) {
        return showWizard(
                target,
                WebPrismUtil.PRISM_SCHEMA,
                CreateComplexOrEnumerationWizardPanel.class);
    }

    @Override
    protected boolean canShowWizard() {
        return isAdd();
    }

    @Override
    protected DetailsFragment createWizardFragment() {
        getObjectDetailsModels().reset();
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageSchema.this) {
            @Override
            protected void initFragmentLayout() {
                add(new SchemaWizardPanel(ID_TEMPLATE, createObjectWizardPanelHelper()));
            }
        };
    }

    @Override
    protected InlineOperationalButtonsPanel<SchemaType> createInlineButtonsPanel(String id, LoadableModel<PrismObjectWrapper<SchemaType>> wrapperModel) {
        return new InlineOperationalButtonsPanel<>(id, wrapperModel) {
            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageSchema.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageSchema.this.hasUnsavedChanges(target);
            }

            @Override
            protected boolean isDeleteButtonVisible() {
                return false;
            }

            @Override
            protected IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<SchemaType> modelObject) {
                return Model.of();
            }

            @Override
            protected IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<SchemaType> modelObject) {
                return createStringResource("PageSchema.save");
            }

            @Override
            protected IModel<String> getTitle() {
                return getPageTitleModel();
            }
        };
    }

    @Override
    protected boolean supportGenericRepository() {
        return false;
    }

    @Override
    protected boolean supportNewDetailsLook() {
        return true;
    }

    @Override
    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected String getMainPanelCssClass() {
        return "col p-0 card";
    }
}
