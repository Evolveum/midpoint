/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.boot;


import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;

/**
 * Created by Lukas Skublik.
 */
@Configuration
@ConditionalOnClass(CollectorRegistry.class)
public class PrometheusConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CollectorRegistry metricRegistry() {
		return CollectorRegistry.defaultRegistry;
	}

	@Bean
	ServletRegistrationBean registerPrometheusExporterServlet(CollectorRegistry metricRegistry) {
		return new ServletRegistrationBean(new MetricsServlet(metricRegistry), "/prometheus");
	}

     @Bean
     ExporterRegister exporterRegister() {
           List<Collector> collectors = new ArrayList<>();
           collectors.add(new StandardExports());
           collectors.add(new MemoryPoolsExports());
           collectors.add(new ClassLoadingExports());
           collectors.add(new GarbageCollectorExports());
           collectors.add(new ThreadExports());
           collectors.add(new VersionInfoExports());
           ExporterRegister register = new ExporterRegister(collectors);
           return register;
     }
     
     public class ExporterRegister {

         private List<Collector> collectors; 

         public ExporterRegister(List<Collector> collectors) {
              for (Collector collector : collectors) {
                  collector.register();
              }
              this.collectors = collectors;
         }

         public List<Collector> getCollectors() {
              return collectors;
         }
    }
     
}