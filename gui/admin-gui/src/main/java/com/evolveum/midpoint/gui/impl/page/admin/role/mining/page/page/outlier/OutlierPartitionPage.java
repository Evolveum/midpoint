/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.CustomListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenu;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisOutlier/result/",
                        matchUrlForSecurity = "/admin/roleAnalysisOutlier/result/")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OUTLIERS_ALL_URL,
                        label = "OutlierPartitionPage.auth.outliersAll.label",
                        description = "OutlierPartitionPage.auth.outliersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OUTLIERS_URL,
                        label = "OutlierPartitionPage.auth.outliersAll.label",
                        description = "OutlierPartitionPage.auth.outliersAll.description")
        }
)
public class OutlierPartitionPage extends PageAdmin {

    @Serial private static final long serialVersionUID = 1L;

    public static final String PARAM_OUTLIER_OID = "outlierOid";
    public static final String PARAM_SESSION_OID = "partitionAssociation";

    private static final String ID_NAVIGATION_MENU = "navigation";
    private static final String ID_PANEL = "panel";
    private static final String ID_FORM = "form";

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private IModel<RoleAnalysisOutlierType> outlierModel;

    public IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return partitionModel;
    }

    private IModel<RoleAnalysisOutlierPartitionType> partitionModel;

    public OutlierPartitionPage() {
        this(new PageParameters());
    }

    public OutlierPartitionPage(PageParameters parameters) {
        super(parameters);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        outlierModel = new LoadableDetachableModel<>() {
            @Override
            protected RoleAnalysisOutlierType load() {
                String oid = getParameterOutlierOid();
                if (StringUtils.isEmpty(oid)) {
                    return null;
                }

                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                Task task = getPageBase().createSimpleTask("Load outlier");
                OperationResult result = new OperationResult("Load outlier");
                PrismObject<RoleAnalysisOutlierType> outlierPrismObject = roleAnalysisService.getObject(
                        RoleAnalysisOutlierType.class, oid, task, result);

                if (outlierPrismObject == null) {
                    result.recordFatalError("Couldn't load outlier with oid: " + oid);
                    getPageBase().showResult(result);
                    return null;
                }

                return outlierPrismObject.asObjectable();
            }
        };

        partitionModel = new LoadableDetachableModel<>() {
            @Override
            protected RoleAnalysisOutlierPartitionType load() {
                String oid = getParameterSessionOid();
                if (StringUtils.isEmpty(oid)) {
                    return null;
                }
                IModel<RoleAnalysisOutlierType> outlierModel = getOutlierModel();
                if (outlierModel == null || outlierModel.getObject() == null) {
                    return null;
                }
                RoleAnalysisOutlierType outlier = outlierModel.getObject();
                List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlier.getPartition();
                if (outlierPartitions == null) {
                    return null;
                }
                for (RoleAnalysisOutlierPartitionType partition : outlierPartitions) {
                    if (oid.equals(partition.getTargetSessionRef().getOid())) {
                        return partition;
                    }
                }

                return null;
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initLayout() {

        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        IModel<ListGroupMenu<?>> menuModel = new LoadableModel<>() {
            @Override
            protected ListGroupMenu<?> load() {
                ListGroupMenu<RoleAnalysisOutlierPartitionType> menu = new ListGroupMenu<>() {
                    @Override
                    public void onItemClickPerformed(ListGroupMenuItem item) {
                        super.onItemClickPerformed(item);
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String getTitle() {
                        return "OutlierGroupItemPanel.title";
                    }

                    @Override
                    public String getIconCss() {
                        return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON + "fa-xl";
                    }
                };

                initOverviewItem(menu);

                initAnomaliesItem(menu);

                initClusterItem(menu);

                initAttributesItem(menu);

                initPatternItem(menu);


                return menu;
            }
        };

        ListGroupMenuPanel<?> menu = new ListGroupMenuPanel(ID_NAVIGATION_MENU, menuModel) {

            @Override
            protected void onMenuClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                super.onMenuClickPerformed(target, item);
            }
        };
        add(menu);

        Component component = buildOutlierOverviewPanel();
        component.setOutputMarkupId(true);
        form.add(component);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    public String getParameterOutlierOid() {
        StringValue stringValue = getPage().getPageParameters().get(PARAM_OUTLIER_OID);
        if (!stringValue.isNull()) {
            return String.valueOf(stringValue.toString());
        }
        return null;
    }

    public String getParameterSessionOid() {
        StringValue stringValue = getPage().getPageParameters().get(PARAM_SESSION_OID);
        if (!stringValue.isNull()) {
            return String.valueOf(stringValue.toString());
        }
        return null;
    }

    private PageBase getPageBase() {
        return OutlierPartitionPage.this;
    }

    protected Component getPanel() {
        return get((getPageBase()).createComponentPath(ID_FORM, ID_PANEL));
    }

    private @NotNull Component buildOutlierOverviewPanel() {
        Component detailsPanel = loadOutlierResultPanel();
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private Component loadOutlierResultPanel() {
        return new RoleAnalysisPartitionOverviewPanel(OutlierPartitionPage.ID_PANEL, getPartitionModel(), getOutlierModel());
    }

    boolean isOutlierOverviewItemPanelActive = true;

    private void initOverviewItem(@NotNull ListGroupMenu<RoleAnalysisOutlierPartitionType> menu) {

        CustomListGroupMenuItem<RoleAnalysisOutlierPartitionType> outlierOverviewItemPanel = new CustomListGroupMenuItem<>(
                "PartitionOverviewItemPanel.title") {

            @Override
            public boolean isActive() {
                return isOutlierOverviewItemPanelActive;
            }

            @Contract("_, _, _ -> new")
            @Override
            public @NotNull Component createMenuItemPanel(String id,
                    IModel<ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> model,
                    SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> onClickHandler) {

                return new PartitionOverviewItemPanel<>(id, model, getPartitionModel(), getOutlierModel()) {
                    @Override
                    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
                        super.onClickPerformed(target, panelComponent);
                        onClickHandler.accept(target, model.getObject());
                        if (!isOutlierOverviewItemPanelActive) {
                            isOutlierOverviewItemPanelActive = true;
                        }
                    }
                };
            }
        };
        outlierOverviewItemPanel.setIconCss("fa-solid fa fa-line-chart");
        menu.getItems().add(outlierOverviewItemPanel);
    }

    private void initClusterItem(@NotNull ListGroupMenu<RoleAnalysisOutlierPartitionType> menu) {
        CustomListGroupMenuItem<RoleAnalysisOutlierPartitionType> outlierClusterItemPanel = new CustomListGroupMenuItem<>(
                "OutlierClusterItemPanel.title") {
            @Contract("_, _, _ -> new")
            @Override
            public @NotNull Component createMenuItemPanel(String id,
                    IModel<ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> model,
                    SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> onClickHandler) {

                return new OutlierClusterItemPanel<>(id, model, getPartitionModel(), getOutlierModel()) {
                    @Override
                    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
                        super.onClickPerformed(target, panelComponent);
                        onClickHandler.accept(target, model.getObject());
                        isOutlierOverviewItemPanelActive = false;
                    }
                };
            }
        };
        outlierClusterItemPanel.setIconCss("fa-solid fa-user-group");
        menu.getItems().add(outlierClusterItemPanel);
    }

    private void initPatternItem(@NotNull ListGroupMenu<RoleAnalysisOutlierPartitionType> menu) {
        CustomListGroupMenuItem<RoleAnalysisOutlierPartitionType> outlierPatternItemPanel = new CustomListGroupMenuItem<>(
                "OutlierPatternItemPanel.title") {
            @Contract("_, _, _ -> new")
            @Override
            public @NotNull Component createMenuItemPanel(String id,
                    IModel<ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> model,
                    SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> onClickHandler) {

                return new OutlierPatternItemPanel<>(id, model, getPartitionModel(), getOutlierModel()) {
                    @Override
                    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
                        super.onClickPerformed(target, panelComponent);
                        onClickHandler.accept(target, model.getObject());
                        isOutlierOverviewItemPanelActive = false;
                    }
                };
            }
        };
        outlierPatternItemPanel.setIconCss("fa-solid fa fa-search");
        menu.getItems().add(outlierPatternItemPanel);
    }

    private void initAnomaliesItem(@NotNull ListGroupMenu<RoleAnalysisOutlierPartitionType> menu) {
        CustomListGroupMenuItem<RoleAnalysisOutlierPartitionType> outlierAnomaliesItemPanel = new CustomListGroupMenuItem<>(
                "OutlierAnomaliesItemPanel.title") {
            @Contract("_, _, _ -> new")
            @Override
            public @NotNull Component createMenuItemPanel(String id,
                    IModel<ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> model,
                    SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> onClickHandler) {

                return new OutlierAnomaliesItemPanel<>(id, model, getPartitionModel(), getOutlierModel()) {
                    @Override
                    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
                        super.onClickPerformed(target, panelComponent);
                        onClickHandler.accept(target, model.getObject());
                        isOutlierOverviewItemPanelActive = false;
                    }
                };
            }
        };
        outlierAnomaliesItemPanel.setIconCss("fa-solid fa fa-exclamation-triangle");
        menu.getItems().add(outlierAnomaliesItemPanel);
    }

    private void initAttributesItem(@NotNull ListGroupMenu<RoleAnalysisOutlierPartitionType> menu) {
        CustomListGroupMenuItem<RoleAnalysisOutlierPartitionType> outlierAttributesItemPanel = new CustomListGroupMenuItem<>(
                "OutlierAttributeItemPanel.title") {
            @Contract("_, _, _ -> new")
            @Override
            public @NotNull Component createMenuItemPanel(String id,
                    IModel<ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> model,
                    SerializableBiConsumer<AjaxRequestTarget, ListGroupMenuItem<RoleAnalysisOutlierPartitionType>> onClickHandler) {

                return new OutlierAttributeItemPanel<>(id, model, getPartitionModel(), getOutlierModel()) {
                    @Override
                    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
                        super.onClickPerformed(target, panelComponent);
                        onClickHandler.accept(target, model.getObject());
                        isOutlierOverviewItemPanelActive = false;
                    }
                };
            }
        };
        outlierAttributesItemPanel.setIconCss("fa-solid fa fa-list");
        menu.getItems().add(outlierAttributesItemPanel);
    }

}
