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
package com.evolveum.icf.dummy.connector;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;

/**
 * @author Radovan Semancik
 *
 */
public class DummyFilterTranslator implements FilterTranslator<Filter> {

	private static final Log log = Log.getLog(DummyFilterTranslator.class);

	/* (non-Javadoc)
	 * @see org.identityconnectors.framework.common.objects.filter.FilterTranslator#translate(org.identityconnectors.framework.common.objects.filter.Filter)
	 */
	public List<Filter> translate(Filter filter) {
		log.info("translate");
		ArrayList<Filter> filters = new ArrayList<Filter>();
		filters.add(filter);
		return filters;
	}

}
