/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

/**
 * @author semancik
 */
public class TestUnitObjectWrapperFactory extends AbstractGuiUnitTest {

    // We cannot unit test user wrapper. It is invokind localization routines that require inialized application.

    // We cannot unit test shadow wrapper. It requires initialized resource, resource schema, capabilities, working model service, etc.


}
