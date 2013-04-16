/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
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
