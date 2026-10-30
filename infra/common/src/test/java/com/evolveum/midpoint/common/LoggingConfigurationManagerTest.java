/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.common;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

public class LoggingConfigurationManagerTest {

    @Test
    public void testLoggingConfigurationManagerJsonAppender() throws Exception {
        File logFile = new File("target/test-json-audit.log");
        Files.deleteIfExists(logFile.toPath());

        FileAppenderConfigurationType appenderConfig = new FileAppenderConfigurationType();
        appenderConfig.setName("AUDIT_JSON");
        appenderConfig.setFileName(logFile.getAbsolutePath());
        appenderConfig.setLayout(AppenderLayoutType.JSON);

        StringBuilder appenderXml = new StringBuilder();
        appenderXml.append("<appender name=\"").append(appenderConfig.getName())
                .append("\" class=\"ch.qos.logback.core.FileAppender\">\n")
                .append("    <file>").append(appenderConfig.getFileName()).append("</file>\n");
        LoggingConfigurationManager.prepareCommonAppenderFooter(appenderXml, appenderConfig);

        String fullLogbackXml = "<configuration>\n" + appenderXml
                + "    <root level=\"INFO\">\n"
                + "        <appender-ref ref=\"AUDIT_JSON\" />\n"
                + "    </root>\n"
                + "</configuration>";

        assertThat(fullLogbackXml)
                .contains("<encoder class=\"net.logstash.logback.encoder.LogstashEncoder\"/>");

        LoggerContext context = new LoggerContext();          // own context, not the global one
        context.setMDCAdapter(new LogbackMDCAdapter());
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(new ByteArrayInputStream(fullLogbackXml.getBytes(StandardCharsets.UTF_8)));

            String testMessage = "Audit message through LoggingConfigurationManager";
            context.getLogger("com.evolveum.midpoint.audit").info(testMessage);
        } finally {
            context.stop();                                     // closes the appender and flushes the file
        }

        String fileContent = Files.readString(logFile.toPath()).trim();
        assertThat(fileContent)
                .startsWith("{")
                .endsWith("}")
                .contains("\"message\":\"Audit message through LoggingConfigurationManager\"")
                .contains("\"@timestamp\"")
                .contains("\"level\":\"INFO\"");
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testTextLayoutWithoutPatternFails() throws Exception {
        AppenderConfigurationType appender = new AppenderConfigurationType();
        appender.setName("TEXT");
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test
    public void testTextLayoutWritesPattern() throws Exception {
        AppenderConfigurationType appender = new AppenderConfigurationType();
        appender.setName("TEXT");
        appender.setPattern("%msg%n");
        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareCommonAppenderFooter(sb, appender);
        assertThat(sb.toString()).contains("<pattern>%msg%n</pattern>").doesNotContain("LogstashEncoder");
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testSyslogWithJsonLayoutFails() throws Exception {
        SyslogAppenderConfigurationType appender = new SyslogAppenderConfigurationType();
        appender.setName("SYSLOG");
        appender.setLayout(AppenderLayoutType.JSON);

        LoggingConfigurationManager.prepareSyslogAppenderConfiguration(
                new StringBuilder(), appender, new LoggingConfigurationType());
    }

    @Test
    public void testSyslogWithTextLayoutWorks() throws Exception {
        SyslogAppenderConfigurationType appender = new SyslogAppenderConfigurationType();
        appender.setName("SYSLOG");
        appender.setPattern("%msg%n");
        appender.setSyslogHost("localhost");
        appender.setPort("514");
        appender.setFacility("USER");

        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareSyslogAppenderConfiguration(sb, appender, new LoggingConfigurationType());

        assertThat(sb.toString()).contains("SyslogAppender").contains("<pattern>%msg%n</pattern>");
    }
    @Test
    public void testTracingAppenderUsesDefaultPatternForJsonRootAppender() {
        AppenderConfigurationType appender = new AppenderConfigurationType();
        appender.setName("ROOT");
        appender.setLayout(AppenderLayoutType.JSON);          // no pattern

        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareTracingAppenderConfiguration(sb, appender);

        assertThat(sb.toString())
                .doesNotContain("null")
                .contains("%msg%n");
    }

}
