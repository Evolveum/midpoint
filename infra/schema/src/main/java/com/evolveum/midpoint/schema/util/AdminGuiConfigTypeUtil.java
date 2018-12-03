/*
 * Copyright (c) 2015-2018 Evolveum
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

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.*;
import java.util.function.BooleanSupplier;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class AdminGuiConfigTypeUtil {

	public static DashboardWidgetType findWidget(DashboardLayoutType dashboard, String widgetIdentifier) {
		return findFeature(dashboard.getWidget(), widgetIdentifier);
	}

	public static <T extends UserInterfaceFeatureType> T findFeature(List<T> features, String identifier) {
		for (T feature: features) {
			if (feature.getIdentifier().equals(identifier)) {
				return feature;
			}
		}
		return null;
	}

	public static UserInterfaceElementVisibilityType getFeatureVisibility(AdminGuiConfigurationType adminGuiConfig, String identifier) {
		if (adminGuiConfig == null) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		UserInterfaceFeatureType feature = findFeature(adminGuiConfig.getFeature(), identifier);
		if (feature == null) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		UserInterfaceElementVisibilityType visibility = feature.getVisibility();
		if (visibility == null) {
			return UserInterfaceElementVisibilityType.AUTOMATIC;
		}
		return visibility;
	}

	public static boolean isFeatureVisible(AdminGuiConfigurationType adminGuiConfig, String identifier) {
		return isFeatureVisible(adminGuiConfig, identifier, null);
	}

	public static boolean isFeatureVisible(AdminGuiConfigurationType adminGuiConfig, String identifier, BooleanSupplier automaticPredicate) {
		UserInterfaceElementVisibilityType visibility = getFeatureVisibility(adminGuiConfig, identifier);
		return isVisible(visibility, automaticPredicate);
	}

	public static boolean isVisible(UserInterfaceElementVisibilityType visibility, BooleanSupplier automaticPredicate) {
		if (visibility == UserInterfaceElementVisibilityType.HIDDEN) {
			return false;
		}
		if (visibility == UserInterfaceElementVisibilityType.VISIBLE) {
			return true;
		}
		if (visibility == UserInterfaceElementVisibilityType.AUTOMATIC) {
			if (automaticPredicate == null) {
				return true;
			} else {
				return automaticPredicate.getAsBoolean();
			}
		}
		return false;
	}

	public static <O extends ObjectType> GuiObjectDetailsPageType findObjectConfiguration(Class<O> type, AdminGuiConfigurationType adminGuiConfig) {
		if (adminGuiConfig == null) {
			return null;
		}
		GuiObjectDetailsSetType objectDetailsSetType = adminGuiConfig.getObjectDetails();
		if (objectDetailsSetType == null) {
			return null;
		}
		return AdminGuiConfigTypeUtil.findObjectConfiguration(objectDetailsSetType.getObjectDetailsPage(), type);
	}

	public static <T extends AbstractObjectTypeConfigurationType, O extends ObjectType> T findObjectConfiguration(
			List<T> list, Class<O> type) {
		if (list == null) {
			return null;
		}
		QName typeQName = ObjectTypes.getObjectType(type).getTypeQName();
		for (T item: list) {
			if (QNameUtil.match(item.getType(), typeQName)) {
				return item;
			}
		}
		for (T item: list) {
			if (item.getType() == null) {
				return item;
			}
		}
		return null;
	}

}
