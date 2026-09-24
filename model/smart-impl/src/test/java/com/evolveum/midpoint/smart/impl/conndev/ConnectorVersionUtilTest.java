/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConnectorVersionUtil#bumpMinor(String)} - the version computation
 * used when importing an existing low-code connector into the connector development.
 */
public class ConnectorVersionUtilTest {

    @Test
    public void twoComponent() {
        assertThat(ConnectorVersionUtil.bumpMinor("1.0")).isEqualTo("1.1");
        assertThat(ConnectorVersionUtil.bumpMinor("0.1")).isEqualTo("0.2");
        assertThat(ConnectorVersionUtil.bumpMinor("2.9")).isEqualTo("2.10");
        assertThat(ConnectorVersionUtil.bumpMinor("1.10")).isEqualTo("1.11");
    }

    @Test
    public void threeComponent() {
        assertThat(ConnectorVersionUtil.bumpMinor("1.2.3")).isEqualTo("1.3.0");
        assertThat(ConnectorVersionUtil.bumpMinor("0.0.9")).isEqualTo("0.1.0");
        assertThat(ConnectorVersionUtil.bumpMinor("10.20.30")).isEqualTo("10.21.0");
    }

    @Test
    public void singleComponent() {
        assertThat(ConnectorVersionUtil.bumpMinor("2")).isEqualTo("2.1");
    }

    @Test
    public void qualifierPreserved() {
        assertThat(ConnectorVersionUtil.bumpMinor("0.2-SNAPSHOT")).isEqualTo("0.3-SNAPSHOT");
        assertThat(ConnectorVersionUtil.bumpMinor("1.2.3-SNAPSHOT")).isEqualTo("1.3.0-SNAPSHOT");
        assertThat(ConnectorVersionUtil.bumpMinor("1.2.3-foo.bar")).isEqualTo("1.3.0-foo.bar");
        assertThat(ConnectorVersionUtil.bumpMinor("1.2.RC1")).isEqualTo("1.3.RC1");
    }

    @Test
    public void longComponent() {
        assertThat(ConnectorVersionUtil.bumpMinor("1.2.3.4")).isEqualTo("1.3.0");
    }

    @Test
    public void unparsableUnchanged() {
        assertThat(ConnectorVersionUtil.bumpMinor("abc")).isEqualTo("abc");
        assertThat(ConnectorVersionUtil.bumpMinor("v1.2")).isEqualTo("v1.2");
        assertThat(ConnectorVersionUtil.bumpMinor("")).isEqualTo("");
        assertThat(ConnectorVersionUtil.bumpMinor("   ")).isEqualTo("   ");
        assertThat(ConnectorVersionUtil.bumpMinor(null)).isNull();
    }

    @Test
    public void trimmedInput() {
        assertThat(ConnectorVersionUtil.bumpMinor(" 1.2 ")).isEqualTo("1.3");
    }
}
