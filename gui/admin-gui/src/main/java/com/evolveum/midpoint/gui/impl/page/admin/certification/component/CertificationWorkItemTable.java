/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.values;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.component.action.CertItemResolveAction;

import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationGuiConfigContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CertificationWorkItemTable extends ContainerableListPanel<AccessCertificationWorkItemType, PrismContainerValueWrapper<AccessCertificationWorkItemType>> {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationWorkItemTable.class);

    private static final String DOT_CLASS = CertificationWorkItemTable.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCESS_CERT_DEFINITION = DOT_CLASS + "loadAccessCertificationDefinition";
    private static final String OPERATION_LOAD_CERTIFICATION_CONFIG = DOT_CLASS + "loadCertificationConfiguration";
    private static final String OPERATION_LOAD_MULTISELECT_CONFIG = DOT_CLASS + "loadMultiselectConfig";

    public CertificationWorkItemTable(String id) {
        super(id, AccessCertificationWorkItemType.class);
    }

    public CertificationWorkItemTable(String id, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationWorkItemType.class, configurationType);
    }

    @Override
    protected boolean useNewColumnConfiguration() {
        return true;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> getPredefinedColumns() {
        return createColumns();
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<AccessCertificationWorkItemType>> createProvider() {
        return CertificationWorkItemTable.this.createProvider(getSearchModel());
    }

    @Override
    public List<AccessCertificationWorkItemType> getSelectedRealObjects() {
        List<PrismContainerValueWrapper<AccessCertificationWorkItemType>> selectedObjects = getSelectedObjects();
        return selectedObjects.stream().map(PrismValueWrapper::getRealValue).collect(Collectors.toList());
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCheckboxColumn() {
        if (!isPreview()) {
            try {
                OperationResult result = new OperationResult(OPERATION_LOAD_MULTISELECT_CONFIG);
                var accessCertConfig = getPageBase().getModelInteractionService().getCertificationConfiguration(result);
                if (accessCertConfig == null) {
                    return new CheckBoxColumn<>(null);
                }
                MultiselectOptionType multiselect = accessCertConfig.getMultiselect();
                if (multiselect == null) {
                    return new CheckBoxColumn<>(null);
                }
                return switch (multiselect) {
                    case NO_SELECT -> null;
                    case SELECT_ALL -> new CheckBoxHeaderColumn<>();
                    case SELECT_INDIVIDUAL_ITEMS -> new CheckBoxColumn<>(null);
                };
            } catch (Exception e) {
                LOGGER.error("Couldn't load multiselect configuration for certification items", e);
                return new CheckBoxHeaderColumn<>();
            }
        }
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createIconColumn() {
        return new IconColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(
                        IconAndStylesUtil.createDefaultBlackIcon(AccessCertificationWorkItemType.COMPLEX_TYPE));
            }

        };
    }

    private List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createColumns() {
        CertificationGuiConfigContext context = new CertificationGuiConfigContext();
        context.setViewAllItems(!isMyCertItems());
        context.setNotDecidedOnly(showOnlyNotDecidedItems());
        context.setPageBase(getPageBase());

        return CertMiscUtil.createCertItemsColumns(getObjectCollectionView(), context);//ColumnUtils.getDefaultCertWorkItemColumns(!isMyCertItems(), showOnlyNotDecidedItems());
    }

    private List<AbstractGuiAction<AccessCertificationWorkItemType>> getCertItemActions() {
        List<AccessCertificationResponseType> availableResponses = new AvailableResponses(getPageBase()).getResponseValues();   //from sys config
        if (CollectionUtils.isEmpty(availableResponses)) {
            availableResponses = Arrays.stream(values()).filter(r -> r != DELEGATE).collect(Collectors.toList());
        }
        List<GuiActionType> actions = getCertItemsViewActions();
        List<AbstractGuiAction<AccessCertificationWorkItemType>> actionsList =
                CertMiscUtil.mergeCertItemsResponses(availableResponses, actions, getPageBase());
        resetAvailableResponses(availableResponses, actionsList);
        return actionsList
                .stream()
                .sorted(Comparator.comparingInt(AbstractGuiAction::getOrder))
                .toList();
    }

    //hack for Resolve item and Change decision actions; they should contain configured responses as well
    private void resetAvailableResponses(List<AccessCertificationResponseType> availableResponses,
            List<AbstractGuiAction<AccessCertificationWorkItemType>> actionsList) {
        for (AbstractGuiAction<AccessCertificationWorkItemType> action : actionsList) {
            if (action instanceof CertItemResolveAction) {
                ((CertItemResolveAction) action).setConfiguredResponses(availableResponses);
            }
        }
    }

    private List<GuiActionType> getCertItemsViewActions() {
        CompiledObjectCollectionView collectionView = getObjectCollectionView();
        return collectionView == null ? new ArrayList<>() : collectionView.getActions();
    }

    @Override
    public CompiledObjectCollectionView getObjectCollectionView() {
        return loadCampaignView();
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createActionsColumn() {
        List<AbstractGuiAction<AccessCertificationWorkItemType>> actions = getCertItemActions();
        if (CollectionUtils.isNotEmpty(actions)) {
            return new GuiActionColumn<>(actions) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected AccessCertificationWorkItemType unwrapRowModelObject(
                        PrismContainerValueWrapper<AccessCertificationWorkItemType> rowModelObject) {
                    return rowModelObject.getRealValue();
                }

                @Override
                protected List<AccessCertificationWorkItemType> getSelectedItems() {
                    return getSelectedRealObjects();
                }
            };
        }
        return null;
    }

//    @Override
//    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCustomExportableColumn(
//            IModel<String> displayModel, GuiObjectColumnType guiObjectColumn, ExpressionType expression) {
//        ItemPath path = WebComponentUtil.getPath(guiObjectColumn);
//
//        if (ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_COMMENT)
//                .equivalent(path)) {
//            String propertyExpression = "realValue" + "." + AccessCertificationWorkItemType.F_OUTPUT.getLocalPart() + "."
//                    + AbstractWorkItemOutputType.F_COMMENT.getLocalPart();
//            return new DirectlyEditablePropertyColumn<>(
//                    createStringResource("PageCertDecisions.table.comment"), propertyExpression) {
//                @Serial private static final long serialVersionUID = 1L;
//
//                @Override
//                public void onBlur(AjaxRequestTarget target,
//                        IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> model) {
//                    recordCommentPerformed(target, model.getObject());
//                }
//            };
//        }
//        return super.createCustomExportableColumn(displayModel, guiObjectColumn, expression);
//    }

    @Override
    protected boolean shouldCheckForNameColumn() {
        return false;
    }


    private ContainerListDataProvider<AccessCertificationWorkItemType> createProvider(IModel<Search<AccessCertificationWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getOpenCertWorkItemsQuery();
            }

        };
//        provider.setSort(CaseWorkItemType.F_DEADLINE.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    protected ObjectQuery getOpenCertWorkItemsQuery() {
        ObjectQuery query;
        MidPointPrincipal principal = null;
        if (isMyCertItems()) {
            principal = getPageBase().getPrincipal();
        }
        if (StringUtils.isNotEmpty(getCampaignOid())) {
            query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(Collections.singletonList(getCampaignOid()),
                    principal, false);
        } else {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .build();
        }
        return QueryUtils.createQueryForOpenWorkItems(query, principal, false);
    }

    protected boolean isMyCertItems() {
        return true;
    }

    protected boolean showOnlyNotDecidedItems() {
        return false;
    }

    protected String getCampaignOid() {
        return OnePageParameterEncoder.getParameter(getPageBase());
    }

    private CompiledObjectCollectionView loadCampaignView() {
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ACCESS_CERT_DEFINITION);
        OperationResult result = task.getResult();

        GuiObjectListViewType campaignDefinitionView = getCollectionViewConfigurationFromCampaignDefinition(task, result);

        GuiObjectListViewType defaultView = null;
        try {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_CERTIFICATION_CONFIG);
            var certificationConfig = getPageBase().getModelInteractionService().getCertificationConfiguration(subResult);
            if (certificationConfig != null) {
                defaultView = certificationConfig.getDefaultView();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't load certification configuration from system configuration, ", e);
        }

        if (campaignDefinitionView == null && defaultView == null) {
            return null;
        }

        try {
            CompiledObjectCollectionView compiledView = new CompiledObjectCollectionView();
            compiledView.setContainerType(AccessCertificationWorkItemType.COMPLEX_TYPE);

            if (defaultView != null) {
                getPageBase().getModelInteractionService().compileView(compiledView, defaultView, task, result);
            }
            if (campaignDefinitionView != null) {
                getPageBase().getModelInteractionService().compileView(compiledView, campaignDefinitionView, task, result);
            }

            return compiledView;
        } catch (Exception e) {
            LOGGER.error("Couldn't load certification work items view, ", e);
        }
        return null;
    }

    private GuiObjectListViewType getCollectionViewConfigurationFromCampaignDefinition(Task task, OperationResult result) {
        String campaignOid = getCampaignOid();
        if (campaignOid == null) {
            return null;
        }
        var campaign = WebModelServiceUtils.loadObject(AccessCertificationCampaignType.class, getCampaignOid(), getPageBase(), task, result);
        if (campaign == null) {
            return null;
        }
        var definitionRef = campaign.asObjectable().getDefinitionRef();
        if (definitionRef == null) {
            return null;
        }
        PrismObject<AccessCertificationDefinitionType> definitionObj = WebModelServiceUtils.loadObject(definitionRef, getPageBase(), task, result);
        if (definitionObj == null) {
            return null;
        }
        AccessCertificationDefinitionType definition = definitionObj.asObjectable();
        return definition.getView();
    }
}
