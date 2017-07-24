/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.SimplePanel;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.ocpsoft.prettytime.PrettyTime;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Date;

/**
 * @author Viliam Repan (lazyman)
 */
public class SystemInfoPanel extends SimplePanel<SystemInfoPanel.SystemInfoDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SystemInfoPanel.class);

    private static final String ID_TABLE = "table";
    private static final String ID_CPU_USAGE = "cpuUsage";
    private static final String ID_HEAP_MEMORY = "heapMemory";
    private static final String ID_NON_HEAP_MEMORY = "nonHeapMemory";
    private static final String ID_THREADS = "threads";
    private static final String ID_START_TIME = "startTime";
    private static final String ID_UPTIME = "uptime";

    public SystemInfoPanel(String id) {
        super(id);
    }

    @Override
    public IModel<SystemInfoDto> createModel() {
        return new LoadableModel<SystemInfoDto>() {

            @Override
            protected SystemInfoDto load() {
                SystemInfoDto dto = new SystemInfoDto();
                try {
                    fillCpuUsage(dto);
                    fillMemoryUsage(dto);
                    fillThreads(dto);
                    fillUptime(dto);
                } catch (Exception ex) {
                    LOGGER.debug("Couldn't load jmx data", ex);
                }

                return dto;
            }
        };
    }

    private void fillUptime(SystemInfoDto dto) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=Runtime");

        dto.uptime = (long) mbs.getAttribute(name, "Uptime");
        dto.starttime = (long) mbs.getAttribute(name, "StartTime");
    }

    private void fillCpuUsage(SystemInfoDto dto) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) {
            dto.cpuUsage = Double.NaN;
            return;
        }

        Attribute att = (Attribute) list.get(0);
        Double value = (Double) att.getValue();

        if (value == -1.0) {
            // usually takes a couple of seconds before we get real values
            dto.cpuUsage = Double.NaN;
            return;
        }

        dto.cpuUsage = ((int) (value * 1000) / 10.0);
    }

    private void fillMemoryUsage(SystemInfoDto dto) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=Memory");

        CompositeData cd = (CompositeData) mbs.getAttribute(name, "HeapMemoryUsage");
        dto.heapMemory[0] = (Long) cd.get("used");
        dto.heapMemory[1] = (Long) cd.get("committed");
        dto.heapMemory[2] = (Long) cd.get("max");

        cd = (CompositeData) mbs.getAttribute(name, "NonHeapMemoryUsage");
        dto.nonHeapMemory[0] = (Long) cd.get("used");
        dto.nonHeapMemory[1] = (Long) cd.get("committed");
        dto.nonHeapMemory[2] = (Long) cd.get("max");
    }

    private void fillThreads(SystemInfoDto dto) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=Threading");

        dto.threads[0] = (Number) mbs.getAttribute(name, "ThreadCount");
        dto.threads[1] = (Number) mbs.getAttribute(name, "PeakThreadCount");
        dto.threads[2] = (Number) mbs.getAttribute(name, "TotalStartedThreadCount");
    }

    @Override
    protected void initLayout() {
        final WebMarkupContainer table = new WebMarkupContainer(ID_TABLE);
        table.setOutputMarkupId(true);
        add(table);
        table.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(10000)));

        Label cpuUsage = new Label(ID_CPU_USAGE, new PropertyModel<>(getModel(), SystemInfoDto.F_CPU_USAGE));
        table.add(cpuUsage);

        Label heapMemory = new Label(ID_HEAP_MEMORY, createMemoryModel(true));
        table.add(heapMemory);

        Label nonHeapMemory = new Label(ID_NON_HEAP_MEMORY, createMemoryModel(false));
        table.add(nonHeapMemory);

        Label threads = new Label(ID_THREADS, createThreadModel());
        table.add(threads);

        DateLabelComponent startTime = new DateLabelComponent(ID_START_TIME, createStartTimeModel(), DateLabelComponent.MEDIUM_MEDIUM_STYLE);
        table.add(startTime);

        Label uptime = new Label(ID_UPTIME, createUptimeModel());
        table.add(uptime);
    }

    private IModel<String> createUptimeModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();

                PrettyTime time = new PrettyTime();
                return time.format(new Date(System.currentTimeMillis() - dto.uptime));
            }
        };
    }

    private IModel<Date> createStartTimeModel() {
        return new AbstractReadOnlyModel<Date>() {

            @Override
            public Date getObject() {
                return getModelObject() == null ? null :
                        (getModelObject().starttime == 0 ? null  : new Date(getModelObject().starttime));
            }
        };
    }

    private IModel<String> createMemoryModel(final boolean heap) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();
                Long[] memory = heap ? dto.heapMemory : dto.nonHeapMemory;

                StringBuilder sb = new StringBuilder();
                sb.append(WebComponentUtil.createHumanReadableByteCount(memory[0])).append(" / ");
                sb.append(WebComponentUtil.createHumanReadableByteCount(memory[1])).append(" / ");
                sb.append(WebComponentUtil.createHumanReadableByteCount(memory[2]));

                return sb.toString();
            }
        };
    }

    private IModel<String> createThreadModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();

                StringBuilder sb = new StringBuilder();
                sb.append(dto.threads[0]).append(" / ");
                sb.append(dto.threads[1]).append(" / ");
                sb.append(dto.threads[2]);

                return sb.toString();
            }
        };
    }

    static class SystemInfoDto implements Serializable {

        static final String F_CPU_USAGE = "cpuUsage";
        static final String F_HEAP_MEMORY = "heapMemory";
        static final String F_NON_HEAP_MEMORY = "nonHeapMemory";

        double cpuUsage = Double.NaN;
        //used, committed, max
        Long[] heapMemory = new Long[3];
        //used, committed, max
        Long[] nonHeapMemory = new Long[3];
        //ThreadCount, PeakThreadCount, TotalStartedThreadCount
        Number[] threads = new Number[3];

        long starttime = 0;
        long uptime = 0;
    }
}
