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
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.map.HashedMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BooleanSupplier;

import javax.xml.namespace.QName;

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
		if (adminGuiConfiguration.isEnableExperimentalFeatures() != null) {
			composite.setEnableExperimentalFeatures(adminGuiConfiguration.isEnableExperimentalFeatures());
		}
		if (adminGuiConfiguration.getDefaultExportSettings() != null) {
			composite.setDefaultExportSettings(adminGuiConfiguration.getDefaultExportSettings());
		}
		if (adminGuiConfiguration.getObjectLists() != null) {
			if (composite.getObjectLists() == null) {
				composite.setObjectLists(adminGuiConfiguration.getObjectLists().clone());
			} else {
				for (GuiObjectListType objectList: adminGuiConfiguration.getObjectLists().getObjectList()) {
					mergeList(composite.getObjectLists(), objectList);
				}
			}
		}
		if (adminGuiConfiguration.getObjectForms() != null) {
			if (composite.getObjectForms() == null) {
				composite.setObjectForms(adminGuiConfiguration.getObjectForms().clone());
			} else {
				for (ObjectFormType objectForm: adminGuiConfiguration.getObjectForms().getObjectForm()) {
					joinForms(composite.getObjectForms(), objectForm);
				}
			}
		}
		if (adminGuiConfiguration.getObjectDetails() != null) {
			if (composite.getObjectDetails() == null) {
				composite.setObjectDetails(adminGuiConfiguration.getObjectDetails().clone());
			} else {
				for (GuiObjectDetailsPageType objectDetails: adminGuiConfiguration.getObjectDetails().getObjectDetailsPage()) {
					joinObjectDetails(composite.getObjectDetails(), objectDetails);
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
		for (UserInterfaceFeatureType feature: adminGuiConfiguration.getFeature()) {
			mergeFeature(composite.getFeature(), feature);
		}
		if (composite.getObjectLists() != null && composite.getObjectLists().getObjectList() != null){
			for (GuiObjectListType objectListType : composite.getObjectLists().getObjectList()){
				if (objectListType.getColumn() != null) {
//					objectListType.getColumn().clear();
//					objectListType.getColumn().addAll(orderCustomColumns(objectListType.getColumn()));
					List<GuiObjectColumnType> orderedList = orderCustomColumns(objectListType.getColumn());
					objectListType.getColumn().clear();
					objectListType.getColumn().addAll(orderedList);
				}
			}
		}
	}

	private static void joinForms(ObjectFormsType objectForms, ObjectFormType newForm) {
		Iterator<ObjectFormType> iterator = objectForms.getObjectForm().iterator();
		while (iterator.hasNext()) {
			ObjectFormType currentForm = iterator.next();
			if (isTheSameObjectForm(currentForm, newForm)) {
				iterator.remove();
			}
		}
		objectForms.getObjectForm().add(newForm.clone());
	}
	
	private static void joinObjectDetails(GuiObjectDetailsSetType objectDetailsSet, GuiObjectDetailsPageType newObjectDetails) {
		Iterator<GuiObjectDetailsPageType> iterator = objectDetailsSet.getObjectDetailsPage().iterator();
		while (iterator.hasNext()) {
			GuiObjectDetailsPageType currentDetails = iterator.next();
			if (isTheSameObjectType(currentDetails, newObjectDetails)) {
				iterator.remove();
			}
		}
		objectDetailsSet.getObjectDetailsPage().add(newObjectDetails.clone());
	}
	
	private static boolean isTheSameObjectType(AbstractObjectTypeConfigurationType oldConf, AbstractObjectTypeConfigurationType newConf) {
		return QNameUtil.match(oldConf.getType(), newConf.getType());
	}

	private static boolean isTheSameObjectForm(ObjectFormType oldForm, ObjectFormType newForm){
		if (!isTheSameObjectType(oldForm,newForm)) {
			return false;
		}
		if (oldForm.isIncludeDefaultForms() != null &&
				newForm.isIncludeDefaultForms() != null){
			return true;
		}
		if (oldForm.getFormSpecification() == null && newForm.getFormSpecification() == null) {
			String oldFormPanelUri = oldForm.getFormSpecification().getPanelUri();
			String newFormPanelUri = newForm.getFormSpecification().getPanelUri();
			if (oldFormPanelUri != null && oldFormPanelUri.equals(newFormPanelUri)) {
				return true;
			}

			String oldFormPanelClass = oldForm.getFormSpecification().getPanelClass();
			String newFormPanelClass = newForm.getFormSpecification().getPanelClass();
			if (oldFormPanelClass != null && oldFormPanelClass.equals(newFormPanelClass)) {
				return true;
			}

			String oldFormRefOid = oldForm.getFormSpecification().getFormRef() == null ?
					null : oldForm.getFormSpecification().getFormRef().getOid();
			String newFormRefOid = newForm.getFormSpecification().getFormRef() == null ?
					null : newForm.getFormSpecification().getFormRef().getOid();
			if (oldFormRefOid != null && oldFormRefOid.equals(newFormRefOid)) {
				return true;
			}
		}
		return false;
	}

	private static void mergeList(GuiObjectListsType objectLists, GuiObjectListType newList) {
		// We support only the default object lists now, so simply replace the existing definition with the
		// latest definition. We will need a more sophisticated merging later.
		Iterator<GuiObjectListType> iterator = objectLists.getObjectList().iterator();
		while (iterator.hasNext()) {
			GuiObjectListType currentList = iterator.next();
			if (currentList.getType().equals(newList.getType())) {
				iterator.remove();
			}
		}
		objectLists.getObjectList().add(newList.clone());
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
	
	public static DashboardWidgetType findWidget(DashboardLayoutType dashboard, String widgetIdentifier) {
		return findFeature(dashboard.getWidget(), widgetIdentifier);
	}

	private static void mergeWidget(DashboardWidgetType compositeWidget, DashboardWidgetType newWidget) {
		mergeFeature(compositeWidget, newWidget, UserInterfaceElementVisibilityType.VACANT);
		// merge other widget properties (in the future)
	}
	
	private static void mergeFeature(List<UserInterfaceFeatureType> compositeFeatures, UserInterfaceFeatureType newFeature) {
		String newIdentifier = newFeature.getIdentifier();
		UserInterfaceFeatureType compositeFeature = findFeature(compositeFeatures, newIdentifier);
		if (compositeFeature == null) {
			compositeFeatures.add(newFeature.clone());
		} else {
			mergeFeature(compositeFeature, newFeature, UserInterfaceElementVisibilityType.AUTOMATIC);
		}
	}
	
	private static <T extends UserInterfaceFeatureType> void mergeFeature(T compositeFeature, T newFeature, UserInterfaceElementVisibilityType defaultVisibility) {
		UserInterfaceElementVisibilityType newCompositeVisibility = mergeVisibility(compositeFeature.getVisibility(), newFeature.getVisibility(), defaultVisibility);
		compositeFeature.setVisibility(newCompositeVisibility);
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

	private static UserInterfaceElementVisibilityType mergeVisibility(
			UserInterfaceElementVisibilityType compositeVisibility, UserInterfaceElementVisibilityType newVisibility, UserInterfaceElementVisibilityType defaultVisibility) {
		if (compositeVisibility == null) {
			compositeVisibility = defaultVisibility;
		}
		if (newVisibility == null) {
			newVisibility = defaultVisibility;
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

	/*
	the ordering algorithm is: the first level is occupied by
	the column which previousColumn == null || "" || notExistingColumnNameValue.
	Each next level contains columns which
	previousColumn == columnNameFromPreviousLevel
	 */
	public static List<GuiObjectColumnType> orderCustomColumns(List<GuiObjectColumnType> customColumns){
		if (customColumns == null || customColumns.size() == 0){
			return new ArrayList<>();
		}
		List<GuiObjectColumnType> customColumnsList = new ArrayList<>();
		customColumnsList.addAll(customColumns);
		List<String> previousColumnValues = new ArrayList<>();
		previousColumnValues.add(null);
		previousColumnValues.add("");

		Map<String, String> columnRefsMap = new HashedMap();
		for (GuiObjectColumnType column : customColumns){
			columnRefsMap.put(column.getName(), column.getPreviousColumn() == null ? "" : column.getPreviousColumn());
		}

		List<String> temp = new ArrayList<> ();
		int index = 0;
		while (index < customColumns.size()){
			int sortFrom = index;
			for (int i = index; i < customColumnsList.size(); i++){
				GuiObjectColumnType column = customColumnsList.get(i);
				if (previousColumnValues.contains(column.getPreviousColumn()) ||
						!columnRefsMap.containsKey(column.getPreviousColumn())){
					Collections.swap(customColumnsList, index, i);
					index++;
					temp.add(column.getName());
				}
			}
			if (temp.size() == 0){
				temp.add(customColumnsList.get(index).getName());
				index++;
			}
			if (index - sortFrom > 1){
				Collections.sort(customColumnsList.subList(sortFrom, index - 1), new Comparator<GuiObjectColumnType>() {

					@Override
					public int compare(GuiObjectColumnType o1, GuiObjectColumnType o2) {
						return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
					}
				});
			}
			previousColumnValues.clear();
			previousColumnValues.addAll(temp);
			temp.clear();
		}
		return customColumnsList;
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
		return null;
	}

}
