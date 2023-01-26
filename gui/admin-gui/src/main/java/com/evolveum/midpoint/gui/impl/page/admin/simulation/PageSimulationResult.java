/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil.getDisplayableIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallBoxData;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

import org.jetbrains.annotations.NotNull;

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

    private static final String ID_WIDGETS = "widgets";

    private static final String ID_WIDGET = "widget";
    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    private IModel<SimulationResultType> resultModel;

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
                String oid = getPageParameterResultOid();
                if (!Utils.isPrismObjectOidValid(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                Task task = getPageTask();
                PrismObject<SimulationResultType> object =
                        WebModelServiceUtils.loadObject(SimulationResultType.class, oid, PageSimulationResult.this, task, task.getResult());
                // todo handle error
                if (object == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                return object.asObjectable();
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
                return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                //todo implement
                setResponsePage(PageSimulationResults.class);
            }
        };
        add(navigation);

        // todo implement
        IModel<List<DashboardWidgetType>> data = () -> {

            List<SimulationMetricValuesType> metrics = resultModel.getObject().getMetric();
            return metrics.stream().map(m -> {
                DashboardWidgetType dw = new DashboardWidgetType();
                dw.setDescription(getDisplayableIdentifier(m.getRef()));
//                dw.setTitle("TODO");//todo + SimulationMetricValuesTypeUtil.getValue(m));
//                dw.setSmallBoxCssClass("bg-info");
//                dw.setLinkText("More info");
//                dw.setIcon("fa fa-database");

                return dw;
            }).collect(Collectors.toList());
        };

        ListView<DashboardWidgetType> widgets = new ListView<>(ID_WIDGETS, data) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(ID_WIDGET, item.getModel()));
            }
        };
        add(widgets);

        MidpointForm form = new MidpointForm(ID_FORM);
        add(form);

        IModel<Search<SimulationResultType>> search = new LoadableModel<>(false) {

            @Override
            protected Search<SimulationResultType> load() {
                return new SearchBuilder(SimulationResultType.class)
                        .modelServiceLocator(PageSimulationResult.this)
                        .build();
            }
        };

        // todo columnsd
        List<IColumn<SelectableBean<TagType>, String>> columns = new ArrayList<>();
        columns.add(new ObjectNameColumn<>(createStringResource("ObjectType.name")));
        columns.add(new PropertyColumn(createStringResource("ObjectType.description"), "value.description"));

        ISortableDataProvider<SelectableBean<TagType>, String> provider = new ObjectDataProvider<>(this, search);
        BoxedTablePanel<SelectableBean<TagType>> table = new BoxedTablePanel<>(ID_TABLE, provider, columns, UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_TAGS);
        form.add(table);
    }

    private void onWidgetClick(AjaxRequestTarget target, SmallBoxData data) {
        System.out.println();
    }
}
