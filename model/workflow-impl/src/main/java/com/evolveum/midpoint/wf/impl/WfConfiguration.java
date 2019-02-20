/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 *
 * @author Pavol Mederly
 */
@Component
@DependsOn({ "midpointConfiguration" })

public class WfConfiguration implements BeanFactoryAware {

    private static final transient Trace LOGGER = TraceManager.getTrace(WfConfiguration.class);

    private static final String CHANGE_PROCESSORS_SECTION = "changeProcessors";         // deprecated

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_ALLOW_APPROVE_OTHERS_ITEMS = "allowApproveOthersItems";

    public static final List<String> KNOWN_KEYS = Arrays.asList("midpoint.home", KEY_ENABLED);

    public static final List<String> DEPRECATED_KEYS = Arrays.asList(CHANGE_PROCESSORS_SECTION, KEY_ALLOW_APPROVE_OTHERS_ITEMS);

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private boolean enabled;

    private List<ChangeProcessor> changeProcessors = new ArrayList<>();

    @PostConstruct
    void initialize() {

        Configuration c = midpointConfiguration.getConfiguration(MidpointConfiguration.WORKFLOW_CONFIGURATION);

        checkAllowedKeys(c, KNOWN_KEYS, DEPRECATED_KEYS);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Workflows are disabled.");
            return;
        }
        validate();
    }

    public void checkAllowedKeys(Configuration c, List<String> knownKeys, List<String> deprecatedKeys) {
        Set<String> knownKeysSet = new HashSet<>(knownKeys);
        Set<String> deprecatedKeysSet = new HashSet<>(deprecatedKeys);

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext())  {
            String keyName = keyIterator.next();
            String normalizedKeyName = StringUtils.substringBefore(keyName, ".");                       // because of subkeys
            normalizedKeyName = StringUtils.substringBefore(normalizedKeyName, "[");                    // because of [@xmlns:c]
            int colon = normalizedKeyName.indexOf(':');                                                 // because of c:generalChangeProcessorConfiguration
            if (colon != -1) {
                normalizedKeyName = normalizedKeyName.substring(colon + 1);
            }
            if (deprecatedKeysSet.contains(keyName) || deprecatedKeysSet.contains(normalizedKeyName)) {
                throw new SystemException("Deprecated key " + keyName + " in workflow configuration. Please see https://wiki.evolveum.com/display/midPoint/Workflow+configuration.");
            }
            if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(normalizedKeyName)) {         // ...we need to test both because of keys like 'midpoint.home'
                throw new SystemException("Unknown key " + keyName + " in workflow configuration");
            }
        }
    }

//    public Configuration getChangeProcessorsConfig() {
//        Validate.notNull(midpointConfiguration, "midpointConfiguration was not initialized correctly (check spring beans initialization order)");
//        return midpointConfiguration.getConfiguration(WORKFLOW_CONFIGURATION).subset(CHANGE_PROCESSORS_SECTION); // via subset, because getConfiguration puts 'midpoint.home' property to the result
//    }

    void validate() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ChangeProcessor findChangeProcessor(String processorClassName) {
        for (ChangeProcessor cp : changeProcessors) {
            if (cp.getClass().getName().equals(processorClassName)) {
                return cp;
            }
        }

        throw new IllegalStateException("Change processor " + processorClassName + " is not registered.");
    }

    public void registerProcessor(ChangeProcessor changeProcessor) {
        changeProcessors.add(changeProcessor);
    }

    public List<ChangeProcessor> getChangeProcessors() {
        return changeProcessors;
    }
}
