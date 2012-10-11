/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public final class WebMiscUtil {

    private static final Trace LOGGER = TraceManager.getTrace(WebMiscUtil.class);
    private static DatatypeFactory df = null;

    static {
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining Datatype Factory instance", dce);
        }
    }

    private WebMiscUtil() {
    }

    public static <T extends Enum> IModel<List<T>> createReadonlyModelFromEnum(final Class<T> type) {
        return new AbstractReadOnlyModel<List<T>>() {

            @Override
            public List<T> getObject() {
                List<T> list = new ArrayList<T>();
                Collections.addAll(list, type.getEnumConstants());

                return list;
            }
        };
    }

    public static String getName(ObjectType object) {
        if (object == null) {
            return null;
        }

        return getName(object.asPrismObject());
    }

    public static String getName(PrismObject object) {
        if (object == null) {
            return null;
        }
        PolyString name = getValue(object, ObjectType.F_NAME, PolyString.class);

        return name != null ? name.getOrig() : null;
    }

    public static PolyStringType createPolyFromOrigString(String str) {
        if (str == null) {
            return null;
        }

        PolyStringType poly = new PolyStringType();
        poly.setOrig(str);

        return poly;
    }

    public static String getOrigStringFromPoly(PolyStringType str) {
        return str != null ? str.getOrig() : null;
    }

    public static <T> T getValue(PrismContainerValue object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        PrismProperty property = object.findProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return null;
        }

        return (T) property.getRealValue(type);
    }

    public static <T> T getValue(PrismContainer object, QName propertyName, Class<T> type) {
        if (object == null) {
            return null;
        }

        return getValue(object.getValue(), propertyName, type);
    }

    public static Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null;
        }
        localeString = localeString.trim();
        if (localeString.toLowerCase().equals("default")) {
            return Locale.getDefault();
        }

        // Extract language
        int languageIndex = localeString.indexOf('_');
        String language = null;
        if (languageIndex == -1) {
            // No further "_" so is "{language}" only
            return new Locale(localeString, "");
        } else {
            language = localeString.substring(0, languageIndex);
        }

        // Extract country
        int countryIndex = localeString.indexOf('_', languageIndex + 1);
        String country = null;
        if (countryIndex == -1) {
            // No further "_" so is "{language}_{country}"
            country = localeString.substring(languageIndex + 1);
            return new Locale(language, country);
        } else {
            // Assume all remaining is the variant so is "{language}_{country}_{variant}"
            country = localeString.substring(languageIndex + 1, countryIndex);
            String variant = localeString.substring(countryIndex + 1);
            return new Locale(language, country, variant);
        }
    }

    public static void encryptCredentials(ObjectDelta delta, boolean encrypt, MidPointApplication app) {
        if (delta == null || delta.isEmpty()) {
            return;
        }

        PropertyDelta propertyDelta = delta.findPropertyDelta(new PropertyPath(
                SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD,
                PasswordType.F_VALUE));
        if (propertyDelta == null) {
            return;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> values = propertyDelta
                .getValues(ProtectedStringType.class);
        for (PrismPropertyValue<ProtectedStringType> value : values) {
            ProtectedStringType string = value.getValue();
            encryptProtectedString(string, encrypt, app);
        }
    }

    public static void encryptCredentials(PrismObject object, boolean encrypt, MidPointApplication app) {
        PrismContainer password = object.findContainer(new PropertyPath(
                SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD));
        if (password == null) {
            return;
        }
        PrismProperty protectedStringProperty = password.findProperty(PasswordType.F_VALUE);
        if (protectedStringProperty == null
                || protectedStringProperty.getRealValue(ProtectedStringType.class) == null) {
            return;
        }

        ProtectedStringType string = (ProtectedStringType) protectedStringProperty
                .getRealValue(ProtectedStringType.class);

        encryptProtectedString(string, encrypt, app);
    }

    private static void encryptProtectedString(ProtectedStringType string, boolean encrypt, MidPointApplication app) {
        if (string == null) {
            return;
        }
        Protector protector = app.getProtector();
        try {
            if (encrypt) {
                if (StringUtils.isEmpty(string.getClearValue())) {
                    return;
                }
                protector.encrypt(string);
            } else {
                if (string.getEncryptedData() == null) {
                    return;
                }
                protector.decrypt(string);
            }
        } catch (EncryptionException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't encrypt protected string", ex);
        }
    }

    public static <T extends Selectable> List<T> getSelectedData(TablePanel panel) {
        DataTable table = panel.getDataTable();
        BaseSortableDataProvider<T> provider = (BaseSortableDataProvider<T>) table.getDataProvider();

        List<T> selected = new ArrayList<T>();
        for (T bean : provider.getAvailableData()) {
            if (bean.isSelected()) {
                selected.add(bean);
            }
        }

        return selected;
    }

    public static Collection<ObjectDelta<? extends ObjectType>> createDeltaCollection(ObjectDelta<? extends ObjectType>... deltas) {
        Collection<ObjectDelta<? extends ObjectType>> collection = new ArrayList<ObjectDelta<? extends ObjectType>>();
        for (ObjectDelta delta : deltas) {
            collection.add(delta);
        }

        return collection;
    }
}
