package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

@SuppressWarnings("unused")
public class UserDashboardProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
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
        return matchParentTypeAndItemName(object, path, AdminGuiConfigurationType.class, AdminGuiConfigurationType.F_USER_DASHBOARD);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) {
        AdminGuiConfigurationType adminGuiConfig = getItemParent(object, path);

        List<HomePageType> homePages = adminGuiConfig.getHomePage();
        HomePageType userHome = homePages.stream()
                .filter(hp -> UserType.COMPLEX_TYPE.equals(hp.getType()))
                .findFirst().orElse(null);

        if (userHome == null) {
            userHome = new HomePageType();
            userHome.setType(UserType.COMPLEX_TYPE);
            userHome.setIdentifier("user");
            homePages.add(userHome);
        }

        DashboardLayoutType dashboard = adminGuiConfig.getUserDashboard();
        List<DashboardWidgetType> dashboardWidgets = dashboard.getWidget();

        for (DashboardWidgetType dw : dashboardWidgets) {
            PreviewContainerPanelConfigurationType widget = new PreviewContainerPanelConfigurationType();
            userHome.getWidget().add(widget);

            copyUserInterfaceFeature(dw, widget);

            dw.getData();
            dw.getPresentation();
        }

        adminGuiConfig.setUserDashboard(null);

        return true;
    }
}
