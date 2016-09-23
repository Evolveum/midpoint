package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created honchar.
 */
public class MultiButtonTable extends BasePanel<List<AssignmentEditorDto>> {
    private static final String ID_ROW = "row";
    private static final String ID_CELL = "cell";
    private static final String ID_BUTTON = "button";

    private int itemsCount = 0;
    private int itemsPerRow = 0;

    public MultiButtonTable (String id){
        super(id);
    }
    public MultiButtonTable (String id, int itemsPerRow, IModel<List<AssignmentEditorDto>> model){
        super(id, model);
        this.itemsPerRow = itemsPerRow;
        initLayout();
    }

    private void initLayout(){
        itemsCount = getModel() != null ? (getModel().getObject() != null ? getModel().getObject().size() : 0) : 0;
        if (itemsCount > 0){
            RepeatingView rows = new RepeatingView(ID_ROW);
            rows.setOutputMarkupId(true);
            add(rows);
            int index = 0;
            List<AssignmentEditorDto> assignmentsList = getModelObject();
            for (int rowNumber = 0; rowNumber <= itemsCount / itemsPerRow; rowNumber++){
                WebMarkupContainer rowContainer = new WebMarkupContainer(rows.newChildId());
                rows.add(rowContainer);
                RepeatingView columns = new RepeatingView(ID_CELL);
                columns.setOutputMarkupId(true);
                rowContainer.add(columns);
                for (int colNumber = 0; colNumber < itemsPerRow; colNumber++){
                    WebMarkupContainer colContainer = new WebMarkupContainer(columns.newChildId());
                    columns.add(colContainer);
                    ObjectReferenceType targetRef = assignmentsList.get(index).getTargetRef();

                    colContainer.add(populateCell(Integer.toString(index)));
                    index++;
                    if (index >= assignmentsList.size()){
                        break;
                    }

                }
            }
        }
    }

    protected Component populateCell(final String caption){
        IModel<String> captionModel = new IModel<String>() {
            @Override
            public String getObject() {
                return caption;
            }

            @Override
            public void setObject(String s) {
            }

            @Override
            public void detach() {
            }
        };
        AjaxButton button = new AjaxButton(ID_BUTTON, captionModel) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                clickPerformed(target);
            }
            @Override
            public boolean isEnabled(){
                return true;
            }
            @Override
            public boolean isVisible(){
                return true;
            }
        };
        button.add(new AttributeAppender("class", getButtonCssClass()));
        return button;
    }



    protected String getButtonCssClass(){
        return "col-sm-3 small-box bg-yellow";
    }

    protected void clickPerformed(AjaxRequestTarget target){
    }

}
