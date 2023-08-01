/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@SuppressWarnings("unused")
public class ReportOutputOidProcessor implements UpgradeObjectProcessor<TaskType> {

    private final static ItemName REPORT_OUTPUT_OID = new ItemName(SchemaConstants.NS_REPORT_EXTENSION, "reportOutputOid");
    private static final ItemName REPORT_DATA_PARAM = new ItemName(SchemaConstants.NS_REPORT_EXTENSION, "reportDataParam");

    private static final ItemPath REPORT_OUTPUT_OID_PATH = ItemPath.create(TaskType.F_EXTENSION, REPORT_OUTPUT_OID);

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.CRITICAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(
                object, path, TaskType.class, REPORT_OUTPUT_OID_PATH);
    }

    @Override
    public boolean process(PrismObject<TaskType> object, ItemPath path) throws Exception {
        PrismProperty<String> property = object.findItem(REPORT_OUTPUT_OID_PATH, PrismProperty.class);
        String oid = property.getRealValue();

        PrismContainerValue extension = property.getParent();
        extension.findOrCreateReference(REPORT_DATA_PARAM).add(
                new ObjectReferenceType()
                        .oid(oid)
                        .asReferenceValue()
        );

        extension.remove(property);

        return true;
    }
}
