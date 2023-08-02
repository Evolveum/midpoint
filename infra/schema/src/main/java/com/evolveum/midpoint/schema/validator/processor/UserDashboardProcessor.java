package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardLayoutType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HomePageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;

@SuppressWarnings("unused")
public class UserDashboardProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, AdminGuiConfigurationType.class, AdminGuiConfigurationType.F_USER_DASHBOARD);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) {
        AdminGuiConfigurationType adminGuiConfig = getItemParent(object, path);
        DashboardLayoutType dashboard = adminGuiConfig.getUserDashboard();

        List<HomePageType> homePages = adminGuiConfig.getHomePage();

        // todo implement, no idea how

        return false;
    }
}
