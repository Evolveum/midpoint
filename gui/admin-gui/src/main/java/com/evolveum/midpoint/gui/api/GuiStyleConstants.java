/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.gui.api;

/**
 * @author semancik
 *
 */
public class GuiStyleConstants {
	
	public static final String CLASS_BOX = "box";
	public static final String CLASS_BOX_DEFAULT = "box-default";
	
	public static final String CLASS_OBJECT_USER_ICON = "fa fa-user";
	public static final String CLASS_OBJECT_USER_ICON_COLORED = CLASS_OBJECT_USER_ICON + " object-user-color";
	public static final String CLASS_OBJECT_USER_BOX_CSS_CLASSES = "object-user-box";
	public static final String CLASS_OBJECT_USER_BOX_THIN_CSS_CLASSES = "object-user-box-thin";
	
	public static final String CLASS_OBJECT_ROLE_ICON = "fa fa-street-view";
	public static final String CLASS_OBJECT_ROLE_BG = "object-role-bg";
	public static final String CLASS_DISABLED_OBJECT_ROLE_BG = "object-disabled-bg";
	public static final String CLASS_OBJECT_ROLE_ICON_COLORED = CLASS_OBJECT_ROLE_ICON + " object-role-color";
	public static final String CLASS_OBJECT_ROLE_BOX_CSS_CLASSES = "object-role-box";
	public static final String CLASS_OBJECT_ROLE_BOX_THIN_CSS_CLASSES = "object-role-box-thin";
	
	public static final String CLASS_OBJECT_ORG_ICON = "fa fa-building";
    public static final String CLASS_OBJECT_ORG_BG = "object-org-bg";
	public static final String CLASS_OBJECT_ORG_ICON_COLORED = CLASS_OBJECT_ORG_ICON + " object-org-color";
	public static final String CLASS_OBJECT_ORG_BOX_CSS_CLASSES = "object-org-box";
	public static final String CLASS_OBJECT_ORG_BOX_THIN_CSS_CLASSES = "object-org-box-thin";
	
	public static final String CLASS_OBJECT_SERVICE_ICON = "fa fa-cloud";
    public static final String CLASS_OBJECT_SERVICE_BG = "object-service-bg";
	public static final String CLASS_OBJECT_SERVICE_ICON_COLORED = CLASS_OBJECT_SERVICE_ICON + " object-service-color";
	public static final String CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES = "object-service-box";
	public static final String CLASS_OBJECT_SERVICE_BOX_THIN_CSS_CLASSES = "object-service-box-thin";
	
	public static final String CLASS_OBJECT_RESOURCE_ICON = "fa fa-database";
	public static final String CLASS_OBJECT_RESOURCE_ICON_COLORED = CLASS_OBJECT_RESOURCE_ICON + " object-resource-color";
	public static final String CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES = "object-resource-box";
	public static final String CLASS_OBJECT_RESOURCE_BOX_THIN_CSS_CLASSES = "object-resource-box-thin";
	
	public static final String CLASS_OBJECT_SHADOW_ICON = "fa fa-eye";
	public static final String CLASS_OBJECT_SHADOW_ICON_COLORED = CLASS_OBJECT_RESOURCE_ICON + " object-shadow-color";
	public static final String CLASS_OBJECT_SHADOW_BOX_CSS_CLASSES = "object-shadow-box";
	public static final String CLASS_OBJECT_SHADOW_BOX_THIN_CSS_CLASSES = "object-shadow-box-thin";
	
	public static final String CLASS_OBJECT_TASK_ICON = "fa fa-tasks";
	public static final String CLASS_OBJECT_TASK_ICON_COLORED = CLASS_OBJECT_TASK_ICON + " object-task-color";
	public static final String CLASS_OBJECT_TASK_BOX_CSS_CLASSES = "object-task-box";
	public static final String CLASS_OBJECT_TASK_BOX_THIN_CSS_CLASSES = "object-task-box-thin";

	public static final String CLASS_OBJECT_WORK_ITEM_ICON = "fa fa-inbox";
	public static final String CLASS_OBJECT_WORK_ITEM_ICON_COLORED = CLASS_OBJECT_WORK_ITEM_ICON + " object-task-color";		// TODO
	public static final String CLASS_OBJECT_WORK_ITEM_BOX_CSS_CLASSES = "object-task-box"; // TODO

	public static final String CLASS_OBJECT_CERT_DEF_ICON = "fa fa-certificate";
	public static final String CLASS_OBJECT_CERT_DEF_ICON_COLORED = CLASS_OBJECT_CERT_DEF_ICON + " object-task-color";		// TODO
	public static final String CLASS_OBJECT_CERT_DEF_BOX_CSS_CLASSES = "object-task-box";									// TODO

	public static final String CLASS_OBJECT_CERT_CAMPAIGN_ICON = "fa fa-gavel";
	public static final String CLASS_OBJECT_CERT_CAMPAIGN_ICON_COLORED = CLASS_OBJECT_CERT_CAMPAIGN_ICON + " object-task-color";		// TODO

