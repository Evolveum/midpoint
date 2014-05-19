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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class used to configure a change processor. (Expects the processor to be a subclass of BaseChangeProcessor;
 * this can be relaxed by moving some methods to ChangeProcessor interface, if needed.)
 *
 * @author mederly
 */

@Component
public class BaseConfigurationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseConfigurationHelper.class);

    private static final String KEY_ENABLED = "enabled";
    private static final List<String> KNOWN_KEYS = Arrays.asList(KEY_ENABLED);

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private PrismContext prismContext;

    public void configureProcessor(BaseChangeProcessor changeProcessor, List<String> locallyKnownKeys) {

        String beanName = changeProcessor.getBeanName();

        Validate.notNull(beanName, "Bean name was not set correctly.");

        Configuration c = wfConfiguration.getChangeProcessorsConfig().subset(beanName);
        if (c.isEmpty()) {
            LOGGER.info("Skipping reading configuration of " + beanName + ", as it is not on the list of change processors or is empty.");
            return;
        }

        List<String> allKnownKeys = new ArrayList<>(KNOWN_KEYS);
        if (locallyKnownKeys != null) {
            allKnownKeys.addAll(locallyKnownKeys);
        }
        wfConfiguration.checkAllowedKeys(c, allKnownKeys);

        boolean enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Change processor " + beanName + " is DISABLED.");
        }
        changeProcessor.setEnabled(enabled);
        changeProcessor.setProcessorConfiguration(c);
    }

    public void validateElement(Element element) throws SchemaException {
        OperationResult result = new OperationResult("validateElement");
        Validator validator = new Validator(prismContext);
        validator.validateSchema(element, result);
        result.computeStatus();
        if (!result.isSuccess()) {
            throw new SchemaException(result.getMessage(), result.getCause());
        }
    }

}
