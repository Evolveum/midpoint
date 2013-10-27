/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.SimplePieChartDto;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author shood
 */
public class SimplePieChart extends SimplePanel<SimplePieChartDto> {


    private static final String ID_CHART = "chart";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";
    private static final String ID_UNIT = "unit";

    public SimplePieChart(String id, IModel<SimplePieChartDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer chart = new WebMarkupContainer(ID_CHART);
        chart.setOutputMarkupId(true);
        chart.add(AttributeModifier.replace("data-percent", new PropertyModel(getModel(), "percent")));
        add(chart);

        Label value = new Label(ID_VALUE, new PropertyModel(getModel(), "percent"));
        chart.add(value);

        Label unit = new Label(ID_UNIT, new PropertyModel<Object>(getModel(), SimplePieChartDto.F_UNIT));
        chart.add(unit);

        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource(getModel().getObject().getLabel()).getString();
            }
        });
        add(label);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initPieChart('" + get(ID_CHART).getMarkupId() + "');");
        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }
}
