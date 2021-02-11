/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

/**
 * Contract to implement to specify what columns to return how to extract/process them
 * for get/search operations.
 */
public interface ResultStyle {
    ResultTransformer getResultTransformer();
    List<String> getIdentifiers(String basePath);
    List<String> getContentAttributes(String basePath);
    String getCountString(String basePath);
}
