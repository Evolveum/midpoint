package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

/**
 * @author lazyman
 * @author shood
 */
public class SystemInfoDto implements Serializable {

    private SimplePieChartDto activeUsersDto;
    private SimplePieChartDto activeTasksDto;
    private SimplePieChartDto serverLoadDto;
    private SimplePieChartDto usedRamDto;


    public SystemInfoDto(SimplePieChartDto usersDto, SimplePieChartDto tasksDto, SimplePieChartDto loadDto, SimplePieChartDto ramDto) {
        this.activeUsersDto = usersDto;
        this.activeTasksDto = tasksDto;
        this.serverLoadDto = loadDto;
        this.usedRamDto = ramDto;
    }

    public SimplePieChartDto getActiveUsersDto() {
        return activeUsersDto;
    }

    public SimplePieChartDto getActiveTasksDto() {
        return activeTasksDto;
    }

    public SimplePieChartDto getServerLoadDto() {
        return serverLoadDto;
    }

    public SimplePieChartDto getUsedRamDto() {
        return usedRamDto;
    }
}
