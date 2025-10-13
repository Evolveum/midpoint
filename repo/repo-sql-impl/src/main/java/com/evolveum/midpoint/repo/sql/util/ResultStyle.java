/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
