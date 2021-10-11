/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api;

/**
 * @author semancik
 *
 */
public class GuiStyleConstants {

    public static final String CLASS_BOX = "box";
    public static final String CLASS_BOX_DEFAULT = "box-default";

    public static final String CLASS_DASHBOARD_ICON = "fa fa-dashboard";

    public static final String CLASS_OBJECT_USER_ICON = "fa fa-user";
    public static final String CLASS_OBJECT_USER_ICON_COLORED = CLASS_OBJECT_USER_ICON + " object-user-color";
    public static final String CLASS_OBJECT_USER_BOX_CSS_CLASSES = "object-user-box";
    public static final String CLASS_OBJECT_USER_BOX_THIN_CSS_CLASSES = "object-user-box-thin";

    public static final String CLASS_OBJECT_ROLE_ICON = "fe fe-role_icon";
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
    public static final String CLASS_OBJECT_WORK_ITEM_ICON_COLORED = CLASS_OBJECT_WORK_ITEM_ICON + " object-task-color";        // TODO
    public static final String CLASS_OBJECT_WORK_ITEM_BOX_CSS_CLASSES = "object-task-box"; // TODO

    public static final String CLASS_OBJECT_CERT_DEF_ICON = "fa fa-certificate";
    public static final String CLASS_OBJECT_CERT_DEF_ICON_COLORED = CLASS_OBJECT_CERT_DEF_ICON + " object-task-color";        // TODO
    public static final String CLASS_OBJECT_CERT_DEF_BOX_CSS_CLASSES = "object-task-box";                                    // TODO

    public static final String CLASS_OBJECT_CERT_CAMPAIGN_ICON = "fa fa-gavel";
    public static final String CLASS_OBJECT_CERT_CAMPAIGN_ICON_COLORED = CLASS_OBJECT_CERT_CAMPAIGN_ICON + " object-task-color";        // TODO

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

    public static final String CLASS_ICON_DASHBOARD = "fa fa-dashboard";
    public static final String CLASS_ICON_PROFILE = "fa fa-user";
    public static final String CLASS_ICON_CREDENTIALS = "fa fa-shield";
    public static final String CLASS_ICON_REQUEST = "fa fa-pencil-square-o";
    public static final String CLASS_ICON_CONSENT = "fa fa-check-square-o";

    public static final String CLASS_APPROVAL_OUTCOME_ICON_UNKNOWN_COLORED = "fa fa-check text-warning";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_APPROVED_COLORED = "fa fa-check text-success";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_REJECTED_COLORED = "fa fa-times text-danger";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_SKIPPED_COLORED = "fa fa-step-forward text-success";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_FORWARDED_COLORED = "fa  fa-fast-forward text-success";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_IN_PROGRESS_COLORED = "fa fa-clock-o text-info";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_FUTURE_COLORED = "fa fa-question-circle text-warning";
    public static final String CLASS_APPROVAL_OUTCOME_ICON_CANCELLED_COLORED = "fa fa-ban text-warning";

    public static final String CLASS_ICON_EXPAND = "fa fa-chevron-left";
    public static final String CLASS_ICON_COLLAPSE = "fa  fa-chevron-down";
    public static final String CLASS_ICON_SORT_AMOUNT_ASC = "fa fa-sort-amount-asc";
    public static final String CLASS_ICON_SORT_ALPHA_ASC = "fa fa-sort-alpha-asc";
    public static final String CLASS_ICON_SHOW_EMPTY_FIELDS = "fa fa-square-o";
    public static final String CLASS_ICON_NOT_SHOW_EMPTY_FIELDS = "fa fa-square";
    public static final String CLASS_ICON_SHOW_METADATA = "fa fa-asterisk";
    public static final String CLASS_ICON_SUPERUSER = "fa fa-shield";
    public static final String CLASS_ICON_NO_OBJECTS = "fa fa-times";
    public static final String CLASS_ICON_ACTIVATION_ACTIVE = "fa fa-check";
    public static final String CLASS_ICON_ACTIVATION_INACTIVE = "fa fa-times";
    public static final String CLASS_ICON_RESOURCE_BROKEN = "fa fa-exclamation";
    public static final String CLASS_ICON_RESOURCE_UNKNOWN = "fa fa-question";
    public static final String CLASS_ICON_ASSIGNMENTS = "fa fa-bank";
    public static final String CLASS_SHADOW_ICON_REQUEST = "fa fa-pencil-square-o";
    public static final String CLASS_ICON_TACHOMETER = "fa fa-tachometer";
    public static final String CLASS_ICON_COLLAPSE_CONTAINER = "fa fa-caret-down fa-lg";
    public static final String CLASS_ICON_EXPAND_CONTAINER = "fa fa-caret-right fa-lg";

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
    public static final String CLASS_IMPORT_MENU_ITEM = "fa fa-download";
    public static final String CLASS_NAVIGATE_ARROW = "fa fa-share";

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

