package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import java.util.List;

public abstract class MultiCompositedButtonPanel extends BasePanel<List<MultiFunctinalButtonDto>> {

    private static final String ID_BUTTON_PANEL = "additionalButton";

    private static final String DEFAULT_BUTTON_STYLE = "btn btn-default btn-sm buttons-panel-marging";

    private List<MultiFunctinalButtonDto> buttonDtos;

    public MultiCompositedButtonPanel(String id, List<MultiFunctinalButtonDto> buttonDtos){
        super(id);
        this.buttonDtos = buttonDtos;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        DisplayType defaultObjectButtonDisplayType = fixDisplayTypeIfNeeded(getDefaultObjectButtonDisplayType());
        DisplayType mainButtonDisplayType = fixDisplayTypeIfNeeded(getMainButtonDisplayType());
        RepeatingView buttonsPanel = new RepeatingView(ID_BUTTON_PANEL);
        add(buttonsPanel);

        if (additionalButtonsExist()){
            buttonDtos.forEach(additionalButtonObject -> {
                DisplayType additionalButtonDisplayType = fixDisplayTypeIfNeeded(additionalButtonObject.getAdditionalButtonDisplayType()); //getAdditionalButtonDisplayType(additionalButtonObject)
                additionalButtonObject.setAdditionalButtonDisplayType(additionalButtonDisplayType);

                AjaxCompositedIconButton additionalButton = new AjaxCompositedIconButton(buttonsPanel.newChildId(), getCompositedIcon(additionalButtonObject),
                        Model.of(WebComponentUtil.getDisplayTypeTitle(additionalButtonDisplayType))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        buttonClickPerformed(target, additionalButtonObject.getAssignmentObjectRelation(), additionalButtonObject.getCollectionView());
                    }
                };
                additionalButton.add(AttributeAppender.append("class", DEFAULT_BUTTON_STYLE));
                buttonsPanel.add(additionalButton);
            });
        }
        //we set main button icon class if no other is defined
        if (StringUtils.isEmpty(defaultObjectButtonDisplayType.getIcon().getCssClass())) {
            defaultObjectButtonDisplayType.getIcon().setCssClass(mainButtonDisplayType.getIcon().getCssClass());
        }

        AjaxCompositedIconButton defaultButton = new AjaxCompositedIconButton(buttonsPanel.newChildId(),
                getAdditionalIconBuilder(defaultObjectButtonDisplayType).build(),
                Model.of(WebComponentUtil.getDisplayTypeTitle(defaultObjectButtonDisplayType))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                buttonClickPerformed(target, null, null);
            }
        };
        defaultButton.add(AttributeAppender.append("class", DEFAULT_BUTTON_STYLE));
        defaultButton.add(new VisibleBehaviour(this::isDefaultButtonVisible));
        buttonsPanel.add(defaultButton);

    }

    private CompositedIcon getCompositedIcon(MultiFunctinalButtonDto additionalButtonObject) {
        CompositedIcon icon = additionalButtonObject.getCompositedIcon();
        if (icon != null) {
            return icon;
        }

        return getAdditionalIconBuilder(additionalButtonObject.getAdditionalButtonDisplayType()).build();
    }

    protected abstract DisplayType getMainButtonDisplayType();

    /**
     * this method should return the display properties for the last button on the dropdown  panel with additional buttons.
     * The last button is supposed to produce a default action (an action with no additional objects to process)
     * @return
     */
    protected abstract DisplayType getDefaultObjectButtonDisplayType();

    protected CompositedIconBuilder getAdditionalIconBuilder(DisplayType additionalButtonDisplayType){
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(WebComponentUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(WebComponentUtil.getIconColor(additionalButtonDisplayType));
        return builder;
    }

    private DisplayType fixDisplayTypeIfNeeded(DisplayType displayType){
        if (displayType == null){
            displayType = new DisplayType();
        }
        if (displayType.getIcon() == null){
            displayType.setIcon(new IconType());
        }
        if (displayType.getIcon().getCssClass() == null){
            displayType.getIcon().setCssClass("");
        }

        return displayType;
    }

    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews){
    }

    protected boolean additionalButtonsExist() {
        return CollectionUtils.isNotEmpty(buttonDtos);
    }

    protected boolean isDefaultButtonVisible(){
        return true;
    }
}
