/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class PcpConfigurationHelper {
    private static final Trace LOGGER = TraceManager.getTrace(PcpConfigurationHelper.class);

    @Autowired
    private BaseConfigurationHelper baseConfigurationHelper;

    // configuration
    private static final String KEY_ASPECT = "aspect";
    private static final List<String> LOCALLY_KNOWN_KEYS = Arrays.asList(KEY_ASPECT);

    public void configure(PrimaryChangeProcessor primaryChangeProcessor) {
        baseConfigurationHelper.configureProcessor(primaryChangeProcessor, LOCALLY_KNOWN_KEYS);
        setPrimaryChangeProcessorAspects(primaryChangeProcessor);
    }

    private void setPrimaryChangeProcessorAspects(PrimaryChangeProcessor primaryChangeProcessor) {

        List<PrimaryChangeAspect> aspects = new ArrayList<>();

        Configuration c = primaryChangeProcessor.getProcessorConfiguration();
        if (c != null) {
            String[] aspectNames = c.getStringArray(KEY_ASPECT);
            if (aspectNames == null || aspectNames.length == 0) {
                LOGGER.warn("No aspects defined for primary change processor " + primaryChangeProcessor.getBeanName());
            } else {
                for (String aspectName : aspectNames) {
                    LOGGER.trace("Searching for aspect " + aspectName);
                    try {
                        PrimaryChangeAspect aspect = (PrimaryChangeAspect) primaryChangeProcessor.getBeanFactory().getBean(aspectName);
                        aspects.add(aspect);
                    } catch (BeansException e) {
                        throw new SystemException("Change aspect " + aspectName + " could not be found.", e);
                    }
                }
                LOGGER.debug("Resolved " + aspects.size() + " process aspects for primary change processor " + primaryChangeProcessor.getBeanName());
            }
        }
        primaryChangeProcessor.setChangeAspects(aspects);
    }

}
