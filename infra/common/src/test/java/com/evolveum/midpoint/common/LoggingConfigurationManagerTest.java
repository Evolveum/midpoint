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
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class LoggingConfigurationManagerTest {

    @Test
    public void testLoggingConfigurationManagerJsonAppender() throws Exception {
        File logFile = new File("target/test-json-audit.log");
        Files.deleteIfExists(logFile.toPath());

        FileAppenderConfigurationType appenderConfig = new FileAppenderConfigurationType();
        appenderConfig.setName("AUDIT_JSON");
        appenderConfig.setFileName(logFile.getAbsolutePath());
        appenderConfig.setFormat(AppenderFormatType.JSON);

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
                .containsPattern("<encoder class=\"net.logstash.logback.encoder.LogstashEncoder\">\\s*</encoder>");

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
        appender.setFormat(AppenderFormatType.JSON);

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
        appender.setFormat(AppenderFormatType.JSON);          // no pattern

        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareTracingAppenderConfiguration(sb, appender);

        assertThat(sb.toString())
                .doesNotContain("null")
                .contains("%msg%n");
    }

    @Test
    public void testJsonCustomFieldsAreEscapedInXml() throws Exception {
        File logFile = new File("target/test-json-escaping.log");
        Files.deleteIfExists(logFile.toPath());

        // custom fields with characters that are special in XML
        String customFields = "{\"app\":\"midpoint\",\"note\":\"a<b & c 'x'\"}";

        JsonFormatConfigurationType json = new JsonFormatConfigurationType();
        json.setCustomFields(customFields);

        FileAppenderConfigurationType appenderConfig = new FileAppenderConfigurationType();
        appenderConfig.setName("ESCAPING_JSON");
        appenderConfig.setFileName(logFile.getAbsolutePath());
        appenderConfig.setFormat(AppenderFormatType.JSON);
        appenderConfig.setJson(json);

        StringBuilder appenderXml = new StringBuilder();
        appenderXml.append("<appender name=\"").append(appenderConfig.getName())
                .append("\" class=\"ch.qos.logback.core.FileAppender\">\n")
                .append("    <file>").append(appenderConfig.getFileName()).append("</file>\n");
        LoggingConfigurationManager.prepareCommonAppenderFooter(appenderXml, appenderConfig);

        String fullLogbackXml = "<configuration>\n" + appenderXml
                + "    <root level=\"INFO\">\n"
                + "        <appender-ref ref=\"ESCAPING_JSON\" />\n"
                + "    </root>\n"
                + "</configuration>";

        // 1. the generated XML contains the escaped value, not the raw one
        assertThat(fullLogbackXml)
                .contains("&lt;")
                .contains("&amp;")
                .doesNotContain("a<b & c");

        // 2. logback parses the XML and the encoder gets the original JSON
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(
                    new ByteArrayInputStream(fullLogbackXml.getBytes(StandardCharsets.UTF_8)));

            context.getLogger("com.evolveum.midpoint.test").info("message with custom fields");
        } finally {
            context.stop();                     // closes the appender and flushes the file
        }

        // 3. the log record contains the fields unchanged
        String fileContent = Files.readString(logFile.toPath()).trim();
        assertThat(fileContent)
                .startsWith("{")
                .endsWith("}")
                .contains("\"message\":\"message with custom fields\"")
                .contains("\"app\":\"midpoint\"")
                .contains("\"note\":\"a<b & c 'x'\"");
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonInvalidTimeZoneFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setTimeZone("Not/AZone");
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonInvalidTimestampPatternFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setTimestampPattern("invalid pattern [");
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    private static AppenderConfigurationType jsonAppender(String name) {
        AppenderConfigurationType appender = new AppenderConfigurationType();
        appender.setName(name);
        appender.setFormat(AppenderFormatType.JSON);
        appender.setJson(new JsonFormatConfigurationType());
        return appender;
    }

    //
    @Test
    public void testJsonWithoutStacktrace() throws Exception {
        JsonFormatConfigurationType json = new JsonFormatConfigurationType();
        json.setIncludeStacktrace(false);
        json.setMaxStacktraceDepth(5);

        String line = logExceptionThroughJsonAppender("NO_STACK", json);

        assertThat(line).contains("\"message\":\"failure\"").doesNotContain("\"stack_trace\"");
    }

    @Test
    public void testJsonWithLimitedStacktrace() throws Exception {
        String full = logExceptionThroughJsonAppender("FULL", new JsonFormatConfigurationType());

        JsonFormatConfigurationType json = new JsonFormatConfigurationType();
        json.setMaxStacktraceDepth(3);
        String limited = logExceptionThroughJsonAppender("LIMITED", json);

        assertThat(limited).contains("\"stack_trace\"");
        assertThat(limited.length()).isLessThan(full.length());
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonCustomFieldsNotJsonFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setCustomFields("app:MidPointTest");
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test
    public void testJsonCustomFieldsObjectIsAccepted() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setCustomFields("{\"app\":\"MidPointTest\"}");
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);   // no exception
    }

    private String logExceptionThroughJsonAppender(String name, JsonFormatConfigurationType json)
            throws Exception {
        File logFile = new File("target/test-json-" + name + ".log");
        Files.deleteIfExists(logFile.toPath());

        FileAppenderConfigurationType appenderConfig = new FileAppenderConfigurationType();
        appenderConfig.setName(name);
        appenderConfig.setFileName(logFile.getAbsolutePath());
        appenderConfig.setFormat(AppenderFormatType.JSON);
        appenderConfig.setJson(json);

        StringBuilder appenderXml = new StringBuilder();
        appenderXml.append("<appender name=\"").append(name)
                .append("\" class=\"ch.qos.logback.core.FileAppender\">\n")
                .append("    <file>").append(appenderConfig.getFileName()).append("</file>\n");
        LoggingConfigurationManager.prepareCommonAppenderFooter(appenderXml, appenderConfig);

        String fullLogbackXml = "<configuration>\n" + appenderXml
                + "    <root level=\"INFO\">\n"
                + "        <appender-ref ref=\"" + name + "\" />\n"
                + "    </root>\n"
                + "</configuration>";

        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(
                    new ByteArrayInputStream(fullLogbackXml.getBytes(StandardCharsets.UTF_8)));

            context.getLogger("com.evolveum.midpoint.test").error("failure", deepException(15));
        } finally {
            context.stop();                     // closes the appender and flushes the file
        }

        return Files.readString(logFile.toPath()).trim();
    }

    private static Exception deepException(int depth) {
        if (depth == 0) {
            return new IllegalStateException("bottom");
        }
        try {
            throw deepException(depth - 1);
        } catch (Exception e) {
            return new RuntimeException("level " + depth, e);
        }
    }

}
