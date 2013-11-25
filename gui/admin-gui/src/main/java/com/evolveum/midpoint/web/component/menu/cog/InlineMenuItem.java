package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class InlineMenuItem implements Serializable {

    private IModel<String> label;
    private IModel<Boolean> enabled;
    private IModel<Boolean> visible;
    private InlineMenuItemAction action;
    private boolean submit;

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
        this(label, null, null, submit, action);
    }

    public InlineMenuItem(IModel<String> label, IModel<Boolean> enabled, IModel<Boolean> visible,
                          InlineMenuItemAction action) {
        this(label, enabled, visible, false, action);
    }

    public InlineMenuItem(IModel<String> label, IModel<Boolean> enabled, IModel<Boolean> visible, boolean submit,
                          InlineMenuItemAction action) {
        this.label = label;
        this.enabled = enabled;
        this.visible = visible;
        this.action = action;
        this.submit = submit;
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

    public boolean isDivider() {
        return label == null && action == null;
    }

    public boolean isMenuHeader() {
        return label != null && action == null;
    }
}
