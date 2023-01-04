/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;

import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.commons.lang3.StringUtils;
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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallBox;
import com.evolveum.midpoint.gui.impl.component.box.SmallBoxData;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULT_URL,
                        label = "PageSimulationResults.auth.simulationResult.label",
                        description = "PageSimulationResults.auth.simulationResult.description")
        }
)
public class PageSimulationResult extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResult.class);

    private static final String DOT_CLASS = PageSimulationResult.class.getName() + ".";

    private static final String ID_WIDGETS = "widgets";

    private static final String ID_WIDGET = "widget";
    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    private IModel<SimulationResultType> model;

    public PageSimulationResult() {
        this(new PageParameters());
    }

    public PageSimulationResult(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        model = new LoadableDetachableModel<>() {
            @Override
            protected SimulationResultType load() {
                String oid = OnePageParameterEncoder.getParameter(PageSimulationResult.this);
                if (StringUtils.isEmpty(oid)) {
                    throw new RestartResponseException(PageError404.class);
                }

                Task task = getPageTask();
                PrismObject<SimulationResultType> object = WebModelServiceUtils.loadObject(SimulationResultType.class, oid, PageSimulationResult.this, task, task.getResult());
                if (object == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                return object.asObjectable();
            }
        };
    }

    private void initLayout() {
        // todo implement
        IModel<List<SmallBoxData>> data = () -> {

            List<SimulationMetricType> metrics = model.getObject().getMetric();
            return metrics.stream().map(m -> {
                SmallBoxData sbd = new SmallBoxData();
                sbd.setDescription(m.getIdentifier());
                sbd.setTitle(m.getMatchedObjects().longValue() + "/" + m.getProcessedObjects().longValue());
                sbd.setSmallBoxCssClass("bg-info");
                sbd.setLinkText("More info");
                sbd.setIcon("fa fa-database");

                return sbd;
            }).collect(Collectors.toList());
        };

        ListView<SmallBoxData> widgets = new ListView<>(ID_WIDGETS, data) {

            @Override
            protected void populateItem(ListItem<SmallBoxData> item) {
                item.add(new SmallBox(ID_WIDGET, item.getModel()) {

                    @Override
                    protected boolean isLinkVisible() {
                        return true;
                    }

                    @Override
                    protected void onClickLink(AjaxRequestTarget target) {
                        onWidgetClick(target, getModelObject());
                    }
                });
            }
        };
        add(widgets);

        MidpointForm form = new MidpointForm(ID_FORM);
        add(form);

        IModel<Search<TagType>> search = new LoadableModel<>(false) {

            @Override
            protected Search<TagType> load() {
                return SearchFactory.createSearch(TagType.class, PageSimulationResult.this);
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
