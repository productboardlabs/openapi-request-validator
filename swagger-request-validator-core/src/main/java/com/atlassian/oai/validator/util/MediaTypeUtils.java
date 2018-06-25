package com.atlassian.oai.validator.util;

import com.google.common.net.MediaType;

public class MediaTypeUtils {
    private static final String HAL_JSON_UTF8_TYPE = "application/hal+json;charset=UTF-8";

    public static final MediaType HAL_JSON_UTF_8 = MediaType.parse(HAL_JSON_UTF8_TYPE);

    private MediaTypeUtils() {

    }
}
