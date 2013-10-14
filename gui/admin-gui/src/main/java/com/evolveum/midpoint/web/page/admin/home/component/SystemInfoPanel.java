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
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimplePieChartDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SystemInfoDto;
import org.apache.wicket.model.IModel;

/**
 *
 *  @author shood
 * */
public class SystemInfoPanel extends SimplePanel<SystemInfoDto>{

    private static final String ID_ACTIVE_USERS = "activeUsers";
    private static final String ID_ACTIVE_TASKS = "activeTasks";
    private static final String ID_SERVER_LOAD = "serverLoad";
    private static final String ID_RAM_USAGE = "usedRam";

    public SystemInfoPanel(String id){
        super(id);
    }

    @Override
    public IModel<SystemInfoDto> createModel() {
        return new LoadableModel<SystemInfoDto>(false) {

            @Override
            protected SystemInfoDto load() {
                return loadSystemInfo();
            }
        };
    }

    private SystemInfoDto loadSystemInfo(){
        return new SystemInfoDto(createStringResource("SystemInfoPanel.activeUsers"), createStringResource("SystemInfoPanel.activeTasks"),
                createStringResource("SystemInfoPanel.serverLoad"), createStringResource("SystemInfoPanel.usedRam"));
    }

    @Override
    protected void initLayout(){
        SimplePieChartDto usersDto = getModel().getObject().getActiveUsersDto();
        SimplePieChartDto tasksDto = getModel().getObject().getActiveTasksDto();
        SimplePieChartDto cpuDto = getModel().getObject().getServerLoadDto();
        SimplePieChartDto ramDto = getModel().getObject().getUsedRamDto();

        SimplePieChart userPanel = new SimplePieChart(ID_ACTIVE_USERS, usersDto.getLabel(), usersDto.getBase(), usersDto.getEntryValue());
        add(userPanel);
        SimplePieChart tasksPanel = new SimplePieChart(ID_ACTIVE_TASKS, tasksDto.getLabel(), tasksDto.getBase(), tasksDto.getEntryValue());
        add(tasksPanel);
        SimplePieChart serverLoadPanel = new SimplePieChart(ID_SERVER_LOAD, cpuDto.getLabel(), cpuDto.getBase(), cpuDto.getEntryValue());
        add(serverLoadPanel);
        SimplePieChart ramPanel = new SimplePieChart(ID_RAM_USAGE, ramDto.getLabel(), ramDto.getBase(), ramDto.getEntryValue());
        add(ramPanel);
    }
}