	public static final String CLASS_ICON_STYLE = "icon-style-";  //some icon styles start with this string
	public static final String CLASS_ICON_STYLE_NORMAL = "icon-style-normal";
	public static final String CLASS_ICON_STYLE_DISABLED = "icon-style-disabled";
	public static final String CLASS_ICON_STYLE_ARCHIVED = "icon-style-archived";
	public static final String CLASS_ICON_STYLE_PRIVILEGED = "icon-style-privileged";
	public static final String CLASS_ICON_STYLE_END_USER = "icon-style-end-user";
	public static final String CLASS_ICON_STYLE_MANAGER = "icon-style-manager";
	public static final String CLASS_ICON_STYLE_WARNING = "icon-style-warning";
	public static final String CLASS_ICON_STYLE_UP = "icon-style-up";
	public static final String CLASS_ICON_STYLE_DOWN = "icon-style-down";
	public static final String CLASS_ICON_STYLE_BROKEN = "icon-style-broken";
	
	public static final String CLASS_SHADOW_ICON_ACCOUNT = "fa fa-male";
	public static final String CLASS_SHADOW_ICON_ENTITLEMENT = "fa fa-group";
	public static final String CLASS_SHADOW_ICON_GENERIC = "fa fa-circle-o";
	public static final String CLASS_SHADOW_ICON_PROTECTED = "fa fa-shield";
	public static final String CLASS_SHADOW_ICON_UNKNOWN = "fa fa-eye";

	public static final String CLASS_APPROVAL_OUTCOME_ICON_UNKNOWN_COLORED = "fa fa-check text-warning";
	public static final String CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED = "fa fa-check text-success";
	public static final String CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED = "fa fa-times text-danger";
	public static final String CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED = "fa fa-clock-o text-info";

	public static final String CLASS_ICON_EXPAND = "fa fa-plus";
	public static final String CLASS_ICON_COLLAPSE = "fa fa-minus";
	public static final String CLASS_ICON_SORT_AMOUNT_ASC = "fa fa-sort-amount-asc";
	public static final String CLASS_ICON_SORT_ALPHA_ASC = "fa fa-sort-alpha-asc";
	public static final String CLASS_ICON_SHOW_EMPTY_FIELDS = "fa fa-square-o";
	public static final String CLASS_ICON_NOT_SHOW_EMPTY_FIELDS = "fa fa-square";
	public static final String CLASS_ICON_SHOW_METADATA = "fa fa-asterisk";
	public static final String CLASS_ICON_SUPERUSER = "fa fa-shield";
	public static final String CLASS_ICON_NO_OBJECTS = "fa fa-times";
	public static final String CLASS_ICON_ACTIVATION_ACTIVE = "fa fa-check";
	public static final String CLASS_ICON_ACTIVATION_INACTIVE = "fa fa-times";
	public static final String CLASS_ICON_ASSIGNMENTS = "fa fa-bank";
	public static final String CLASS_SHADOW_ICON_REQUEST = "fa fa-pencil-square-o";
	public static final String CLASS_ICON_TACHOMETER = "fa fa-tachometer";

	public static final String CLASS_OP_RESULT_STATUS_ICON_UNKNOWN_COLORED = "fa fa-question-circle text-warning";
	public static final String CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED = "fa fa-check-circle text-success";
	public static final String CLASS_OP_RESULT_STATUS_ICON_WARNING_COLORED = "fa fa-exclamation-circle text-warning";
	public static final String CLASS_OP_RESULT_STATUS_ICON_PARTIAL_ERROR_COLORED = "fa fa-minus-circle text-danger";
	public static final String CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED = "fa fa-times-circle text-danger";
	public static final String CLASS_OP_RESULT_STATUS_ICON_HANDLED_ERROR_COLORED = "fa fa-minus-circle text-warning";
	public static final String CLASS_OP_RESULT_STATUS_ICON_NOT_APPLICABLE_COLORED = "fa fa-check-circle text-muted";
	public static final String CLASS_OP_RESULT_STATUS_ICON_IN_PROGRESS_COLORED = "fa fa-clock-o text-info";

	//menu items icons classes
	public static final String CLASS_THREE_DOTS = "fa fa-ellipsis-h";
	public static final String CLASS_RECONCILE_MENU_ITEM = "fa fa-exchange";
	public static final String CLASS_TEST_CONNECTION_MENU_ITEM = "fa fa-question";
	public static final String CLASS_DELETE_MENU_ITEM = "fa fa-minus";
	public static final String CLASS_EDIT_MENU_ITEM = "fa fa-edit";
	public static final String CLASS_SUSPEND_MENU_ITEM = "fa fa-pause";
	public static final String CLASS_RESUME_MENU_ITEM = "fa fa-check-square";
	public static final String CLASS_STOP_MENU_ITEM = "fa fa-stop";
	public static final String CLASS_START_MENU_ITEM = "fa fa-play";

	public static final String CLASS_BUTTON_TOGGLE_OFF = "btn-default";
	public static final String CLASS_BUTTON_TOGGLE_ON = "btn-info";

	public static final String CLASS_BADGE_ACTIVE = "badge-active";
	public static final String CLASS_BADGE_PASSIVE = "badge-passive";

	public static final String DEFAULT_BG_COLOR = "#3c8dbc"; //blue-light theme

	public static final String CLASS_PLUS_CIRCLE = "fa fa-plus-circle";
	public static final String CLASS_PLUS_CIRCLE_SUCCESS = CLASS_PLUS_CIRCLE + " text-success";
	public static final String CLASS_MINUS_CIRCLE = "fa fa-minus-circle";
	public static final String CLASS_MINUS_CIRCLE_DANGER = CLASS_MINUS_CIRCLE + " text-danger";
	public static final String CLASS_CIRCLE_FULL = "fa fa-circle";

	public static final String CLASS_FILE_TEXT = "fa fa-file-text-o";

}
