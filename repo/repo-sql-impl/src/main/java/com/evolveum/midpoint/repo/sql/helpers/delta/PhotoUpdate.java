/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import java.util.Set;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RFocus;
import com.evolveum.midpoint.repo.sql.data.common.RFocusPhoto;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Handles jpegPhoto updates.
 */
class PhotoUpdate extends BaseUpdate {

    PhotoUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx);
    }

    public void handlePropertyDelta() throws SchemaException {
        if (!(object instanceof RFocus focus)) {
            throw new SystemException("Bean is not instance of " + RFocus.class + ", shouldn't happen");
        }

        Set<RFocusPhoto> photos = focus.getJpegPhoto();

        if (isDelete()) {
            photos.clear();
            return;
        }

        PrismValue value = getSingleValue();

        MapperContext context = new MapperContext();

        context.setRepositoryContext(beans.createRepositoryContext());
        context.setDelta(delta);
        context.setOwner(object);

        RFocusPhoto photo = beans.prismEntityMapper.map(value.getRealValue(), RFocusPhoto.class, context);

        if (delta.isAdd()) {
            if (!photos.isEmpty()) {
                throw new SchemaException("Object '" + focus.getOid() + "' already contains photo");
            }

            photo.setTransient(true);
            photos.add(photo);
            return;
        }

        if (photos.isEmpty()) {
            photo.setTransient(true);
            photos.add(photo);
            return;
        }

        RFocusPhoto oldPhoto = photos.iterator().next();
        oldPhoto.setPhoto(photo.getPhoto());
    }
}
