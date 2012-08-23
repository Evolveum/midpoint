/**
 * 
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.TreeTable;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.LinkIconPanel;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * @author mserbak
 * 
 */
public class OrgStructPanel extends Panel {
	private TreeTable treeTable;

	public OrgStructPanel(String id, final IModel<String> model) {
		super(id);
		add(new Label("body", model));
		initLayout();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		response.renderCSSReference(new PackageResourceReference(OrgStructPanel.class, "OrgStructPanel.css"));
		response.renderJavaScriptReference(new PackageResourceReference(OrgStructPanel.class, "OrgStructPanel.js"));
		response.renderOnLoadJavaScript("initOrgStruct()");
	}

	private void initLayout() {
		IColumn columns[] = new IColumn[] { new PropertyTreeColumn<String>(new ColumnLocation(
				Alignment.MIDDLE, 8, Unit.PROPORTIONAL), "", "") {
		} };

		treeTable = new TreeTable("treeTable", createTreeModel(), columns) {

/*			@Override
			protected ResourceReference getNodeIcon(TreeNode node) {
				NodeModel nodeModel = (NodeModel) node;
//				switch (nodeModel.getType()) {
//					case FOLDER:
//						
//						break;
//					case BOSS:
//
//						break;
//					case MANAGER:
//						
//						break;
//						
//					default:
//						break;
//				}
				return super.getNodeIcon(node);
			}*/

		};
		treeTable.getTreeState().setAllowSelectMultiple(true);
		treeTable.getTreeState().collapseAll();
		add(treeTable);
	}

	private TreeModel createTreeModel() {
		List<Object> group0 = new ArrayList<Object>();
		
		List<Object> group2 = new ArrayList<Object>();
		group0.add(group2);
		group2.add("Josua L. Carlton (joslcarl)");
		group2.add("Josua L. Carlton (joslcarl)");
		group2.add("Josua L. Carlton (joslcarl)");
		
		
		List<Object> group3 = new ArrayList<Object>();
		group0.add(group3);
		group3.add("Josua L. Carlton (joslcarl)");
		group3.add("Josua L. Carlton (joslcarl)");
		group3.add("Josua L. Carlton (joslcarl)");

		List<Object> group4 = new ArrayList<Object>();
		group3.add(group4);
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		group4.add("Josua L. Carlton (joslcarl)");
		
		List<Object> group5 = new ArrayList<Object>();
		group3.add(group5);
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		group5.add("Josua L. Carlton (joslcarl)");
		
		List<Object> group6 = new ArrayList<Object>();
		group0.add(group6);
		group6.add("Josua L. Carlton (joslcarl)");
		group6.add("Josua L. Carlton (joslcarl)");
		group6.add("Josua L. Carlton (joslcarl)");

		return convertToTreeModel(group0);
	}

	private TreeModel convertToTreeModel(List<Object> list) {
		TreeModel model = null;
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("The good directory");
		add(rootNode, list);
		model = new DefaultTreeModel(rootNode);
		return model;
	}

	private void add(DefaultMutableTreeNode parent, List<Object> sub) {
		for (Object obj : sub) {
			if (obj instanceof List) {
				//DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NodeDto(NodeType.FOLDER, "Subfolder"));
				DefaultMutableTreeNode child = new DefaultMutableTreeNode("Subfolder");
				parent.add(child);
				add(child, (List<Object>) obj);
			} else {
				//DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NodeDto(NodeType.USER, obj.toString()));
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(obj);
				parent.add(child);
			}
		}
	}
}
