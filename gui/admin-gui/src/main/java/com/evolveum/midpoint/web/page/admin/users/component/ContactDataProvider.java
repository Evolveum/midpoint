package com.evolveum.midpoint.web.page.admin.users.component;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;

import java.util.Iterator;

/**
 * @author lazyman
 */
public class ContactDataProvider implements ITreeProvider {

    @Override
    public Iterator getRoots() {
        return null;
    }

    @Override
    public boolean hasChildren(Object node) {
        return false;
    }

    @Override
    public Iterator getChildren(Object node) {
        return null;
    }

    @Override
    public IModel model(Object object) {
        return null;
    }

    @Override
    public void detach() {
    }
}