    /**
     * Plus icon used for overlaid buttons (button with + or - in the bottom-right corner)
     */
    public static final String CLASS_BUTTON_OVERLAY_PLUS = "fa fa-plus";

    public static final String CLASS_FILE_TEXT = "fa fa-file-text-o";
    public static final String CLASS_FILE_EXCEL = "fa fa-file-excel-o";
    public static final String CLASS_FILE_BLACK_FILLED = "fa fa-file";
    public static final String CLASS_FILE_WHITE_FILLED = "fa fa-file-o";
    public static final String CLASS_FILE_ARCHIVE = "fa fa-file-archive-o";
    public static final String CLASS_LOCK_STATUS = "fa fa-lock";
    public static final String CLASS_POLICY_RULES_ICON = "fa fa-balance-scale";
    public static final String CLASS_POLICY_RULES_ICON_COLORED = "fa fa-balance-scale"; //TODO

    public static final String CLASS_VALUE_POLICY_ICON = "fa fa-asterisk";
    public static final String CLASS_ARCHIVE = "fa fa-archive";
    public static final String CLASS_BAN = "fa fa-ban";

    public static final String CLASS_SYSTEM_CONFIGURATION_ICON = "fa fa-cog";
    public static final String CLASS_SYSTEM_CONFIGURATION_ICON_COLORED = CLASS_SYSTEM_CONFIGURATION_ICON + " object-shadow-color";

    public static final String CLASS_ASSIGN = "fa fa-link";
    public static final String CLASS_UNASSIGN = "fa fa-unlink";
    public static final String CLASS_RECONCILE = "fa fa-refresh";
    public static final String CLASS_ADD_NEW_OBJECT = "fa fa-plus";
    public static final String CLASS_UPLOAD = "fa fa-upload";
    public static final String CLASS_CREATE_FOCUS = "fa fa-user-plus";
    public static final String CLASS_PLAY = "fa fa-play";
    public static final String CLASS_PAUSE = "fa fa-pause";

    public static final String EVO_CROW_ICON = "fe fe-crown-object";
    public static final String EVO_RIGHT_TOP_ARROW_ICON = "fe fe-arrow";
    public static final String EVO_ASSIGNMENT_ICON = "fe fe-assignment";
    public static final String EVO_ASSIGNMENT_ARROW_ICON = "fe fe-action-assign";
    public static final String EVO_ASSIGNMENT_ICON_A_PIECE = "fe fe-assign_horizontal_A_piece";
    public static final String EVO_ASSIGNMENT_ICON_B_PIECE = "fe fe-assign_horizontal_B_piece";
    public static final String EVO_OFFICER_CAP_ICON = "fe fe-officer-cap-object";
    public static final String EVO_ASSIGNMENT_STRAIGHT_THICKER_ICON = "fe fe-assignment-straight-thicker-object";
    public static final String EVO_ASSIGNMENT_STRAIGHT_ICON = "fe fe-assignment-straight-object";
    public static final String EVO_UNEMPLOYER_ICON = "fe fe-unemployer-icon";
    public static final String EVO_EMPLOYER_ICON = "fe fe-employer-icon";
    public static final String EVO_APPROVER_ICON = "fe fe-approver-object";
    public static final String EVO_MANAGER_ICON = "fe fe-manager-tie-object";
    public static final String EVO_MP_SHORTER_LINES = "fe fe-midpoint-shorter-lines";
    public static final String EVO_MP_WHEEL_ICON = "fe fe-midpoint-wheel";
    public static final String EVO_MP_WITH_LINES_ICON = "fe fe-midpoint-with-lines";
    public static final String EVO_ROLE_HAT_ICON = "fe fe-role-hat";
    public static final String EVO_ROLE_TIE_ICON = "fe fe-role-tie";
    public static final String EVO_ROLE_TOP_HAT_ICON = "fe fe-role-top-hat";
    public static final String EVO_ASSIGNMENT_THICKER_ICON = "fe assignment-thicker";
    public static final String EVO_CASE_OBJECT_ICON = "fe fe-case-object";
    public static final String EVO_ARCHETYPE_TYPE_ICON = "fe fe-archetype_smooth";

    public static final String GREEN_COLOR = "color-green";
    public static final String YELLOW_COLOR = "color-yellow";
    public static final String RED_COLOR = "color-red";
    public static final String BLUE_COLOR = "color-blue";

    public static final String CLASS_ICON_SIGN_OUT = "fa fa-sign-out";
    public static final String CLASS_ICON_CLAIM = "fa fa-paper-plane-o";
    public static final String CLASS_ICON_TEXT = "fa fa-text-width";

    public static final String CLASS_ICON_TRASH = "fa fa-trash-o";
    public static final String CLASS_ICON_PERFORMANCE = "fa fa-area-chart";
    public static final String CLASS_ICON_TASK_RESULTS = "fa fa-list-alt";


}
