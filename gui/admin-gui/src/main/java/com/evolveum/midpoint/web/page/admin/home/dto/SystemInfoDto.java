package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemInfoDto implements Serializable {

    private SimplePieChartDto activeUsersDto;
    private SimplePieChartDto activeTasksDto;
    private SimplePieChartDto serverLoadDto;
    private SimplePieChartDto usedRamDto;


    public SystemInfoDto(StringResourceModel s1, StringResourceModel s2, StringResourceModel s3, StringResourceModel s4) {
        //TODO - initialize all SimplePieChartsDto here
        activeUsersDto = new SimplePieChartDto(s1.getString(), 100, 15);
        activeTasksDto = new SimplePieChartDto(s2.getString(), 100, 20);
        serverLoadDto = new SimplePieChartDto(s3.getString(), 100, WebMiscUtil.getSystemLoad());
        usedRamDto = new SimplePieChartDto(s4.getString(), WebMiscUtil.getMaxRam(), WebMiscUtil.getRamUsage());
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
