/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.common;

import ch.qos.logback.classic.LoggerContext;

import ch.qos.logback.classic.joran.JoranConfigurator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderLayoutType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileAppenderConfigurationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LoggingConfigurationManagerTest {


    @Test
    public void testLoggingConfigurationManagerJsonAppender() throws Exception {
        File logFile = new File("target/test-json-audit.log");
        if (logFile.exists()) {
            logFile.delete();
        }

        FileAppenderConfigurationType appenderConfig = new FileAppenderConfigurationType();
        appenderConfig.setName("AUDIT_JSON");
        appenderConfig.setFileName(logFile.getAbsolutePath());
        appenderConfig.setLayout(AppenderLayoutType.JSON);

        StringBuilder appenderXmlBuilder = new StringBuilder();

        appenderXmlBuilder.append("<appender name=\"").append(appenderConfig.getName())
                .append("\" class=\"ch.qos.logback.core.FileAppender\">\n")
                .append("    <file>").append(appenderConfig.getFileName()).append("</file>\n");

        LoggingConfigurationManager.prepareCommonAppenderFooter(appenderXmlBuilder, appenderConfig);

        String fullLogbackXml =
                "<configuration>\n" +
                        appenderXmlBuilder.toString() +
                        "    <root level=\"INFO\">\n" +
                        "        <appender-ref ref=\"AUDIT_JSON\" />\n" +
                        "    </root>\n" +
                        "</configuration>";

        assertThat(fullLogbackXml)
                .contains("<encoder class=\"net.logstash.logback.encoder.LogstashEncoder\"/>");

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new ByteArrayInputStream(fullLogbackXml.getBytes(StandardCharsets.UTF_8)));
        Logger logger = LoggerFactory.getLogger("com.evolveum.midpoint.audit");
        String testMessage = "Audit message through LoggingConfigurationManager";
        logger.info(testMessage);

        String fileContent = Files.readString(logFile.toPath()).trim();


        assertThat(fileContent)
                .startsWith("{")
                .endsWith("}")
                .contains("\"message\":\"" + testMessage + "\"")
                .contains("\"@timestamp\"")
                .contains("\"level\":\"INFO\"");




    }

}
