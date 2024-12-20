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

    private record CategoryData(String helpKey, int count, ProgressBar.State state, String labelKey, String cssClass) {
    }

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

        List<CategoryData> categories = collectCategoryData(itemContainer, isRoleSelected, advanced);

        int total = categories.stream().mapToInt(CategoryData::count).sum();

        List<CategorizationValueModel> progressValues = new ArrayList<>();
        for (CategoryData category : categories) {
            progressValues.add(new CategorizationValueModel(
                    pageBase.createStringResource(category.helpKey()),
                    true,
                    category.count(),
                    calculatePercentage(category.count(), total),
                    category.state(),
                    pageBase.createStringResource(category.labelKey()),
                    category.cssClass()
            ));
        }

        progressValues.sort(Comparator.comparing(CategorizationValueModel::getDoubleValue).reversed());
        return progressValues;
    }

    private static @NotNull List<CategoryData> collectCategoryData(
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            boolean isRoleSelected,
            boolean advanced) {

        List<CategoryData> categories = new ArrayList<>();

        if (isRoleSelected) {
            addRoleCategories(itemContainer, advanced, categories);
        } else {
            addUserCategories(itemContainer, advanced, categories);
        }

        return categories;
    }

    private static void addRoleCategories(
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            boolean advanced,
            @NotNull List<CategoryData> categories) {

        categories.add(new CategoryData(
                "RoleAnalysisObjectCategorizationType.noise_exclusive.role.help",
                safeCount(itemContainer.getNoiseExclusiveCount()),
                ProgressBar.State.WARNING,
                "RoleAnalysisObjectCategorizationType.noise_exclusive.role",
                "text-warning"
        ));

        categories.add(new CategoryData(
                "RoleAnalysisObjectCategorizationType.un_popular.role.help",
                safeCount(itemContainer.getUnPopularCount()),
                ProgressBar.State.DANGER,
                "RoleAnalysisObjectCategorizationType.un_popular.role",
                "text-danger"
        ));

        if (advanced) {
            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.anomaly",
                    safeCount(itemContainer.getAnomalyCount()),
                    ProgressBar.State.DARK,
                    "RoleAnalysisObjectCategorizationType.anomaly",
                    "text-dark"
            ));
            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.anomaly_exclusive",
                    safeCount(itemContainer.getOverallAnomalyCount()),
                    ProgressBar.State.SECONDARY,
                    "RoleAnalysisObjectCategorizationType.anomaly_exclusive",
                    "text-secondary"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.above_popular",
                    safeCount(itemContainer.getAbovePopularCount()),
                    ProgressBar.State.PRIMARY,
                    "RoleAnalysisObjectCategorizationType.above_popular",
                    "text-primary"
            ));
            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.role.help",
                    safeCount(itemContainer.getNoiseExclusiveUnpopular()),
                    ProgressBar.State.LIGHT,
                    "RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.role",
                    "text-light"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.noise",
                    safeCount(itemContainer.getNoiseCount()),
                    ProgressBar.State.INFO,
                    "RoleAnalysisObjectCategorizationType.noise",
                    "text-info"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.excluded",
                    safeCount(itemContainer.getExcludedCount()),
                    ProgressBar.State.SUCCESS,
                    "RoleAnalysisObjectCategorizationType.excluded",
                    "text-success"
            ));
        }
    }

    //TODO (improve localization keys) then apply getHelpKey and getCategoryKey (together with getRoleDisplayValues getUserDisplayValues)
    private static String getHelpKey(RoleAnalysisObjectCategorizationType category, boolean isRole) {
        return "RoleAnalysisObjectCategorizationType." + category + (isRole ? ".role.help" : ".user.help");
    }

    private static String getCategoryKey(RoleAnalysisObjectCategorizationType category, boolean isRole) {
        return "RoleAnalysisObjectCategorizationType." + category + (isRole ? ".role" : ".user");
    }

    private static void addUserCategories(
            @NotNull RoleAnalysisIdentifiedCharacteristicsItemsType itemContainer,
            boolean advanced,
            @NotNull List<CategoryData> categories) {

        categories.add(new CategoryData(
                "RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.user.help",
                safeCount(itemContainer.getNoiseExclusiveUnpopular()),
                ProgressBar.State.DANGER,
                "RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.user",
                "text-danger"
        ));

        categories.add(new CategoryData(
                "RoleAnalysisObjectCategorizationType.help.insufficient.peer.similarity",
                safeCount(itemContainer.getInsufficientCount()),
                ProgressBar.State.WARNING,
                "RoleAnalysisObjectCategorizationType.insufficient.peer.similarity",
                "text-warning"
        ));

        if (advanced) {
            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.outlier",
                    safeCount(itemContainer.getOutlierCount()),
                    ProgressBar.State.PRIMARY,
                    "RoleAnalysisObjectCategorizationType.outlier",
                    "text-primary"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.noise_exclusive.user",
                    safeCount(itemContainer.getNoiseExclusiveCount()),
                    ProgressBar.State.SECONDARY,
                    "RoleAnalysisObjectCategorizationType.noise_exclusive.user",
                    "text-secondary"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.un_popular.user.help",
                    safeCount(itemContainer.getUnPopularCount()),
                    ProgressBar.State.DARK,
                    "RoleAnalysisObjectCategorizationType.un_popular.user",
                    "text-dark"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.above_popular",
                    safeCount(itemContainer.getAbovePopularCount()),
                    ProgressBar.State.LIGHT,
                    "RoleAnalysisObjectCategorizationType.above_popular",
                    "text-light"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.noise",
                    safeCount(itemContainer.getNoiseCount()),
                    ProgressBar.State.INFO,
                    "RoleAnalysisObjectCategorizationType.noise",
                    "text-info"
            ));

            categories.add(new CategoryData(
                    "RoleAnalysisObjectCategorizationType.help.excluded",
                    safeCount(itemContainer.getExcludedCount()),
                    ProgressBar.State.SUCCESS,
                    "RoleAnalysisObjectCategorizationType.excluded",
                    "text-success"
            ));
        }
    }

    private static int safeCount(Integer count) {
        return count != null ? count : 0;
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
