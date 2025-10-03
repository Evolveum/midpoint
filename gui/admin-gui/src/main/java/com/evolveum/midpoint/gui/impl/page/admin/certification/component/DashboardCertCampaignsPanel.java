/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serial;
import java.util.List;

@PanelType(name = "myCertificationItems")
public class DashboardCertCampaignsPanel extends ObjectListPanel<AccessCertificationCampaignType> {

    @Serial private static final long serialVersionUID = 1L;

    public DashboardCertCampaignsPanel(String id) {
        super(id, AccessCertificationCampaignType.class);
    }

    public DashboardCertCampaignsPanel(String id, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);
    }

//    //todo cleanup; the same hack as for MyCaseWorkItemsPanel
    public DashboardCertCampaignsPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, AccessCertificationCampaignType.class, configurationType);
    }

    @Override
    protected LoadableModel<ChartedHeaderDto<DoughnutChartConfiguration>> getChartedHeaderDtoModel() {
        return new LoadableModel<>() {
            @Override
            protected ChartedHeaderDto<DoughnutChartConfiguration> load() {
                List<String> campaignsOids = CertMiscUtil.getActiveCampaignsOids(true, getPageBase());
                MidPointPrincipal principal = getPageBase().getPrincipal();
                long notDecidedCertItemsCount = CertMiscUtil.countOpenCertItems(campaignsOids, principal, true,
                        getPageBase());
                DoughnutChartConfiguration chartConfig = CertMiscUtil.createDoughnutChartConfigForCampaigns(
                        campaignsOids, principal, getPageBase());

                return new ChartedHeaderDto<>(chartConfig,
                        createStringResource("MyCertificationItemsPanel.chartTitle").getString(),
                        String.valueOf(notDecidedCertItemsCount));
            }
        };
    }



    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<AccessCertificationCampaignType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected final ISelectableDataProvider<SelectableBean<AccessCertificationCampaignType>> createProvider() {
        return createSelectableBeanObjectDataProvider(() -> CertMiscUtil.getPrincipalActiveCampaignsQuery(getPageBase()),
                null);
    }

    @Override
    protected boolean isPreview() {
        return true;
    }

    @Override
    protected List<IColumn<SelectableBean<AccessCertificationCampaignType>, String>> createDefaultColumns() {
        return ColumnUtils.getPreviewCampaignColumns(getPageBase());
    }

    @Override
    protected LoadableModel<PageParameters> getNavigationParametersModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected PageParameters load() {
                SelectableBeanObjectDataProvider<AccessCertificationCampaignType> provider =
                        (SelectableBeanObjectDataProvider) getDataProvider();
                if (provider != null && provider.getAvailableData().size() == 1) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, provider.getAvailableData().get(0).getValue().getOid());
                    return parameters;
                }
                return null;
            }
        };

    }

    @Override
    protected boolean isDataTableVisible() {
        return getDataProvider().size() > 0;
    }

}
