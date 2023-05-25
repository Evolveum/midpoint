/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import java.util.*;
import jakarta.annotation.PostConstruct;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;

/**
 * Holds static configuration of workflows (from config.xml file).
 */
@Component
@DependsOn({ "midpointConfiguration" })
public class WfConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(WfConfiguration.class);

    private static final String KEY_ENABLED = "enabled";
    private static final List<String> KNOWN_KEYS = Arrays.asList(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, KEY_ENABLED);
    private static final List<String> DEPRECATED_KEYS = Collections.emptyList();

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    private boolean enabled;

    private final List<ChangeProcessor> changeProcessors = new ArrayList<>();

    @PostConstruct
    void initialize() {
        Configuration c = midpointConfiguration.getConfiguration(MidpointConfiguration.WORKFLOW_CONFIGURATION);
        checkAllowedKeys(c);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Workflows are disabled.");
        }
    }

    private void checkAllowedKeys(Configuration c) {
        Set<String> knownKeysSet = new HashSet<>(KNOWN_KEYS);
        Set<String> deprecatedKeysSet = new HashSet<>(DEPRECATED_KEYS);

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext()) {
            String keyName = keyIterator.next();
            String normalizedKeyName = StringUtils.substringBefore(keyName, "."); // because of sub-keys
            if (deprecatedKeysSet.contains(keyName) || deprecatedKeysSet.contains(normalizedKeyName)) {
                throw new SystemException("Deprecated key " + keyName + " in workflow configuration."
                        + " Please see: https://docs.evolveum.com/midpoint/reference/cases/workflow-3/new-3-5-workflow-configuration/");
            }
            if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(normalizedKeyName)) { // ...we need to test both because of keys like 'midpoint.home'
                throw new SystemException("Unknown key " + keyName + " in workflow configuration");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void registerProcessor(ChangeProcessor changeProcessor) {
        changeProcessors.add(changeProcessor);
    }

    public List<ChangeProcessor> getChangeProcessors() {
        return changeProcessors;
    }
}
