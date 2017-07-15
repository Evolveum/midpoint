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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.map.HashedMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
		if (adminGuiConfiguration.getUserDashboard() != null) {
			if (composite.getUserDashboard() == null) {
				composite.setUserDashboard(adminGuiConfiguration.getUserDashboard().clone());
			} else {
				for (DashboardWidgetType widget: adminGuiConfiguration.getUserDashboard().getWidget()) {
					mergeWidget(composite.getUserDashboard(), widget);
				}
			}
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

	private static boolean isTheSameObjectForm(ObjectFormType oldForm, ObjectFormType newForm){
		if (!oldForm.getType().equals(newForm.getType())){
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

	public static DashboardWidgetType findWidget(DashboardLayoutType dashboard, String widgetIdentifier) {
		for (DashboardWidgetType widget: dashboard.getWidget()) {
			if (widget.getIdentifier().equals(widgetIdentifier)) {
				return widget;
			}
		}
		return null;
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

}
