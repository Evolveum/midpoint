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
import com.evolveum.midpoint.web.page.admin.home.dto.SystemInfoDto;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 *
 *  @author shood
 * */
public class SystemInfoPanel extends SimplePanel<SystemInfoDto>{

    private static final String ID_ACTIVE_USERS = "activeUsers";
    private static final String ID_ACTIVE_TASKS = "activeTasks";
    private static final String ID_SERVER_LOAD = "serverLoad";
    private static final String ID_RAM_USAGE = "usedRam";

    public SystemInfoPanel(String id, IModel<SystemInfoDto> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){

        SimplePieChart userPanel = new SimplePieChart(ID_ACTIVE_USERS, new PropertyModel<SimplePieChartDto>(getModel(), "activeUsersDto"));
        add(userPanel);
        SimplePieChart tasksPanel = new SimplePieChart(ID_ACTIVE_TASKS, new PropertyModel<SimplePieChartDto>(getModel(), "activeTasksDto"));
        add(tasksPanel);
        SimplePieChart serverLoadPanel = new SimplePieChart(ID_SERVER_LOAD, new PropertyModel<SimplePieChartDto>(getModel(), "serverLoadDto"));
        add(serverLoadPanel);
        SimplePieChart ramPanel = new SimplePieChart(ID_RAM_USAGE, new PropertyModel<SimplePieChartDto>(getModel(), "usedRamDto"));
        add(ramPanel);
    }
}
