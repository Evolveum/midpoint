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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManagerImpl;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorRegistry;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Created by Viliam Repan (lazyman).
 */
@Configuration
public class WebConfig {

    @Bean
    public MidPointApplication midpointApplication() {
        return new MidPointApplication();
    }

    @Bean
    public MidpointFormValidatorRegistry midpointFormValidatorRegistry() {
        return new MidpointFormValidatorRegistry();
    }

    @Bean
    public AsyncWebProcessManager asyncWebProcessManager() {
        return new AsyncWebProcessManagerImpl();
    }

    @Configuration
    public static class StaticResourceConfiguration implements WebMvcConfigurer {

        @Autowired
        private ResourceProperties resourceProperties;
        @Value("${midpoint.home}")
        private String midpointHome;

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
            CacheControl cacheControl = this.resourceProperties.getCache()
                    .getCachecontrol().toHttpCacheControl();
            if (!registry.hasMappingForPattern("/static-web/**")) {
                registry
                        .addResourceHandler("/static-web/**")
                        .addResourceLocations("file://" + midpointHome + "/static-web/")
                        .setCachePeriod(getSeconds(cachePeriod))
                        .setCacheControl(cacheControl);
            }
        }

        private Integer getSeconds(Duration cachePeriod) {
            return (cachePeriod != null ? (int) cachePeriod.getSeconds() : null);
        }
    }
}
