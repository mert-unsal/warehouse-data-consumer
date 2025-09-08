package com.ikea.warehouse_data_consumer.util;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoBulkUtilTest {

    @Test
    void getNotMatchedCriteria_shouldReturnEmptyWhenAllMatched() {
        List<String> items = List.of("a", "b");
        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getMatchedCount()).thenReturn(items.size());
        when(result.getUpserts()).thenReturn(List.of());

        List<String> notMatched = MongoBulkUtil.getNotMatchedCriteria(items, result);
        assertTrue(notMatched.isEmpty());
    }

    @Test
    void getNotMatchedCriteria_shouldReturnNonUpsertedWhenMatchedLessThanSize() {
        List<String> items = List.of("a", "b", "c");
        BulkWriteResult result = mock(BulkWriteResult.class);
        when(result.getMatchedCount()).thenReturn(1);
        when(result.getUpserts()).thenReturn(List.of(new BulkWriteUpsert(0, null)));

        List<String> notMatched = MongoBulkUtil.getNotMatchedCriteria(items, result);
        // indices 1 and 2 should be not matched
        assertEquals(List.of("b", "c"), notMatched);
    }
}
