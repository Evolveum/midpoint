package com.evolveum.midpoint.ninja.action.upgrade.processor;

import java.util.List;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeObjectProcessor;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradePhase;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradePriority;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class UserDashboardProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public String getIdentifier() {
        return "GUI-1234";
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.OPTIONAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.PREVIEW;
    }

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        AdminGuiConfigurationType configuration = null;
        if (object.isOfType(SystemConfigurationType.class)) {
            SystemConfigurationType obj = (SystemConfigurationType) object.asObjectable();
            configuration = obj.getAdminGuiConfiguration();
        } else if (object.isOfType(UserType.class)) {
            UserType obj = (UserType) object.asObjectable();
            configuration = obj.getAdminGuiConfiguration();
        } else if (object.isOfType(AbstractRoleType.class)) {
            AbstractRoleType obj = (AbstractRoleType) object.asObjectable();
            configuration = obj.getAdminGuiConfiguration();
        }

        if (configuration == null) {
            return false;
        }

        List<RichHyperlinkType> links = configuration.getUserDashboardLink();
        return !links.isEmpty();
    }

    @Override
    public boolean process(PrismObject<ObjectType> object) {
        return true;
    }
}
