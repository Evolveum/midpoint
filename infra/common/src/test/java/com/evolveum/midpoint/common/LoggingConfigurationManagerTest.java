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
import java.io.IOException;
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
                .doesNotContain("<format")
                .contains("<layout class=\"ch.qos.logback.classic.PatternLayout\">")
                .contains("%msg%n");
    }

    @Test
    public void testJsonCustomFieldsAreEscapedInXml() throws Exception {
        File logFile = new File("target/test-json-escaping.log");
        Files.deleteIfExists(logFile.toPath());

        JsonFormatConfigurationType json = new JsonFormatConfigurationType();
        json.getCustomField().add(customField("app", "midpoint"));
        json.getCustomField().add(customField("note", "a<b & c 'x' \"q\" \\"));
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
                .doesNotContain("a<b & c")
                .containsPattern("\\t\\t\\t<customFields>");

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
            context.stop();
        }

        // 3. the log record contains the fields unchanged
        String fileContent = Files.readString(logFile.toPath()).trim();
        assertThat(fileContent)
                .startsWith("{")
                .endsWith("}")
                .contains("\"message\":\"message with custom fields\"")
                .contains("\"app\":\"midpoint\"")
                .contains("\"note\":\"a<b & c 'x' \\\"q\\\" \\\\\"");
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonInvalidTimeZoneFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setTimezone("Not/AZone");
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
    public void testJsonCustomFieldEmptyNameFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().getCustomField().add(customField(" ", "x"));
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonCustomFieldDuplicateNameFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().getCustomField().add(customField("app", "a"));
        appender.getJson().getCustomField().add(customField("app", "b"));
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonCustomFieldWithoutValueFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().getCustomField().add(customField("app", null));
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void testJsonMaxStacktraceDepthZeroFails() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setMaxStacktraceDepth(0);
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test
    public void testJsonMaxStacktraceDepthOneIsAccepted() throws Exception {
        AppenderConfigurationType appender = jsonAppender("JSON");
        appender.getJson().setMaxStacktraceDepth(1);
        LoggingConfigurationManager.prepareCommonAppenderFooter(new StringBuilder(), appender);
    }

    @Test
    public void testJsonTimestampPatternAndTimeZoneAreApplied() throws Exception {
        JsonFormatConfigurationType json = new JsonFormatConfigurationType();
        json.setTimezone("UTC");
        json.setTimestampPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        String line = logExceptionThroughJsonAppender("TIMESTAMP", json);

        assertThat(line).containsPattern("\"@timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z\"");
    }
    @Test
    public void testJsonSettingsAreIgnoredForTextFormat() throws Exception {
        AppenderConfigurationType appender = textAppender("TEXT", "%msg%n");
        appender.setJson(new JsonFormatConfigurationType());
        appender.getJson().setTimezone("Not/AZone");   // would fail for the JSON format

        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareCommonAppenderFooter(sb, appender);

        assertThat(sb.toString()).contains("<pattern>%msg%n</pattern>").doesNotContain("LogstashEncoder");
    }

// ------------------------------------------------------------------------------------------
// ALT appender
// ------------------------------------------------------------------------------------------

    @Test
    public void testAltAppenderToConsoleUsesPatternLayout() {
        String xml = altAppenderXml(null, "ALT: ");

        assertThat(xml)
                .contains("ch.qos.logback.core.ConsoleAppender")
                .contains("<layout class=\"ch.qos.logback.classic.PatternLayout\">")
                .contains("<pattern>ALT: %msg%n</pattern>")
                .doesNotContain("<format");


    }

    @Test
    public void testAltAppenderToFileWritesRecordWithPrefix() throws Exception {

        File logFile = new File("target/test-alt.log");
        Files.deleteIfExists(logFile.toPath());

        String appenderXml = altAppenderXml(logFile.getAbsolutePath(), "ALT: ");

        assertThat(appenderXml)
                .contains("ch.qos.logback.core.FileAppender")
                .contains("<layout class=\"ch.qos.logback.classic.PatternLayout\">")
                .doesNotContain("<format");

        String fullLogbackXml = "<configuration>\n" + appenderXml
                + "    <root level=\"INFO\">\n"
                + "        <appender-ref ref=\"ALT_LOG\" />\n"
                + "    </root>\n"
                + "</configuration>";

        LoggerContext context = new LoggerContext();          // own context, not the global one
        context.setMDCAdapter(new LogbackMDCAdapter());
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(
                    new ByteArrayInputStream(fullLogbackXml.getBytes(StandardCharsets.UTF_8)));

            context.getLogger("com.evolveum.midpoint.test").info("hello");
        } finally {
            context.stop();                                     // closes the appender and flushes the file
        }

        // if the layout element were not recognized, the pattern and the prefix would be missing
        assertThat(Files.readString(logFile.toPath()).trim()).isEqualTo("ALT: hello");
    }

// ------------------------------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------------------------------

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

    private static JsonCustomFieldType customField(String name, String value) {
        JsonCustomFieldType field = new JsonCustomFieldType();
        field.setName(name);
        field.setValue(value);
        return field;
    }

    private static AppenderConfigurationType textAppender(String name, String pattern) {
        AppenderConfigurationType appender = new AppenderConfigurationType();
        appender.setName(name);
        appender.setPattern(pattern);
        return appender;
    }
    /** XML of the ALT appender for the given file name (null = console) and prefix. */
    private static String altAppenderXml(String fileName, String prefix) {
        StringBuilder sb = new StringBuilder();
        LoggingConfigurationManager.prepareAltAppenderConfiguration(
                sb, textAppender("ROOT", "%msg%n"), fileName, prefix);
        return sb.toString();
    }
}
