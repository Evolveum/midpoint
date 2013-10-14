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

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.SimplePieChartDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 *
 *  @author shood
 * */
public class SimplePieChart extends SimplePanel<SimplePieChartDto>{

    private static final String ID_CHART_LABEL = "chartLabel";
    private static final String ID_PERCENT_VALUE = "percentValue";
    private SimplePieChartDto model;

    public SimplePieChart(String id){
        super(id);
        this.model = new SimplePieChartDto("", 100,1);
    }

    public SimplePieChart(String id, String label, int base, int currentValue){
        super(id);
        this.model = new SimplePieChartDto(label, base, currentValue);
    }

    @Override
    public IModel<SimplePieChartDto> createModel() {
        return new LoadableModel<SimplePieChartDto>(false) {

            @Override
            protected SimplePieChartDto load() {
                return loadPieChartInfo();
            }
        };
    }

    private SimplePieChartDto loadPieChartInfo(){
        return this.model;
    }

    @Override
    protected void initLayout(){
        Label graphLabel = new Label(ID_CHART_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject(){
                SimplePieChartDto dto = getModel().getObject();
                return dto.getLabel();
            }
        });
        add(graphLabel);

        Label percentLabel = new Label(ID_PERCENT_VALUE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject(){
                SimplePieChartDto dto = getModel().getObject();
                return Integer.toString(dto.getPercent());
            }
        });
        add(percentLabel);
    }


}
