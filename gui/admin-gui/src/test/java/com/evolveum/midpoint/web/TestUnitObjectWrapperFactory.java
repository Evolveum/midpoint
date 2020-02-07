/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

/**
 * @author semancik
 */
public class TestUnitObjectWrapperFactory extends AbstractGuiUnitTest {

    // We cannot unit test user wrapper. It is invokind localization routines that require inialized application.

    // We cannot unit test shadow wrapper. It requires initialized resource, resource schema, capabilities, working model service, etc.


}
