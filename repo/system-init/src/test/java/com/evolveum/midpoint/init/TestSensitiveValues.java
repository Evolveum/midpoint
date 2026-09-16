/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import static org.testng.Assert.*;

import static com.evolveum.midpoint.init.StartupConfiguration.SENSITIVE_VALUE_OUTPUT;

import org.testng.annotations.Test;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Tests detection and hiding of sensitive values in configuration keys and JVM arguments.
 */
public class TestSensitiveValues extends AbstractUnitTest {

    @Test
    public void test100PasswordLikeKeysAreSensitive() {
        assertTrue(StartupConfiguration.isSensitiveKey("http.proxyPassword"));
        assertTrue(StartupConfiguration.isSensitiveKey("https.proxyPassword"));
        assertTrue(StartupConfiguration.isSensitiveKey("javax.net.ssl.trustStorePassword"));
        assertTrue(StartupConfiguration.isSensitiveKey("javax.net.ssl.keyStorePassword"));
        assertTrue(StartupConfiguration.isSensitiveKey("MY_SECRET"));
        assertTrue(StartupConfiguration.isSensitiveKey("some.access.token"));
        assertTrue(StartupConfiguration.isSensitiveKey("db.passwd"));
        assertTrue(StartupConfiguration.isSensitiveKey("db.pwd"));
        assertTrue(StartupConfiguration.isSensitiveKey("service.credentials"));
        assertTrue(StartupConfiguration.isSensitiveKey("vendor.apiKey"));
        assertTrue(StartupConfiguration.isSensitiveKey("vendor.api_key"));
    }

    @Test
    public void test110KnownKeysAreSensitive() {
        assertTrue(StartupConfiguration.isSensitiveKey("midpoint.repository.jdbcUrl"));
        assertTrue(StartupConfiguration.isSensitiveKey("midpoint.repository.jdbcUsername"));
        assertTrue(StartupConfiguration.isSensitiveKey("midpoint.repository.dataSource"));
        assertTrue(StartupConfiguration.isSensitiveKey("midpoint.audit.jdbcUrl"));
        assertTrue(StartupConfiguration.isSensitiveKey("midpoint.administrator.initialPassword"));
        // short keys used in config.xml dumps
        assertTrue(StartupConfiguration.isSensitiveKey("jdbcPassword"));
        assertTrue(StartupConfiguration.isSensitiveKey("keyStorePassword"));
    }

    @Test
    public void test120OrdinaryKeysAreNotSensitive() {
        assertFalse(StartupConfiguration.isSensitiveKey("midpoint.home"));
        assertFalse(StartupConfiguration.isSensitiveKey("midpoint.keystore.keyStorePath"));
        assertFalse(StartupConfiguration.isSensitiveKey("javax.net.ssl.trustStore"));
        assertFalse(StartupConfiguration.isSensitiveKey("http.proxyHost"));
        assertFalse(StartupConfiguration.isSensitiveKey("server.port"));
        assertFalse(StartupConfiguration.isSensitiveKey(null));
        assertFalse(StartupConfiguration.isSensitiveKey(""));
    }

    @Test
    public void test200MaskReportedJvmArguments() {
        assertEquals(
                StartupConfiguration.maskSensitiveArgument("-Dhttp.proxyPassword=plaintextpwd"),
                "-Dhttp.proxyPassword=" + SENSITIVE_VALUE_OUTPUT);
        assertEquals(
                StartupConfiguration.maskSensitiveArgument("-Djavax.net.ssl.trustStorePassword=plaintextpwd"),
                "-Djavax.net.ssl.trustStorePassword=" + SENSITIVE_VALUE_OUTPUT);
        assertEquals(
                StartupConfiguration.maskSensitiveArgument("-Djavax.net.ssl.keyStorePassword=plaintextpwd"),
                "-Djavax.net.ssl.keyStorePassword=" + SENSITIVE_VALUE_OUTPUT);
        assertEquals(
                StartupConfiguration.maskSensitiveArgument("-Dmidpoint.repository.jdbcPassword=plaintextpwd"),
                "-Dmidpoint.repository.jdbcPassword=" + SENSITIVE_VALUE_OUTPUT);
    }

    @Test
    public void test210MaskWholeValueIncludingEqualsSigns() {
        // JDBC URL may carry credentials in query parameters, whole value must be hidden.
        assertEquals(
                StartupConfiguration.maskSensitiveArgument(
                        "-Dmidpoint.repository.jdbcUrl=jdbc:postgresql://db/midpoint?user=mp&password=secret"),
                "-Dmidpoint.repository.jdbcUrl=" + SENSITIVE_VALUE_OUTPUT);
        assertEquals(
                StartupConfiguration.maskSensitiveArgument("-Dhttp.proxyPassword=a=b=c"),
                "-Dhttp.proxyPassword=" + SENSITIVE_VALUE_OUTPUT);
    }

    @Test
    public void test220NonSensitiveArgumentsUnchanged() {
        assertEquals(StartupConfiguration.maskSensitiveArgument("-Dmidpoint.home=/opt/midpoint/var"),
                "-Dmidpoint.home=/opt/midpoint/var");
        assertEquals(StartupConfiguration.maskSensitiveArgument("-Dhttp.proxyHost=proxy.example.com"),
                "-Dhttp.proxyHost=proxy.example.com");
        assertEquals(StartupConfiguration.maskSensitiveArgument("-Xmx4g"), "-Xmx4g");
        assertEquals(StartupConfiguration.maskSensitiveArgument("-XX:+UseG1GC"), "-XX:+UseG1GC");
        // flag without value
        assertEquals(StartupConfiguration.maskSensitiveArgument("-Dmidpoint.printSensitiveValues"),
                "-Dmidpoint.printSensitiveValues");
        assertNull(StartupConfiguration.maskSensitiveArgument(null));
    }

    @Test
    public void test230SensitiveValueInNonSensitiveKeyIsNotMasked() {
        // Only the key decides, value content is irrelevant.
        assertEquals(StartupConfiguration.maskSensitiveArgument("-Dmidpoint.home=/home/password-manager"),
                "-Dmidpoint.home=/home/password-manager");
    }
}
