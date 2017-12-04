/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * TODO: update to better use with DropdownButtonPanel. Move away from depreated com.evolveum.midpoint.web.component.menu.cog.
 * TODO: Create a builder for this.
 * 
 * @author lazyman
 */
public class InlineMenuItem implements Serializable {

    private IModel<String> label;
    private IModel<Boolean> enabled;
    private IModel<Boolean> visible;
    private InlineMenuItemAction action;
    private boolean submit;
    private int id = -1;
    private String buttonIconCssClass;
    private String buttonColorCssClass;
    private IModel<String> confirmationMessageModel = null;
    private boolean showConfirmationDialog = false;

    public static enum FOCUS_LIST_INLINE_MENU_ITEM_ID {
        ENABLE(0), DISABLE(1), RECONCILE(2),
        UNLOCK(3), DELETE(4), MERGE(5),
        HEADER_ENABLE(0), HEADER_RECONCILE(1),
        HEADER_DISABLE(2);

        private int menuItemId = -1;

        FOCUS_LIST_INLINE_MENU_ITEM_ID(final int id){menuItemId = id;}

        public int getMenuItemId(){
            return menuItemId;
        }
        public String toString(){return Integer.toString(menuItemId);}
    }
    public static enum RESOURCE_INLINE_MENU_ITEM_ID {
        TEST_CONNECTION(0),
        HEADER_TEST_CONNECTION(1),
        EDIT_XML(1), HEADER_DELETE(0), DELETE_RESOURCE(2),
        DELETE_SYNC_TOKEN(3), EDIT_USING_WIZARD(4);

        private int menuItemId = -1;

        RESOURCE_INLINE_MENU_ITEM_ID(final int id){menuItemId = id;}

        public int getMenuItemId(){
            return menuItemId;
        }
        public String toString(){return Integer.toString(menuItemId);}
    }
    public static enum TASKS_INLINE_MENU_ITEM_ID {
        SUSPEND(0), RESUME(1), RUN_NOW(2), DELETE(3), DELETE_CLOSED(4),
        NODE_STOP_SCHEDULER(1), NODE_STOP_SCHEDULER_TASK(2), NODE_START(0), NODE_DELETE(3);

        private int menuItemId = -1;

        TASKS_INLINE_MENU_ITEM_ID(final int id){menuItemId = id;}

        public int getMenuItemId(){
            return menuItemId;
        }
        public String toString(){return Integer.toString(menuItemId);}
    }

    public InlineMenuItem() {
        this(null, null);
    }

    public InlineMenuItem(IModel<String> label) {
        this(label, null);
    }

    public InlineMenuItem(IModel<String> label, InlineMenuItemAction action) {
        this(label, false, action);
    }

    public InlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action) {
        this(label, null, null, submit, action, -1, "", "");
    }

    public InlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action, int id) {
        this(label, null, null, submit, action, id, "", DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString());
    }

    public InlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action, int id,
                          String buttonIconCssClass) {
        this(label, null, null, submit, action, id, buttonIconCssClass, DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString());
    }

    public InlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action, int id,
                          String buttonIconCssClass, String buttonColorCssClass) {
        this(label, null, null, submit, action, id, buttonIconCssClass, buttonColorCssClass);
    }

    public InlineMenuItem(IModel<String> label, IModel<Boolean> enabled, IModel<Boolean> visible,
                          InlineMenuItemAction action) {
        this(label, enabled, visible, false, action, -1, "", "");
    }

    public InlineMenuItem(IModel<String> label, IModel<Boolean> enabled, IModel<Boolean> visible, boolean submit,
                          InlineMenuItemAction action) {
        this(label, enabled, visible, submit, action, -1, "", "");
    }

    public InlineMenuItem(IModel<String> label, IModel<Boolean> enabled, IModel<Boolean> visible, boolean submit,
                          InlineMenuItemAction action, int id, String buttonIconCssClass, String buttonColorCssClass) {
        this.label = label;
        this.enabled = enabled;
        this.visible = visible;
        this.action = action;
        this.submit = submit;
        this.id = id;
        this.buttonIconCssClass = buttonIconCssClass;
        this.buttonColorCssClass = buttonColorCssClass;
    }

    public InlineMenuItemAction getAction() {
        return action;
    }

    public IModel<Boolean> getEnabled() {
        return enabled;
    }

    public IModel<String> getLabel() {
        return label;
    }

    /**
     * if true, link must be rendered as submit link button, otherwise normal ajax link
     */
    public boolean isSubmit() {
        return submit;
    }

    public IModel<Boolean> getVisible() {
        return visible;
    }

    public void setVisible(IModel<Boolean> visible) {
        this.visible = visible;
    }

    public boolean isDivider() {
        return label == null && action == null;
    }

    public boolean isMenuHeader() {
        return label != null && action == null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getButtonIconCssClass() {
        return buttonIconCssClass;
    }

    public void setButtonIconCssClass(String buttonIconCssClass) {
        this.buttonIconCssClass = buttonIconCssClass;
    }

    public String getButtonColorCssClass() {
        return buttonColorCssClass;
    }

    public void setButtonColorCssClass(String buttonColorCssClass) {
        this.buttonColorCssClass = buttonColorCssClass;
    }

    public IModel<String> getConfirmationMessageModel() {
        return confirmationMessageModel;
    }

    public void setConfirmationMessageModel(IModel<String> confirmationMessageModel) {
        this.confirmationMessageModel = confirmationMessageModel;
    }

    public boolean isShowConfirmationDialog() {
        return showConfirmationDialog;
    }

    public void setShowConfirmationDialog(boolean showConfirmationDialog) {
        this.showConfirmationDialog = showConfirmationDialog;
    }
}
