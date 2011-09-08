/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.util.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jmx.export.MBeanExporter;

/**
 *
 * @author lazyman
 */
public class LoggerMXBeanExporter {

    private static Trace trace = TraceManager.getTrace(LoggerMXBeanExporter.class);
    private MBeanExporter exporter;

    public void setExporter(MBeanExporter exporter) {
        this.exporter = exporter;
    }

    public void setBeans(Map<String, LoggerBean> map) {
        if (map == null) {
            return;
        }
        
        exporter.setRegistrationBehavior(MBeanExporter.REGISTRATION_REPLACE_EXISTING);
        try {
            Set<Entry<String, LoggerBean>> entries = map.entrySet();
            
            Map<String, Object> beanMap = new HashMap<String, Object>();
            for (Entry<String, LoggerBean> entry : entries) {
                trace.info(this.toString() + " Trying to export: " + entry.getKey() + ", " + entry.getValue());
                
                ClassPathResource resource = new ClassPathResource(entry.getValue().getContext());
                XmlBeanFactory factory = new XmlBeanFactory(resource);
                LoggerMXBean LOGGER = (LoggerMXBean) factory.getBean(entry.getValue().getBeanName());

                beanMap.put(entry.getKey(), LOGGER);
            }
            exporter.setBeans(beanMap);
            exporter.afterPropertiesSet();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class LoggerBean {

        private String context;
        private String beanName;

        public LoggerBean(String context, String beanName) {
            this.context = context;
            this.beanName = beanName;
        }

        public String getContext() {
            return context;
        }

        public String getBeanName() {
            return beanName;
        }
    }
}
