/**
 * Copyright (c) 2015-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardLayoutType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * @author semancik
 *
 */
public class AdminGuiConfigTypeUtil {

	public static AdminGuiConfigurationType compileAdminGuiConfiguration(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
			PrismObject<SystemConfigurationType> systemConfiguration) {

		// if there's no admin config at all, return null (to preserve original behavior)
		if (adminGuiConfigurations.isEmpty() &&
				(systemConfiguration == null || systemConfiguration.asObjectable().getAdminGuiConfiguration() == null)) {
			return null;
		}

		AdminGuiConfigurationType composite = new AdminGuiConfigurationType();
		if (systemConfiguration != null) {
			applyAdminGuiConfiguration(composite, systemConfiguration.asObjectable().getAdminGuiConfiguration());
		}
		for (AdminGuiConfigurationType adminGuiConfiguration: adminGuiConfigurations) {
			applyAdminGuiConfiguration(composite, adminGuiConfiguration);
		}
		return composite;
	}

	private static void applyAdminGuiConfiguration(AdminGuiConfigurationType composite, AdminGuiConfigurationType adminGuiConfiguration) {
		if (adminGuiConfiguration == null) {
			return;
		}
		composite.getAdditionalMenuLink().addAll(adminGuiConfiguration.getAdditionalMenuLink());
		composite.getUserDashboardLink().addAll(adminGuiConfiguration.getUserDashboardLink());
		if (adminGuiConfiguration.getDefaultTimezone() != null) {
			composite.setDefaultTimezone(adminGuiConfiguration.getDefaultTimezone());
		}
		if (adminGuiConfiguration.getPreferredDataLanguage() != null) {
			composite.setPreferredDataLanguage(adminGuiConfiguration.getPreferredDataLanguage());
		}
		if (adminGuiConfiguration.getObjectForms() != null) {
			if (composite.getObjectForms() == null) {
				composite.setObjectForms(adminGuiConfiguration.getObjectForms().clone());
			} else {
				for (ObjectFormType objectForm: adminGuiConfiguration.getObjectForms().getObjectForm()) {
					replaceForm(composite.getObjectForms(), objectForm.clone());
				}
			}
		}
		if (adminGuiConfiguration.getUserDashboard() != null) {
			if (composite.getUserDashboard() == null) {
				composite.setUserDashboard(adminGuiConfiguration.getUserDashboard().clone());
			} else {
				for (DashboardWidgetType widget: adminGuiConfiguration.getUserDashboard().getWidget()) {
					mergeWidget(composite.getUserDashboard(), widget);
				}
			}
		}
	}

	private static void replaceForm(ObjectFormsType objectForms, ObjectFormType newForm) {
		Iterator<ObjectFormType> iterator = objectForms.getObjectForm().iterator();
		while (iterator.hasNext()) {
			ObjectFormType currentForm = iterator.next();
			if (currentForm.getType().equals(newForm.getType())) {
				iterator.remove();
			}
		}
		objectForms.getObjectForm().add(newForm);
	}

	private static void mergeWidget(DashboardLayoutType compositeDashboard, DashboardWidgetType newWidget) {
		String newWidgetIdentifier = newWidget.getIdentifier();
		DashboardWidgetType compositeWidget = findWidget(compositeDashboard, newWidgetIdentifier);
		if (compositeWidget == null) {
			compositeDashboard.getWidget().add(newWidget.clone());
		} else {
			mergeWidget(compositeWidget, newWidget);
		}
	}

	private static void mergeWidget(DashboardWidgetType compositeWidget, DashboardWidgetType newWidget) {
		UserInterfaceElementVisibilityType newCompositeVisibility = mergeVisibility(compositeWidget.getVisibility(), newWidget.getVisibility());
		compositeWidget.setVisibility(newCompositeVisibility);
	}

	private static UserInterfaceElementVisibilityType mergeVisibility(
			UserInterfaceElementVisibilityType compositeVisibility, UserInterfaceElementVisibilityType newVisibility) {
		if (compositeVisibility == null) {
			compositeVisibility = UserInterfaceElementVisibilityType.VACANT;
		}
		if (newVisibility == null) {
			newVisibility = UserInterfaceElementVisibilityType.VACANT;
		}
		if (compositeVisibility == UserInterfaceElementVisibilityType.HIDDEN || newVisibility == UserInterfaceElementVisibilityType.HIDDEN) {
			return UserInterfaceElementVisibilityType.HIDDEN;
		}
		if (compositeVisibility == UserInterfaceElementVisibilityType.VISIBLE || newVisibility == UserInterfaceElementVisibilityType.VISIBLE) {
			return UserInterfaceElementVisibilityType.VISIBLE;
		}
		if (compositeVisibility == UserInterfaceElementVisibilityType.AUTOMATIC || newVisibility == UserInterfaceElementVisibilityType.AUTOMATIC) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		return UserInterfaceElementVisibilityType.VACANT;
	}

	private static DashboardWidgetType findWidget(DashboardLayoutType dashboard, String widgetIdentifier) {
		for (DashboardWidgetType widget: dashboard.getWidget()) {
			if (widget.getIdentifier().equals(widgetIdentifier)) {
				return widget;
			}
		}
		return null;
	}

}
