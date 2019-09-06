/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * @author mederly
 */
public interface ResultStyle {
	ResultTransformer getResultTransformer();
	List<String> getIdentifiers(String basePath);
	List<String> getContentAttributes(String basePath);
	String getCountString(String basePath);
}
