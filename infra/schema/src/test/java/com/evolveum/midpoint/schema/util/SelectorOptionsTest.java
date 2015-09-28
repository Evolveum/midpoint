/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * @author lazyman
 */
public class SelectorOptionsTest {

    @Test
    public void testFocusPhoto() throws Exception {
        AssertJUnit.assertFalse(SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, null));

        Collection o = SelectorOptions.createCollection(FocusType.F_JPEG_PHOTO,
                GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT));
        AssertJUnit.assertFalse(SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, o));

        o = SelectorOptions.createCollection(FocusType.F_JPEG_PHOTO,
                GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
        AssertJUnit.assertFalse(SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, o));

        o = SelectorOptions.createCollection(FocusType.F_JPEG_PHOTO,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
        AssertJUnit.assertTrue(SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, o));
    }
}
