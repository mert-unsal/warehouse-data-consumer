package com.ikea.warehouse_data_consumer.util;

import lombok.experimental.UtilityClass;

//TODO: If it is not required, remove this object

@UtilityClass
public class ErrorCodeUtil {
    public static final String BASKET_RECOMMENDATION_REQUEST_COMPLETE_PARAMETER_NOT_VALID_EXCEPTION = "1001";
    public static final String BASKET_RECOMMENDATION_REQUEST_HIGH_AND_LOW_PRICE_PARAMETER_NOT_VALID_EXCEPTION = "1002";
    public static final String BASKET_RECOMMENDATION_REQUEST_EMPTY_PRODUCT_IDS_PARAMETER_NOT_VALID_EXCEPTION = "1003";
    public static final String BASE_LINE_VARIANT_INFORMATION_NOT_FOUND_EXCEPTION = "4000";
}
