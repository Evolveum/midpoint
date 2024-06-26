/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.PageSystemConfiguration;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

public abstract class PageBaseSystemConfiguration extends PageAssignmentHolderDetails<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageBaseSystemConfiguration.class);

    public PageBaseSystemConfiguration() {
        super();
    }

    public PageBaseSystemConfiguration(PageParameters parameters) {
        super(parameters);
    }

    public PageBaseSystemConfiguration(final PrismObject<SystemConfigurationType> object) {
        super(object);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        clearBreadcrumbs();

        IModel<String> model = createStringResource("PageAdmin.menu.top.configuration.basic");
        addBreadcrumb(new Breadcrumb(model, new Model(GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON), PageSystemConfiguration.class, null));
        createBreadcrumb();
    }

    @Override
    public Class<SystemConfigurationType> getType() {
        return SystemConfigurationType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<SystemConfigurationType> model) {
        return new ObjectSummaryPanel(id, model, getSummaryPanelSpecification()) {

            @Override
            protected String getDefaultIconCssClass() {
                return getSummaryIconCssClass();
            }

            @Override
            protected String getIconBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected String getBoxAdditionalCssClass() {
                return null;
            }

            @Override
            protected IModel<String> getDisplayNameModel() {
                return getPageTitleModel();
            }
        };
    }

    protected String getSummaryIconCssClass() {
        return PageSystemConfiguration.SubPage.getIcon(getClass());
    }

    protected IModel<String> getSummaryDisplayNameModel() {
        return getPageTitleModel();
    }

    @Override
    protected String getObjectOidParameter() {
        return SystemObjectsType.SYSTEM_CONFIGURATION.value();
    }

    @Override
    protected AssignmentHolderDetailsModel<SystemConfigurationType> createObjectDetailsModels(PrismObject<SystemConfigurationType> object) {
        return new AssignmentHolderDetailsModel<>(createPrismObjectModel(object), this) {

            @Override
            protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
                CompiledGuiProfile profile = getModelServiceLocator().getCompiledGuiProfile();
                try {
                    GuiObjectDetailsPageType defaultPageConfig = null;
                    for (Class<? extends Containerable> clazz : getAllDetailsTypes()) {
                        QName type = GuiImplUtil.getContainerableTypeName(clazz);
                        if (defaultPageConfig == null) {
                            defaultPageConfig = profile.findObjectDetailsConfiguration(type);
                        } else {
                            GuiObjectDetailsPageType anotherConfig = profile.findObjectDetailsConfiguration(type);
                            defaultPageConfig = getModelServiceLocator().getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, anotherConfig);
                        }
                    }

                    return applyArchetypePolicy(defaultPageConfig);
                } catch (Exception ex) {
                    LOGGER.error("Couldn't create default gui object details page and apply archetype policy", ex);
                }

                return null;
            }
        };
    }

    public List<Class<? extends Containerable>> getAllDetailsTypes() {
        return Arrays.asList(getDetailsType());
    }

    public Class<? extends Containerable> getDetailsType() {
        return getType();
    }

    @Override
    protected AssignmentHolderOperationalButtonsPanel<SystemConfigurationType> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<SystemConfigurationType>> wrapperModel) {
        return new AssignmentHolderOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageBaseSystemConfiguration.this.refresh(target);
            }

            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageBaseSystemConfiguration.this.savePerformed(target);
            }

            @Override
            protected boolean isDeleteButtonVisible() {
                return false;
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageBaseSystemConfiguration.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected boolean isContentVisible() {
        return true;
    }
}
