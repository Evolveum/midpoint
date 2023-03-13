/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/definitions", matchUrlForSecurity = "/admin/certification/definitions")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS,
                        label = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS_DESCRIPTION)
        })
public class PageCertDefinitions extends PageAdminWorkItems {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = PageCertDefinitions.class.getName() + ".";
    private static final String OPERATION_CREATE_CAMPAIGN = DOT_CLASS + "createCampaign";
    private static final String OPERATION_DELETE_DEFINITION = DOT_CLASS + "deleteDefinition";

    private static final Trace LOGGER = TraceManager.getTrace(PageCertDefinitions.class);

    private AccessCertificationDefinitionType singleDelete;

    public PageCertDefinitions() {
        initLayout();
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<AccessCertificationDefinitionType> mainPanel =
                new MainObjectListPanel<>(
                        ID_TABLE, AccessCertificationDefinitionType.class, null) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_CERT_DEFINITIONS_PANEL;
                    }

                    @Override
                    protected IColumn<SelectableBean<AccessCertificationDefinitionType>, String> createCheckboxColumn() {
                        return null;
                    }

                    @Override
                    public void objectDetailsPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType service) {
                        PageCertDefinitions.this.detailsPerformed(service);
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return PageCertDefinitions.this.createInlineMenu();
                    }

                    @Override
                    protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                        navigateToNext(PageCertDefinition.class);
                    }
                };
        mainPanel.setOutputMarkupId(true);
        mainPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_CERT_DEF_BOX_CSS_CLASSES);
        mainForm.add(mainPanel);
    }

    private MainObjectListPanel<AccessCertificationDefinitionType> getDefinitionsTable() {
        return (MainObjectListPanel<AccessCertificationDefinitionType>)
                get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private IModel<String> createDeleteConfirmString() {
        return () -> {
            if (singleDelete == null) {
                return "";
            } else {
                return createStringResource("PageCertDefinitions.deleteDefinitionConfirmSingle", singleDelete.getName()).getString();
            }
        };
    }

    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new ButtonInlineMenuItem(createStringResource("PageCertDefinitions.button.createCampaign")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<AccessCertificationDefinitionType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AccessCertificationDefinitionType campaign = getRowModel().getObject().getValue();
                        createCampaignPerformed(target, campaign);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_START_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
        menu.add(new ButtonInlineMenuItem(createStringResource("PageCertDefinitions.button.showCampaigns")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<AccessCertificationDefinitionType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AccessCertificationDefinitionType campaign = getRowModel().getObject().getValue();
                        showCampaignsPerformed(campaign);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.ICON_FAR_COPY);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
        menu.add(new ButtonInlineMenuItem(createStringResource("PageCertDefinitions.button.deleteDefinition")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<AccessCertificationDefinitionType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AccessCertificationDefinitionType campaign = getRowModel().getObject().getValue();
                        deleteConfirmation(target, campaign);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
        return menu;
    }

    protected void detailsPerformed(AccessCertificationDefinitionType service) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
        navigateToNext(PageCertDefinition.class, parameters);
    }

    private void showCampaignsPerformed(AccessCertificationDefinitionType definition) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, definition.getOid());
        navigateToNext(PageCertCampaigns.class, parameters);
    }

    private void createCampaignPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
        LOGGER.debug("Create certification campaign performed for {}", definition.asPrismObject());

        OperationResult result = new OperationResult(OPERATION_CREATE_CAMPAIGN);
        try {
            Task task = createSimpleTask(OPERATION_CREATE_CAMPAIGN);
            if (!Boolean.TRUE.equals(definition.isAdHoc())) {
                AccessCertificationCampaignType campaign = getCertificationService()
                        .createCampaign(definition.getOid(), task, result);
                result.setUserFriendlyMessage(
                        new LocalizableMessageBuilder()
                                .key("PageCertDefinitions.campaignWasCreated")
                                .arg(getOrig(campaign.getName()))
                                .build());
            } else {
                result.recordWarning(createStringResource("PageCertDefinitions.message.createCampaignPerformed.warning", definition.getName()).getString());
            }
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void deleteConfirmation(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {

        this.singleDelete = definition;
        showMainPopup(getDeleteDefinitionConfirmationPanel(),
                target);
    }

    private void deleteDefinitionPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
        OperationResult result = new OperationResult(OPERATION_DELETE_DEFINITION);
        try {
            Task task = createSimpleTask(OPERATION_DELETE_DEFINITION);
            ObjectDelta<AccessCertificationDefinitionType> delta =
                    getPrismContext().deltaFactory().object()
                            .createDeleteDelta(AccessCertificationDefinitionType.class, definition.getOid()
                            );
            getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
        } catch (Exception ex) {
            result.recordPartialError(createStringResource("PageCertDefinitions.message.deleteDefinitionPerformed.partialError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete campaign definition", ex);
        }

        result.computeStatusIfUnknown();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageCertDefinitions.message.deleteDefinitionPerformed.success").getString());
        }

        getDefinitionsTable().clearCache();

        showResult(result);
        target.add(getFeedbackPanel(), getDefinitionsTable());
    }

    private Popupable getDeleteDefinitionConfirmationPanel() {
        return new DeleteConfirmationPanel(getMainPopupBodyId(),
                createDeleteConfirmString()) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteDefinitionPerformed(target, singleDelete);
            }
        };
    }

}
