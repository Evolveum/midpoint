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

package com.evolveum.midpoint.wf.processors.general;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralChangeProcessorConfigurationType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.Arrays;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class GeneralChangeProcessorConfigurationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(GeneralChangeProcessorConfigurationHelper.class);

    private static final String KEY_GENERAL_CHANGE_PROCESSOR_CONFIGURATION = "generalChangeProcessorConfiguration";
    private static final List<String> LOCALLY_KNOWN_KEYS = Arrays.asList(KEY_GENERAL_CHANGE_PROCESSOR_CONFIGURATION);

    @Autowired
    private BaseConfigurationHelper baseConfigurationHelper;

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    @Autowired
    private PrismContext prismContext;

    GeneralChangeProcessorConfigurationType configure(GeneralChangeProcessor generalChangeProcessor) {
        baseConfigurationHelper.configureProcessor(generalChangeProcessor, LOCALLY_KNOWN_KEYS);
        if (generalChangeProcessor.isEnabled()) {
            return readConfiguration(generalChangeProcessor);
        } else {
            return null;
        }
    }

    private GeneralChangeProcessorConfigurationType readConfiguration(GeneralChangeProcessor generalChangeProcessor) {
        String beanName = generalChangeProcessor.getBeanName();

        String path = determineConfigurationPath(generalChangeProcessor);
        LOGGER.info("Configuration path: " + path);

        Document midpointConfig = midpointConfiguration.getXmlConfigAsDocument();
        Validate.notNull(midpointConfig, "XML version of midPoint configuration couldn't be found");

        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Element processorConfig = (Element) xpath.evaluate(path + "/*[local-name()='" + KEY_GENERAL_CHANGE_PROCESSOR_CONFIGURATION + "']", midpointConfig, XPathConstants.NODE);
            if (processorConfig == null) {
                throw new SystemException("There's no " + KEY_GENERAL_CHANGE_PROCESSOR_CONFIGURATION + " element in " + beanName + " configuration.");
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("processor configuration = {}", DOMUtil.printDom(processorConfig));
            }
            try {
                baseConfigurationHelper.validateElement(processorConfig);
            } catch (SchemaException e) {
                throw new SystemException("Schema validation failed for " + KEY_GENERAL_CHANGE_PROCESSOR_CONFIGURATION + " element in " + beanName + " configuration: " + e.getMessage(), e);
            }
            return prismContext.getPrismJaxbProcessor().toJavaValue(processorConfig, GeneralChangeProcessorConfigurationType.class);
        } catch (XPathExpressionException|JAXBException e) {
            throw new SystemException("Couldn't read general workflow processor configuration in " + beanName, e);
        }
    }

    // if this would not work, use simply "/configuration/midpoint/workflow/changeProcessors/generalChangeProcessor/" :)
    private String determineConfigurationPath(GeneralChangeProcessor generalChangeProcessor) {
        Configuration c = generalChangeProcessor.getProcessorConfiguration();
        if (!(c instanceof SubsetConfiguration)) {
            throw new IllegalStateException(generalChangeProcessor.getBeanName() + " configuration is not a subset configuration, it is " + c.getClass());
        }
        SubsetConfiguration sc = (SubsetConfiguration) c;
        return "/*/" + sc.getPrefix().replace(".", "/");
    }
}
