/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.buildSimpleDensityBasedProgressBar;

import java.text.DecimalFormat;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisClusterOccupationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisSettingsUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

public class RoleAnalysisClusterSummaryPanel extends ObjectVerticalSummaryPanel<RoleAnalysisClusterType> {

    public RoleAnalysisClusterSummaryPanel(String id, IModel<RoleAnalysisClusterType> model) {
        super(id, model);
    }

    @Override
    protected IModel<String> getTitleForNewObject(RoleAnalysisClusterType modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisClusterSummaryPanel.new");
    }

    @Override
    protected @NotNull IModel<List<DetailsTableItem>> createDetailsItems() {
        AnalysisClusterStatisticType clusterStatistics = getModelObject().getClusterStatistics();

        Double density = clusterStatistics.getMembershipDensity();
        if (density == null) {
            density = 0.0;
        }

        String formattedDensity = new DecimalFormat("#.###")
                .format(Math.round(density * 1000.0) / 1000.0);

        Integer rolesCount = clusterStatistics.getRolesCount();
        Integer usersCount = clusterStatistics.getUsersCount();

        Double membershipMean = clusterStatistics.getMembershipMean();
        String formattedMembershipMean = new DecimalFormat("#.###")
                .format(Math.round(membershipMean * 1000.0) / 1000.0);

        Double detectedReductionMetric = clusterStatistics.getDetectedReductionMetric();
        String formattedDetectedReductionMetric = new DecimalFormat("#.###")
                .format(Math.round(detectedReductionMetric * 1000.0) / 1000.0);

        List<DetailsTableItem> detailsModel = List.of(

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisClusterSummaryPanel.details.table.analysis.type.title"),
                        () -> {
                            PrismObject<RoleAnalysisSessionType> roleAnalysisSession = getRoleAnalysisSession();
                            if (roleAnalysisSession == null) {
                                return "N/A";
                            }

                            RoleAnalysisOptionType analysisOption = roleAnalysisSession.asObjectable().getAnalysisOption();
                            return RoleAnalysisSettingsUtil.getRoleAnalysisTypeMode(analysisOption);
                        }) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisClusterSummaryPanel.details.table.mode.title"),
                        () -> {
                            //TODO really necessary?
                            PrismObject<RoleAnalysisSessionType> roleAnalysisSession = getRoleAnalysisSession();
                            if (roleAnalysisSession == null) {
                                return "N/A";
                            }

                            RoleAnalysisOptionType analysisOption = roleAnalysisSession.asObjectable().getAnalysisOption();
                            return RoleAnalysisSettingsUtil.getRoleAnalysisMode(analysisOption);
                        }) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.max.reduction.title"),
                        Model.of(formattedDetectedReductionMetric)) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
                                return "fa fa-arrow-down";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.membership.mean.title"),
                        Model.of(formattedMembershipMean)) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
                                return "fe fe-assignment";
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };
                    }
                },
                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.occupation.title"),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {

                        IModel<String> roleObjectCount = Model.of(rolesCount.toString());
                        IModel<String> userObjectCount = Model.of(usersCount.toString());

                        RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(id) {
                            @Override
                            public Component createFirstPanel(String idFirstPanel) {
                                return new IconWithLabel(idFirstPanel, userObjectCount) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fa fa-user object-user-color";
                                    }
                                };
                            }

                            @Override
                            public Component createSecondPanel(String idSecondPanel) {
                                return new IconWithLabel(idSecondPanel, roleObjectCount) {
                                    @Override
                                    public String getIconCssClass() {
                                        return "fe fe-role object-role-color d-block";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:18px!important;line-height:1;";
                                    }
                                };
                            }

                            @Override
                            public Component createSeparatorPanel(String idSeparatorPanel) {
                                Label separator = new Label(idSeparatorPanel, "/");
                                separator.add(AttributeModifier.replace("class",
                                        "d-flex align-items-center"));
                                separator.setOutputMarkupId(true);
                                add(separator);
                                return separator;
                            }

                            @Override
                            public @NotNull String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };

                        occupationPanel.setOutputMarkupId(true);
                        return occupationPanel;
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.assignment.range.title"),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        RangeType membershipRange = clusterStatistics.getMembershipRange();

                        IModel<String> min = Model.of(membershipRange.getMin().toString());
                        IModel<String> max = Model.of(membershipRange.getMax().toString());

                        RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(id) {
                            @Override
                            public Component createFirstPanel(String idFirstPanel) {
                                return new IconWithLabel(idFirstPanel, min);
                            }

                            @Override
                            public Component createSecondPanel(String idSecondPanel) {
                                return new IconWithLabel(idSecondPanel, max);
                            }

                            @Override
                            public Component createSeparatorPanel(String idSeparatorPanel) {
                                Label separator = new Label(idSeparatorPanel, "");
                                separator.add(AttributeModifier.replace("class",
                                        "d-flex align-items-center gap-3 fa-solid fa fa-arrows-h"));
                                separator.setOutputMarkupId(true);
                                add(separator);
                                return separator;
                            }

                            @Override
                            public @NotNull String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };

                        occupationPanel.setOutputMarkupId(true);
                        return occupationPanel;
                    }
                },
                new DetailsTableItem(createStringResource("Mean density"),
                        Model.of(formattedDensity)) {

                    @Override
                    public Component createValueComponent(String id) {
                        return buildSimpleDensityBasedProgressBar(id, getValue());
                    }

                });

        return Model.ofList(detailsModel);
    }

    @Override
    protected String defineDescription(RoleAnalysisClusterType object) {
        MetadataType metadata = object.getMetadata();
        if (metadata == null) {
            return "unknown";
        }
        XMLGregorianCalendar createTimestamp = metadata.getCreateTimestamp();
        if (createTimestamp != null) {
            int eonAndYear = createTimestamp.getYear();
            int month = createTimestamp.getMonth();
            int day = createTimestamp.getDay();
            String time = day + "/" + month + "/" + eonAndYear;
            return "Last rebuild: " + time;
        }
        return "unknown";
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-cluster";
    }

    private PrismObject<RoleAnalysisSessionType> getRoleAnalysisSession() {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        Task task = getPageBase().createSimpleTask("Loading session");
        OperationResult result = task.getResult();

        ObjectReferenceType roleAnalysisSessionRef = getModelObject().getRoleAnalysisSessionRef();

        return roleAnalysisService.getSessionTypeObject(roleAnalysisSessionRef.getOid(), task, result);
    }

}
