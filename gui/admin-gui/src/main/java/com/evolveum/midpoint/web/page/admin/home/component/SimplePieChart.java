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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.*;

/**
 *
 *  @author shood
 * */
public class SimplePieChart extends SimplePanel<SimplePieChartDto>{


    private static final String ID_CHART_LABEL = "chartLabel";
    private static final String ID_PERCENT_VALUE = "percentValue";

    public SimplePieChart(String id, IModel<SimplePieChartDto> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){
        Label graphLabel = new Label(ID_CHART_LABEL, new AbstractReadOnlyModel<String>(){

            @Override
            public String getObject(){
                return createStringResource(getModel().getObject().getLabel()).getString();
            }
        });
        add(graphLabel);

        Label percentLabel = new Label(ID_PERCENT_VALUE, new PropertyModel<Integer>(getModel(), "percent"));
        add(percentLabel);
    }
}
