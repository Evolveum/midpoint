/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULT_URL,
                        label = "PageSimulationResults.auth.simulationResult.label",
                        description = "PageSimulationResults.auth.simulationResult.description")
        }
)
public class PageSimulationResult extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResult.class);

    private static final String DOT_CLASS = PageSimulationResult.class.getName() + ".";

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";
    private static final String ID_WIDGETS = "widgets";
    private static final String ID_WIDGET = "widget";

    private IModel<SimulationResultType> resultModel;

    private IModel<TaskType> rootTaskModel;

    private IModel<List<ResultDetail>> detailsModel;

    public PageSimulationResult() {
        this(new PageParameters());
    }

    public PageSimulationResult(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {
            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResult.this);
            }
        };

        rootTaskModel = new LoadableDetachableModel<>() {
            @Override
            protected TaskType load() {
                SimulationResultType result = resultModel.getObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), PageSimulationResult.this);
                return task != null ? task.asObjectable() : null;
            }
        };

        detailsModel = new LoadableModel<>() {

            @Override
            protected List<ResultDetail> load() {
                List<ResultDetail> list = new ArrayList<>();
                list.add(new ResultDetail("PageSimulationResult.startTimestamp",
                        () -> WebComponentUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getStartTimestamp())))));
                list.add(new ResultDetail("PageSimulationResult.endTimestamp",
                        () -> WebComponentUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getEndTimestamp())))));
                list.add(new ResultDetail("PageSimulationResult.finishedIn", () -> createResultDurationText(resultModel.getObject(), PageSimulationResult.this)));
                list.add(new ResultDetail("PageSimulationResult.rootTask", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        AjaxButton link = new AjaxButton(id, () -> getTaskName()) {

                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                redirectToTaskDetails();
                            }
                        };
                        link.add(new VisibleBehaviour(() -> rootTaskModel.getObject() != null));

                        return link;
                    }
                });
                list.add(new ResultDetail("PageSimulationResult.taskStatus", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return createTaskStateLabel(id, resultModel, rootTaskModel, PageSimulationResult.this);
                    }
                });
                list.add(new ResultDetail("PageSimulationResult.productionConfiguration", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, () -> {
                            ConfigurationSpecificationType specification = resultModel.getObject().getConfigurationUsed();
                            if (specification == null || BooleanUtils.isNotFalse(specification.isProductionConfiguration())) {
                                return getString("PageSimulationResult.production");
                            }

                            return getString("PageSimulationResult.development");
                        });
                        label.add(AttributeModifier.replace("class", "badge badge-success"));
                        return label;
                    }
                });

                return list;
            }
        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return PageSimulationResult.this.createTitleModel();
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResult.this.onBackPerformed(target);
            }
        };
        add(navigation);

        ListView<ResultDetail> details = new ListView<>(ID_DETAILS, detailsModel) {

            @Override
            protected void populateItem(ListItem<ResultDetail> item) {
                item.add(new Label(ID_LABEL, () -> getString(item.getModelObject().label)));
                item.add(item.getModelObject().createValueComponent(ID_VALUE));
            }
        };
        add(details);

        IModel<List<DashboardWidgetType>> data = new LoadableDetachableModel<>() {

            @Override
            protected List<DashboardWidgetType> load() {
                List<SimulationMetricValuesType> metrics = resultModel.getObject().getMetric();
                return metrics.stream().map(m -> {

                    BigDecimal value = SimulationMetricValuesTypeUtil.getValue(m);

                    DashboardWidgetType dw = new DashboardWidgetType();
                    dw.beginData()
                            .sourceType(DashboardWidgetSourceTypeType.METRIC)
                            .metricRef(m.getRef())
                            .storedData(value.toString())
                            .end();

                    SimulationMetricReferenceType metricRef = m.getRef();
                    if (metricRef.getEventMarkRef() != null) {
                        PrismObject<MarkType> mark = WebModelServiceUtils.loadObject(metricRef.getEventMarkRef(), PageSimulationResult.this);
                        if (mark != null) {
                            DisplayType display = mark.asObjectable().getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(new PolyStringType(mark.getName()));
                            }
                            dw.setDisplay(display);
                        }
                    } else {
                        SimulationMetricDefinitionType def = getSimulationResultManager().getMetricDefinition(metricRef.getIdentifier());
                        if (def != null) {
                            DisplayType display = def.getDisplay();
                            if (display == null) {
                                display = new DisplayType();
                                display.setLabel(new PolyStringType(def.getIdentifier()));
                            }
                            dw.setDisplay(display);
                        }
                    }

                    return dw;
                }).collect(Collectors.toList());
            }
        };

        ListView<DashboardWidgetType> widgets = new ListView<>(ID_WIDGETS, data) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(ID_WIDGET, item.getModel()) {

                    @Override
                    protected void onMoreInfoPerformed(AjaxRequestTarget target) {
                        openMarkMetricPerformed(target, item.getModelObject());
                    }
                });
            }
        };

        add(widgets);
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private void openMarkMetricPerformed(AjaxRequestTarget target, DashboardWidgetType widget) {
        if (widget == null || widget.getData() == null) {
            return;
        }

        DashboardWidgetDataType data = widget.getData();
        if (data.getMetricRef() == null) {
            return;
        }

        ObjectReferenceType ref = data.getMetricRef().getEventMarkRef();
        if (ref == null || StringUtils.isEmpty(ref.getOid())) {
            return;
        }

        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());
        params.add(SimulationPage.PAGE_PARAMETER_MARK_OID, ref.getOid());

        navigateToNext(PageSimulationResultObjects.class, params);
    }

    private static class ResultDetail implements Serializable {

        String label;

        IModel<String> value;

        public ResultDetail(String label, IModel<String> value) {
            this.label = label;
            this.value = value;
        }

        public Component createValueComponent(String id) {
            Label label = new Label(id, value);
            label.setRenderBodyOnly(true);
            return label;
        }
    }

    public static String createResultDurationText(SimulationResultType result, Component panel) {
        XMLGregorianCalendar start = result.getStartTimestamp();
        if (start == null) {
            return panel.getString("SimulationResultsPanel.notStartedYet");
        }

        XMLGregorianCalendar end = result.getEndTimestamp();
        if (end == null) {
            end = MiscUtil.asXMLGregorianCalendar(new Date());
        }

        long duration = end.toGregorianCalendar().getTimeInMillis() - start.toGregorianCalendar().getTimeInMillis();
        if (duration < 0) {
            return null;
        }

        return DurationFormatUtils.formatDurationWords(duration, true, true);
    }

    public static Component createTaskStateLabel(String
            id, IModel<SimulationResultType> model, IModel<TaskType> taskModel, PageBase page) {
        IModel<TaskExecutionStateType> stateModel = () -> {
            TaskType task;
            if (taskModel != null) {
                task = taskModel.getObject();
            } else {
                SimulationResultType result = model.getObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> obj = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
                task = obj != null ? obj.asObjectable() : null;
            }

            return task != null ? task.getExecutionState() : null;
        };

        Label label = new Label(id, () -> {
            if (model.getObject().getEndTimestamp() != null) {
                return null;
            }

            TaskExecutionStateType state = stateModel.getObject();
            if (state == null) {
                return null;
            }

            return page.getString(state);
        });
        label.add(AttributeAppender.replace("class", () -> {
            TaskExecutionStateType state = stateModel.getObject();
            if (state == TaskExecutionStateType.RUNNABLE || state == TaskExecutionStateType.RUNNING) {
                return Badge.State.SUCCESS.getCss();
            }

            return Badge.State.SECONDARY.getCss();
        }));

        return label;
    }

    private String getTaskName() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return null;
        }

        return WebComponentUtil.getDisplayNameOrName(task.asPrismObject());
    }

    private void redirectToTaskDetails() {
        TaskType task = rootTaskModel.getObject();
        if (task == null) {
            return;
        }

        PageParameters params = new PageParameters();
        params.set(OnePageParameterEncoder.PARAMETER, task.getOid());
        navigateToNext(PageTask.class, params);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private IModel<String> createTitleModel() {
        return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }
}
