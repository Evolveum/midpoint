/**
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import org.testng.annotations.AfterClass;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Almost same sa TestOpenDj, but there is unsafeNameHint setting and maybe
 * some other possibly risky and alternative connector settings.
 *
 * @author semancik
 */
public class TestOpenDjUnsafe extends TestOpenDj {

	@Override
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource-unsafe.xml");
	}

}
