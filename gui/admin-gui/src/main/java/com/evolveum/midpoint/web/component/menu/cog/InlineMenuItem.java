/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * TODO: update to better use with DropdownButtonPanel. Move away from depreated com.evolveum.midpoint.web.component.menu.cog.
 * TODO: Create a builder for this.
 *
 * @author lazyman
 */
public abstract class InlineMenuItem implements Serializable {

    private IModel<String> label;
    private IModel<Boolean> enabled = Model.of(true);
    private IModel<Boolean> visible = Model.of(true);
    private boolean submit = false;
    private InlineMenuItemAction action;
    private int id = -1;

    private VisibilityChecker visibilityChecker;

    public InlineMenuItem(IModel<String> label) {
        this.label = label;
        action = initAction();
    }

    public InlineMenuItem(IModel<String> label, boolean isSubmit) {
        this.submit = isSubmit;
        this.label = label;
        action = initAction();
    }

    public abstract InlineMenuItemAction initAction();

    public IModel<Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(IModel<Boolean> enabled) {
        this.enabled = enabled;
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
        return false;
        //TODO fix after menu items refactoring
//        return label == null && action == null;
    }

    public boolean isMenuHeader() {
//        return true;
//        TODO fix after menu items refactoring
        return label != null && action == null;
    }

    /**
     * visible behavior for menu item in the header
     */
    public boolean isHeaderMenuItem(){
        return true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public InlineMenuItemAction getAction() {
        return action;
    }

    public void setAction(InlineMenuItemAction action) {
        this.action = action;
    }

    public IModel<String> getConfirmationMessageModel() {
        return null;
    }

   public boolean showConfirmationDialog() {
        return true;
    }

    public VisibilityChecker getVisibilityChecker() {
        return visibilityChecker;
    }

    public void setVisibilityChecker(VisibilityChecker visibilityChecker) {
        this.visibilityChecker = visibilityChecker;
    }

    @FunctionalInterface
    public interface VisibilityChecker extends Serializable {
        boolean isVisible(IModel<?> rowModel, boolean isHeader);
    }
}
