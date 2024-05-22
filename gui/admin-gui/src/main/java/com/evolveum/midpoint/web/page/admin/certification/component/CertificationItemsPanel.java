/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertWorkItemDto;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

public class CertificationItemsPanel extends ContainerableListPanel<AccessCertificationWorkItemType,
        PrismContainerValueWrapper<AccessCertificationWorkItemType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CertificationItemsPanel.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    public CertificationItemsPanel(String id) {
        super(id, AccessCertificationWorkItemType.class);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createDefaultColumns() {
        return new ArrayList<>(); // TODO
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        if (!isPreview()) {
            return createRowActions();
        }
        return null;
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<AccessCertificationWorkItemType>> createProvider() {
        return CertificationItemsPanel.this.createProvider(getSearchModel());
    }

    @Override
    public List<AccessCertificationWorkItemType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(PrismValueWrapper::getRealValue).collect(Collectors.toList());
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createCheckboxColumn() {
        if (!isPreview()) {
            return new CheckBoxHeaderColumn<>();
        }
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String> createIconColumn() {
        return new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(
                        IconAndStylesUtil.createDefaultBlackIcon(AccessCertificationWorkItemType.COMPLEX_TYPE));
            }

        };
    }


    @Override
    protected String getStorageKey() {
        return SessionStorage.KEY_WORK_ITEMS;
    }

    private ContainerListDataProvider<AccessCertificationWorkItemType> createProvider(IModel<Search<AccessCertificationWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CertificationItemsPanel.this.getPageBase()
                .getOperationOptionsBuilder()
                .resolveNames()
                .build();
        ContainerListDataProvider<AccessCertificationWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getCertDecisions();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getCertWorkItemsQuery();
            }

        };
//        provider.setSort(CaseWorkItemType.F_DEADLINE.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    private List<InlineMenuItem> createRowActions() {
        final AvailableResponses availableResponses = new AvailableResponses(getPageBase());
        List<InlineMenuItem> items = new ArrayList<>();
        if (availableResponses.isAvailable(ACCEPT)) {
            items.add(createMenu("PageCertDecisions.menu.acceptSelected", ACCEPT));
        }
        if (availableResponses.isAvailable(REVOKE)) {
            items.add(createMenu("PageCertDecisions.menu.revokeSelected", REVOKE));
        }
        if (availableResponses.isAvailable(REDUCE)) {
            items.add(createMenu("PageCertDecisions.menu.reduceSelected", REDUCE));
        }
        if (availableResponses.isAvailable(NOT_DECIDED)) {
            items.add(createMenu("PageCertDecisions.menu.notDecidedSelected", NOT_DECIDED));
        }
        if (availableResponses.isAvailable(NO_RESPONSE)) {
            items.add(createMenu("PageCertDecisions.menu.noResponseSelected", NO_RESPONSE));
        }
        return items;
    }

    private InlineMenuItem createMenu(String titleKey, final AccessCertificationResponseType response) {
        return new InlineMenuItem(createStringResource(titleKey)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new HeaderMenuAction(CertificationItemsPanel.this) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recordActionOnSelected(response, new ArrayList<>(), "", target);
                    }
                };
            }
        };
    }

    private void recordActionOnSelected(AccessCertificationResponseType response, List<AccessCertificationWorkItemType> items,
            String comment, AjaxRequestTarget target) {
        if (CollectionUtils.isNotEmpty(items)) {
            warn(getString("PageCertDecisions.message.noItemSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
        Task task = getPageBase().createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
        items.stream().forEach(item -> {
            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
            try {
                AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(item);
                AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
                getPageBase().getCertificationService().recordDecision(
                        campaign.getOid(),
                        certCase.getId(), item.getId(),
                        response, comment, task, resultOne);
            } catch (Exception ex) {
                resultOne.recordFatalError(ex);
            } finally {
                resultOne.computeStatusIfUnknown();
            }
        });
        result.computeStatus();

        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(getFeedbackPanel());
        refreshTable(target);
    }

    private ObjectQuery getCertWorkItemsQuery() {
        String campaignOid = getCampaignOid();
        ObjectQuery query;
        if (StringUtils.isNotEmpty(campaignOid)) {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .ownedBy(AccessCertificationCaseType.class, AccessCertificationCaseType.F_WORK_ITEM)
                    .id(campaignOid)
                    .build();
        } else {
            query = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .build();
        }
        MidPointPrincipal principal = null;
        if (isMyCertItems()) {
            principal = getPageBase().getPrincipal();
        }
        return  QueryUtils.createQueryForOpenWorkItems(query, principal, true); //todo notDecidedOnly
    }

    protected String getCampaignOid() {
        return null;
    }

    protected boolean isMyCertItems() {
        return false;
    }

}
