/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class FilterManagerImplTest extends AbstractTestNGSpringContextTests  {

	@Autowired(required = true)
	private FilterManager<? extends Filter> manager;

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void setFilterMappingNull() {
		manager.setFilterMapping(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getFilterInstanceNullUri() {
		manager.getFilterInstance(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getFilterInstanceEmptyUri() {
		manager.getFilterInstance("");
	}

	@Test
	public void getFilterInstanceNullParameters() {
		Filter filter = manager.getFilterInstance(
				"http://midpoint.evolveum.com/xml/ns/public/common/value-filter-1.xsd#emptyFilter", null);
		assertNotNull(filter);
	}

	@Test
	public void getFilterInstanceNotExistingFilter() {
		Filter filter = manager.getFilterInstance("notExistingUri", null);
		assertNull(filter);
	}
}
