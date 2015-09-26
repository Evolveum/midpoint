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

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;
import java.lang.management.ManagementFactory;

/**
 * @author Viliam Repan (lazyman)
 */
public class SystemInfoPanel extends SimplePanel<SystemInfoPanel.SystemInfoDto> {

    private static final String ID_CPU_USAGE = "cpuUsage";
    private static final String ID_HEAP_MEMORY = "heapMemory";
    private static final String ID_NON_HEAP_MEMORY = "nonHeapMemory";
    private static final String ID_THREADS = "threads";

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
                } catch (Exception ex) {
                    //todo fix
                    ex.printStackTrace();
                }

                return dto;
            }
        };
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
        Label cpuUsage = new Label(ID_CPU_USAGE, new PropertyModel<>(getModel(), SystemInfoDto.F_CPU_USAGE));
        add(cpuUsage);

        Label heapMemory = new Label(ID_HEAP_MEMORY, createMemoryModel(true));
        add(heapMemory);

        Label nonHeapMemory = new Label(ID_NON_HEAP_MEMORY, createMemoryModel(false));
        add(nonHeapMemory);

        Label threads = new Label(ID_THREADS, createThreadModel());
        add(threads);
    }

    private IModel<String> createMemoryModel(final boolean heap) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SystemInfoDto dto = getModelObject();
                Long[] memory = heap ? dto.heapMemory : dto.nonHeapMemory;

                StringBuilder sb = new StringBuilder();
                sb.append(WebMiscUtil.createHumanReadableByteCount(memory[0])).append(" / ");
                sb.append(WebMiscUtil.createHumanReadableByteCount(memory[1])).append(" / ");
                sb.append(WebMiscUtil.createHumanReadableByteCount(memory[2]));

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
    }
}
