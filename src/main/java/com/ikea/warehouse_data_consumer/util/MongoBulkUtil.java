package com.ikea.warehouse_data_consumer.util;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for handling MongoDB bulk write results.
 */
public final class MongoBulkUtil {

    private MongoBulkUtil() {}

    /**
     * Returns the subset of input items that did not match the update filter in a bulk writing
     * (i.e., neither matched nor were upserted), based on Mongo's upsert indices and matchedCount.
     *
     * This method is generic and can be reused by different services (e.g., InventoryService, ProductService).
     *
     * @param items the original list of items used to build the bulk operations (ordered)
     * @param bulkWriteResult the result of the bulk write
     * @return list of items considered as "not matched" according to the criteria used in services
     */
    public static <T> List<T> getNotMatchedCriteria(List<T> items, BulkWriteResult bulkWriteResult) {
        List<T> notMatched = new ArrayList<>();
        Set<Integer> upsertedIndexList = bulkWriteResult.getUpserts()
                .stream()
                .map(BulkWriteUpsert::getIndex)
                .collect(Collectors.toSet());

        if (ObjectUtils.notEqual(bulkWriteResult.getMatchedCount(), items.size())) {
            for (int i = 0; i < items.size(); i++) {
                if (!upsertedIndexList.contains(i)) {
                    notMatched.add(items.get(i));
                }
            }
        }
        return notMatched;
    }
}
