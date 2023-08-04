package com.evolveum.midpoint.schema.validator.processor;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class UserDashboardLinkProcessor implements UpgradeObjectProcessor<ObjectType> {

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
        return matchParentTypeAndItemName(object, path, AdminGuiConfigurationType.class, AdminGuiConfigurationType.F_USER_DASHBOARD_LINK);
    }

    @Override
    public String upgradeDescription(PrismObject<ObjectType> object, ItemPath path) {
        return "Authorization of user dashboard links has to be configured manually via widget/visibility.";
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) {
        AdminGuiConfigurationType adminGuiConfig = getItemParent(object, path);
        List<RichHyperlinkType> links = adminGuiConfig.getUserDashboardLink();

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

        for (RichHyperlinkType link : links) {
            PreviewContainerPanelConfigurationType widget = new PreviewContainerPanelConfigurationType();
            userHome.getWidget().add(widget);

            widget.setPanelType("linkWidget");
            widget.setDocumentation(link.getDocumentation());

            DisplayType display = new DisplayType();
            widget.setDisplay(display);

            if (link.getLabel() != null) {
                display.setLabel(new PolyStringType(link.getLabel()));
            }
            if (link.getDescription() != null) {
                display.setHelp(new PolyStringType(link.getDescription()));
            }

            display.setIcon(link.getIcon());

            if (link.getColor() != null) {
                IconType icon = display.getIcon();
                if (icon == null) {
                    icon = new IconType();
                    display.setIcon(icon);
                }

                icon.setCssClass(StringUtils.join(Arrays.asList(
                        icon.getCssClass(), link.getColor()), " "));
            }

            List<GuiActionType> actions = widget.getAction();
            GuiActionType action = new GuiActionType();
            action.setIdentifier("link");
            action.setTarget(new RedirectionTargetType()
                    .targetUrl(link.getTargetUrl()));
            actions.add(action);
        }

        links.clear();

        return true;
    }
}
