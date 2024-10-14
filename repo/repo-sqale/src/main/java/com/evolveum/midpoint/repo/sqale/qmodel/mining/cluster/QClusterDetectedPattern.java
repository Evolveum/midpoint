/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.MOutlier;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QClusterDetectedPattern extends QContainer<MClusterDetectedPattern, MClusterObject> {

    public static final String TABLE_NAME = "m_role_analysis_cluster_detected_pattern";
    public static final String ALIAS = "cdp";


    public static final ColumnMetadata REDUCTION_COUNT =
            ColumnMetadata.named("reductionCount").ofType(Types.DOUBLE);

    public QClusterDetectedPattern(String variable) {
        super(MClusterDetectedPattern.class, variable , DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public NumberPath<Double> reductionCount = createNumber("reductionCount", Double.class, REDUCTION_COUNT);
}
