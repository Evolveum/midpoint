/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportReconDeprecated extends TestImportRecon {

	@Override
	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_DEPRECATED_FILE;
	}

	@Override
	protected File getDummyResourceLimeFile() {
		return RESOURCE_DUMMY_LIME_DEPRECATED_FILE;
	}

	@Override
	protected File getDummyResourceAzureFile() {
		return RESOURCE_DUMMY_AZURE_DEPRECATED_FILE;
	}

	@Override
	public void test500ImportTAugustusFromResourceDummy() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test502ImportAugustusFromResourceDummy() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test510ImportFromResourceDummy() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test600SearchAllDummyAccounts() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test610SearchDummyAccountsNameSubstring() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test900DeleteDummyShadows() throws Exception {
		// Not relevant for deprecated syntax
	}

	@Override
	public void test910DeleteDummyAccounts() throws Exception {
		// Not relevant for deprecated syntax
	}
	
}
