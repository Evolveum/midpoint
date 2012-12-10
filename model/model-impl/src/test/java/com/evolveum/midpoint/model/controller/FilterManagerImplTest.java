/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

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
@ContextConfiguration(locations = { "classpath:ctx-model.xml",
		"classpath:ctx-model-unit-test.xml",
		"classpath:ctx-configuration-test-no-repo.xml",
		"classpath:ctx-task.xml",
		"classpath:ctx-audit.xml" })
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
