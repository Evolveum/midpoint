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

import com.evolveum.midpoint.web.util.ConfigurableXmlModelFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ro.isdc.wro.extensions.processor.css.Less4jProcessor;
import ro.isdc.wro.http.ConfigurableWroFilter;
import ro.isdc.wro.http.WroFilter;
import ro.isdc.wro.manager.factory.ConfigurableWroManagerFactory;
import ro.isdc.wro.manager.factory.WroManagerFactory;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.model.resource.processor.factory.SimpleProcessorsFactory;
import ro.isdc.wro.model.resource.processor.impl.css.CssImportPreProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssUrlRewritingProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.SemicolonAppenderPreProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Viliam Repan (lazyman).
 */
@Configuration
public class Wro4jConfig {

    public static final String WRO_MBEAN_NAME = "wro4j-midpoint";

    @Bean
    public WroModelFactory wroModelFactory() {
        return new ConfigurableXmlModelFactory(new ClassPathResource("/wro.xml"));
    }

    @Bean
    public WroManagerFactory wroManagerFactory(WroModelFactory wroModelFactory) {
        ConfigurableWroManagerFactory factory = new ConfigurableWroManagerFactory();
        factory.setModelFactory(wroModelFactory);

        SimpleProcessorsFactory processors = new SimpleProcessorsFactory();
        Collection<ResourcePreProcessor> preProcessors = new ArrayList<>();
        preProcessors.add(new CssUrlRewritingProcessor());
        preProcessors.add(new CssImportPreProcessor());
        preProcessors.add(new SemicolonAppenderPreProcessor());

        Collection<ResourcePostProcessor> postProcessors = new ArrayList<>();
        postProcessors.add(new Less4jProcessor());

        processors.setResourcePreProcessors(preProcessors);
        processors.setResourcePostProcessors(postProcessors);

        factory.setProcessorsFactory(processors);

        return factory;
    }

    @Bean
    public WroFilter wroFilter(WroManagerFactory wroManagerFactory) throws IOException {
        ConfigurableWroFilter filter = new ConfigurableWroFilter();
        filter.setWroManagerFactory(wroManagerFactory);
        filter.setMbeanName(WRO_MBEAN_NAME);

        filter.setDisableCache(false);

        return filter;
    }
}
