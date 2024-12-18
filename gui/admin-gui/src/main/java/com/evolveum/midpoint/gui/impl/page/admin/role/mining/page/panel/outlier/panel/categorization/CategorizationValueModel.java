package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierDashboardPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class CategorizationValueModel implements Serializable {
    int integerValue;
    double doubleValue;
    IModel<String> title;
    String textClass;
    boolean visible;
    IModel<String> helpModel;
    ProgressBar.State progressBarColor;

    public CategorizationValueModel(
            IModel<String> helpModel,
            boolean visible,
            int integerValue,
            double doubleValue,
            ProgressBar.State progressBarColor,
            IModel<String> title,
            String textClass) {
        this.helpModel = helpModel;
        this.integerValue = integerValue;
        this.doubleValue = doubleValue;
        this.title = title;
        this.textClass = textClass;
        this.progressBarColor = progressBarColor;
        this.visible = visible;
    }

    public int getIntegerValue() {
        return integerValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public IModel<String> getTitle() {
        return title;
    }

    public String getTextClass() {
        return textClass;
    }

    public IModel<String> getHelpModel() {
        return helpModel;
    }

    public ProgressBar.State getProgressBarColor() {
        return progressBarColor;
    }

    public boolean isVisible() {
        return visible;
    }

    public ProgressBar buildProgressBar() {
        return new ProgressBar(getDoubleValue(), getProgressBarColor());
    }

    protected static @NotNull List<CategorizationValueModel> prepareDistributionCatalog(
            @NotNull PageBase pageBase,
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            boolean isRoleSelected,
            boolean advanced) {
        int unPopular = itemContainer.getUnPopularCount();
        int noiseExclusive = itemContainer.getNoiseExclusiveCount();
        int anomalyExclusive = itemContainer.getAnomalyExclusiveCount();

        int total = unPopular + noiseExclusive;
        if (isRoleSelected) {
            total += anomalyExclusive;
        }

        int abovePopular = itemContainer.getAbovePopularCount();
        int noise = itemContainer.getNoiseCount();
        int anomaly = itemContainer.getAnomalyCount();
        int outlier = itemContainer.getOutlierCount();
        int excluded = itemContainer.getExcludedCount();

        int noiseAndUnpopular = 0;

        List<RoleAnalysisIdentifiedCharacteristicsItemType> items = itemContainer.getItem();

        for (RoleAnalysisIdentifiedCharacteristicsItemType item : items) {
            List<RoleAnalysisObjectCategorizationType> category = item.getCategory();
            if (new HashSet<>(category).containsAll(
                    List.of(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE, RoleAnalysisObjectCategorizationType.UN_POPULAR))) {
                noiseAndUnpopular++;
            }
        }

        if (advanced) {
            total += abovePopular + noise + anomaly + outlier + excluded;
        }

        List<CategorizationValueModel> progressValues = new ArrayList<>();
        progressValues.add(new CategorizationValueModel(
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.noise_exclusive"),
                true, noiseExclusive, calculatePercentage(noiseExclusive, total),
                ProgressBar.State.WARNING,
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.noise_exclusive"),
                "text-warning"));
        progressValues.add(new CategorizationValueModel(
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.un_popular"),
                true, unPopular, calculatePercentage(unPopular, total),
                ProgressBar.State.DANGER,
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.un_popular"),
                "text-danger"));
        if (isRoleSelected) {
            progressValues.add(new CategorizationValueModel(
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.anomaly_exclusive"),
                    true, anomalyExclusive, calculatePercentage(anomalyExclusive, total),
                    ProgressBar.State.DARK,
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.anomaly_exclusive"),
                    "text-dark"));
        }

        progressValues.add(new CategorizationValueModel(
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.help"),
                true, noiseAndUnpopular, calculatePercentage(noiseAndUnpopular, total),
                ProgressBar.State.SECONDARY,
                pageBase.createStringResource("RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular"),
                "text-secondary"));

        if (advanced) {
            progressValues.add(new CategorizationValueModel(
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.noise"),
                    true, noise, calculatePercentage(noise, total),
                    ProgressBar.State.INFO,
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.noise"),
                    "text-info"));

            progressValues.add(new CategorizationValueModel(
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.excluded"),
                    true, excluded, calculatePercentage(excluded, total),
                    ProgressBar.State.SUCCESS,
                    pageBase.createStringResource("RoleAnalysisObjectCategorizationType.excluded"),
                    "text-success"));

            if (abovePopular > 0) {
                progressValues.add(new CategorizationValueModel(
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.above_popular"),
                        true, abovePopular, calculatePercentage(abovePopular, total),
                        ProgressBar.State.LIGHT,
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.above_popular"),
                        "text-light"));
            }

            if (isRoleSelected) {
                progressValues.add(new CategorizationValueModel(
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.anomaly"),
                        true, anomaly, calculatePercentage(anomaly, total),
                        ProgressBar.State.PRIMARY,
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.anomaly"),
                        "text-primary"));
            }

            if (!isRoleSelected) {
                progressValues.add(new CategorizationValueModel(
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.help.outlier"),
                        true, outlier, calculatePercentage(outlier, total),
                        ProgressBar.State.PRIMARY,
                        pageBase.createStringResource("RoleAnalysisObjectCategorizationType.outlier"),
                        "text-primary"));
            }

        }

        progressValues.sort(Comparator.comparing(CategorizationValueModel::getDoubleValue).reversed());
        return progressValues;
    }

    protected static @NotNull WebMarkupContainer buildDistributionRolePanel
            (@NotNull PageBase pageBase,
                    boolean isRoleSelected,
                    @NotNull RoleAnalysisSessionType session,
                    String panelId,
                    boolean advanced,
                    String title,
                    Class<?> typeClass) {

        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = session.getIdentifiedCharacteristics();
        if (identifiedCharacteristics == null || identifiedCharacteristics.getRoles() == null) {
            return new WebMarkupContainer(panelId);
        }

        RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer = identifiedCharacteristics.getRoles();
        if (typeClass.equals(UserType.class)) {
            itemContainer = identifiedCharacteristics.getUsers();
        }

        List<RoleAnalysisIdentifiedCharacteristicsItemType> items = itemContainer.getItem();

        if (items == null) {
            return new WebMarkupContainer(panelId);
        }

        List<CategorizationValueModel> progressValueModels = CategorizationValueModel
                .prepareDistributionCatalog(pageBase, itemContainer, isRoleSelected, advanced);

        RoleAnalysisOutlierDashboardPanel<?> distributionHeader = new RoleAnalysisOutlierDashboardPanel<>(panelId,
                pageBase.createStringResource(title)) {
            @Contract(pure = true)
            @Override
            protected @NotNull String getIconCssClass() {
                return "fa fa-lock";
            }

            @Override
            protected boolean isFooterVisible() {
                return false;
            }

            @Override
            protected @NotNull Component getPanelComponent(String id) {
                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {

                    @Override
                    protected @NotNull Component getPanelComponent(String id) {
                        ProgressBarPanel components = new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                List<ProgressBar> progressBars = new ArrayList<>();
                                for (CategorizationValueModel progressValueModel : progressValueModels) {
                                    if (progressValueModel.isVisible()) {
                                        progressBars.add(progressValueModel.buildProgressBar());
                                    }
                                }
                                return progressBars;
                            }
                        });
                        components.setOutputMarkupId(true);
                        return components;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getContainerLegendCssClass() {
                        return "d-flex flex-wrap justify-content-between pt-2 pb-0 px-0";
                    }

                    @Override
                    protected @NotNull Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);

                        for (CategorizationValueModel valueModel : progressValueModels) {
                            if (!valueModel.isVisible()) {
                                continue;
                            }
                            MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                                @Contract("_ -> new")
                                @Override
                                protected @NotNull Component getTitleComponent(String id) {
                                    return new IconWithLabel(id, valueModel.getTitle()) {
                                        @Override
                                        protected String getIconCssClass() {
                                            return "fa fa-circle fa-2xs align-middle " + valueModel.getTextClass();
                                        }

                                        @Override
                                        protected String getIconComponentCssStyle() {
                                            return "font-size:8px;margin-bottom:2px;";
                                        }

                                        @Override
                                        protected String getLabelComponentCssClass() {
                                            return "txt-toned";
                                        }

                                        @Override
                                        protected String getComponentCssClass() {
                                            return super.getComponentCssClass() + " gap-2";
                                        }
                                    };
                                }

                                @Contract("_ -> new")
                                @Override
                                protected @NotNull Component getValueComponent(String id) {
                                    Label label = new Label(id, valueModel.getIntegerValue());
                                    label.add(AttributeModifier.append(CLASS_CSS, "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                    label.add(AttributeModifier.append(STYLE_CSS, "font-size:18px"));
                                    return label;
                                }

                                @Override
                                protected IModel<String> getHelpModel() {
                                    return valueModel.getHelpModel();
                                }
                            };
                            resolved.setOutputMarkupId(true);
                            view.add(resolved);
                        }

                        return view;

                    }
                };
                panel.setOutputMarkupId(true);
                panel.add(AttributeModifier.append(CLASS_CSS, "col-12"));
                return panel;
            }
        };

        distributionHeader.setOutputMarkupId(true);
        return distributionHeader;

    }

    private static double calculatePercentage(int value, int total) {
        if (total == 0) {
            return 0;
        }
        if (value == 0) {
            return 0;
        }
        return value * 100 / (double) total;
    }
}
