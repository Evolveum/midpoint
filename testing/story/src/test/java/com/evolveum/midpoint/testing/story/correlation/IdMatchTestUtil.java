/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.correlation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IdMatchTestUtil {

    /**
     * Converts a date from mm/dd/yyyy into yyyy-mm-dd form.
     */
    public static String convertDate(String mdy) {
        if (mdy == null || mdy.isEmpty()) {
            return mdy;
        }

        SimpleDateFormat hrFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        SimpleDateFormat stdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date date;
        try {
            date = hrFormat.parse(mdy);
        } catch (ParseException e) {
            return mdy;
        }
        return stdFormat.format(date);
    }

    /**
     * Normalizes national ID into the form of xxxxxx/xxxx.
     */
    public static String normalizeNationalId(String id) {
        if (id == null) {
            return null;
        }

        String numbers = id.replaceAll("\\D+","");
        if (numbers.length() <= 6) {
            return numbers;
        } else {
            return numbers.substring(0, 6) + "/" + numbers.substring(6);
        }
    }
}
