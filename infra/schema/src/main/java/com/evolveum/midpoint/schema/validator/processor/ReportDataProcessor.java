package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

@SuppressWarnings("unused")
public class ReportDataProcessor implements UpgradeObjectProcessor<ReportDataType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.AFTER;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.OPTIONAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(object, path, ReportDataType.class, ReportDataType.F_DATA);
    }

    @Override
    public boolean process(PrismObject<ReportDataType> object, ItemPath path) {
        return false;
    }
}
