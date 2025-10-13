/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.RFocus;
import com.evolveum.midpoint.repo.sql.data.common.RFocusPhoto;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RFocusPhotoMapper implements Mapper<byte[], RFocusPhoto> {

    @Override
    public RFocusPhoto map(byte[] input, MapperContext context) {
        RFocusPhoto photo = new RFocusPhoto();
        photo.setOwner((RFocus) context.getOwner());
        photo.setPhoto(input);

        return photo;
    }
}
