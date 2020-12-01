/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.boot;

import com.evolveum.midpoint.web.util.ConfigurableXmlModelFactory;
import com.evolveum.midpoint.web.util.MidPointUrlLocatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ClassPathResource;
import ro.isdc.wro.extensions.processor.css.Less4jProcessor;
import ro.isdc.wro.extensions.processor.css.LessCssProcessor;
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
import ro.isdc.wro.model.resource.processor.impl.css.LessCssImportPreProcessor;
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
    public WroManagerFactory wroManagerFactory(WroModelFactory wroModelFactory, PropertyResolver propertyResolver) {
        ConfigurableWroManagerFactory factory = new ConfigurableWroManagerFactory();
        factory.setModelFactory(wroModelFactory);
        factory.setUriLocatorFactory(new MidPointUrlLocatorFactory(propertyResolver));

        SimpleProcessorsFactory processors = new SimpleProcessorsFactory();
        Collection<ResourcePreProcessor> preProcessors = new ArrayList<>();
        preProcessors.add(new CssUrlRewritingProcessor());
        preProcessors.add(new MidPointCssImportPreProcessor()); //CssImportPreProcessor()
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
