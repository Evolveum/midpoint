package com.evolveum.midpoint.schema.validator.processor;

import java.util.List;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class UserDashboardProcessor implements UpgradeObjectProcessor<Objectable> {

    @Override
    public String getIdentifier() {
        return "UserDashboard";
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
    public boolean process(PrismObject<Objectable> object, ItemPath path) {
        return true;
    }
}
