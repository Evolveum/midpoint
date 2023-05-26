/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Date;

/**
 * @author Viliam Repan (lazyman)
 */
public class SystemInfoPanel extends BasePanel<SystemInfoPanel.SystemInfoDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SystemInfoPanel.class);

    private static final String ID_TABLE = "table";
    private static final String ID_CPU_USAGE = "cpuUsage";
    private static final String ID_HEAP_MEMORY = "heapMemory";
    private static final String ID_NON_HEAP_MEMORY = "nonHeapMemory";
    private static final String ID_THREADS = "threads";
    private static final String ID_DB_POOL = "dbPool";
    private static final String ID_START_TIME = "startTime";
    private static final String ID_UPTIME = "uptime";

    public SystemInfoPanel(String id) {
        super(id, (IModel<SystemInfoDto>) null);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
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
                fillDBPool(dto);

                return dto;
            }
        };
    }

    private void fillDBPool(SystemInfoDto dto) {
        dto.dbPool = getPageBase().getTaskManager().getDBPoolStats();
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

    private void initLayout() {
        final WebMarkupContainer table = new WebMarkupContainer(ID_TABLE);
        table.setOutputMarkupId(true);
        add(table);
        table.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofMillis(10000)));

        Label cpuUsage = new Label(ID_CPU_USAGE, new PropertyModel<>(getModel(), SystemInfoDto.F_CPU_USAGE));
        table.add(cpuUsage);

        Label heapMemory = new Label(ID_HEAP_MEMORY, createMemoryModel(true));
        table.add(heapMemory);

        Label nonHeapMemory = new Label(ID_NON_HEAP_MEMORY, createMemoryModel(false));
        table.add(nonHeapMemory);

        Label threads = new Label(ID_THREADS, createThreadModel());
        table.add(threads);

        Label dbPool = new Label(ID_DB_POOL, createDBPoolModel());
        table.add(dbPool);

        DateLabelComponent startTime = new DateLabelComponent(ID_START_TIME, createStartTimeModel(),
                WebComponentUtil.getLongDateTimeFormat(SystemInfoPanel.this.getPageBase()));
        table.add(startTime);

        Label uptime = new Label(ID_UPTIME, createUptimeModel());
        table.add(uptime);
    }

    private IModel<String> createUptimeModel() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();

                if (dto == null) {
                    return null;
                }

                int minutes = (int)(dto.uptime / 1000L / 60L);
                return WebComponentUtil.formatDurationWordsForLocal(minutes < 1 ? dto.uptime : minutes * 1000L * 60L, true, true,
                        SystemInfoPanel.this.getPageBase());
            }
        };
    }

    private IModel<Date> createStartTimeModel() {
        return new IModel<Date>() {

            @Override
            public Date getObject() {
                return getModelObject() == null ? null :
                        (getModelObject().starttime == 0 ? null  : new Date(getModelObject().starttime));
            }
        };
    }

    private IModel<String> createMemoryModel(final boolean heap) {
        return new IModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();

                //this is quite strange situation and probably it should not occur,
                // but sometimes, in the development mode the model object is null
                if (dto == null) {
                    return null;
                }

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
        return new IModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();

                if (dto == null) {
                    return null;
                }

                StringBuilder sb = new StringBuilder();
                sb.append(dto.threads[0]).append(" / ");
                sb.append(dto.threads[1]).append(" / ");
                sb.append(dto.threads[2]);

                return sb.toString();
            }
        };
    }

    private IModel<String> createDBPoolModel() {
        return () -> {
            SystemInfoDto dto = getModelObject();

            if (dto == null) {
                return null;
            }

            if (dto.dbPool == null)
                return "N/A";

            StringBuilder sb = new StringBuilder();
            sb.append(dto.dbPool[0]).append(" / ");
            sb.append(dto.dbPool[1]).append(" / ");
            sb.append(dto.dbPool[2]).append(" / ");
            sb.append(dto.dbPool[3]).append(" / ");
            sb.append(dto.dbPool[4]);

            return sb.toString();
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
        //Hikari pool connections active, idle, waiting, total, max-size"
        Number[] dbPool = new Number[5];

        long starttime = 0;
        long uptime = 0;
    }
}
