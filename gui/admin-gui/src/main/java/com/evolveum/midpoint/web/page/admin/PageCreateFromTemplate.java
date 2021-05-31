package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import java.util.Collection;

public class PageCreateFromTemplate extends PageAdmin {

    private Collection<CompiledObjectCollectionView> compiledObjectCollectionViews;

    public PageCreateFromTemplate(Collection<CompiledObjectCollectionView> compiledObjectCollectionViews) {
        this.compiledObjectCollectionViews = compiledObjectCollectionViews;
    }


}
