package com.zilliz.milvustestv2.vectorOperation;

import com.google.common.collect.Lists;
import com.zilliz.milvustestv2.common.BaseTest;
import com.zilliz.milvustestv2.common.CommonData;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.service.collection.request.GetCollectionStatsReq;
import io.milvus.v2.service.collection.response.GetCollectionStatsResp;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @Author yongpeng.li
 * @Date 2024/2/19 17:03
 */
public class QueryTest extends BaseTest {
    @DataProvider(name = "filterAndExcept")
    public Object[][] providerData() {
        return new Object[][]{
                {CommonData.fieldInt64 + " < 10 ", 10},
                {CommonData.fieldInt64 + " != 10 ", CommonData.numberEntities*3 - 1},
                {CommonData.fieldInt64 + " <= 10 ", 11},
                {"5<" + CommonData.fieldInt64 + " <= 10 ", 5},
                {CommonData.fieldInt64 + " >= 10 ", CommonData.numberEntities*3 - 10},
                {CommonData.fieldInt64 + " > 100 ", CommonData.numberEntities*3 - 101},
                {CommonData.fieldInt64 + " < 10 and " + CommonData.fieldBool + " == true", 5},
                {CommonData.fieldInt64 + " in [1,2,3] ", 3},
                {CommonData.fieldInt64 + " not in [1,2,3] ", CommonData.numberEntities*3 - 3},
                {CommonData.fieldInt64 + " < 10 and " + CommonData.fieldInt32 + " >5 ", 4},
                {CommonData.fieldVarchar + " > \"0\" ", CommonData.numberEntities*3},
                {CommonData.fieldVarchar + " like \"str%\" ", 0},
                {CommonData.fieldVarchar + " like \"Str%\" ", CommonData.numberEntities*3},
                {CommonData.fieldVarchar + " like \"Str1\" ", 1},
                {CommonData.fieldInt8 + " > 129 ", 0},
        };
    }

    @DataProvider(name = "queryPartition")
    private Object[][] providePartitionQueryParams() {
        return new Object[][]{
                {Lists.newArrayList(CommonData.partitionNameA), "0 <= " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities, CommonData.numberEntities},
                {Lists.newArrayList(CommonData.partitionNameA), CommonData.numberEntities + " < " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 2, 0},
                {Lists.newArrayList(CommonData.partitionNameB), CommonData.numberEntities + " <= " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 2, CommonData.numberEntities},
                {Lists.newArrayList(CommonData.partitionNameB), CommonData.numberEntities * 2 + " < " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 3, 0},
                {Lists.newArrayList(CommonData.partitionNameC), CommonData.numberEntities * 2 + " <= " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 3, CommonData.numberEntities},
                {Lists.newArrayList(CommonData.partitionNameC), CommonData.numberEntities * 3 + " < " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 4, 0},
                {Lists.newArrayList(CommonData.partitionNameA,CommonData.partitionNameB), "0 <= " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 2, CommonData.numberEntities * 2},
                {Lists.newArrayList(CommonData.partitionNameA,CommonData.partitionNameB,CommonData.partitionNameC),"0 <= " + CommonData.fieldInt64 + " <= " + CommonData.numberEntities * 3, CommonData.numberEntities * 3},
        };
    }

    @Test(description = "query", groups = {"Smoke"}, dataProvider = "filterAndExcept")
    public void query(String filter, long expect) {
        QueryResp query = milvusClientV2.query(QueryReq.builder()
                .collectionName(CommonData.defaultFloatVectorCollection)
                .filter(filter)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .outputFields(Lists.newArrayList("*"))
                .build());
        Assert.assertEquals(query.getQueryResults().size(), expect);
    }

    @Test(description = "queryByIds", groups = {"Smoke"})
    public void queryByIds() {
        QueryResp query = milvusClientV2.query(QueryReq.builder()
                .collectionName(CommonData.defaultFloatVectorCollection)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .outputFields(Lists.newArrayList("*"))
                .ids(Lists.newArrayList(1, 2, 3, 4))
                .filter(" fieldInt64 in [10] ")
                .build());
        for (QueryResp.QueryResult queryResult : query.getQueryResults()) {
            System.out.println("查询结果fieldInt64：" + queryResult.getFields().get("fieldInt64"));

        }
        Assert.assertEquals(query.getQueryResults().size(), 1);
    }

    @Test(description = "queryByAlias", groups = {"Smoke"})
    public void queryByAlias() {
        QueryResp query = milvusClientV2.query(QueryReq.builder()
                .collectionName(CommonData.alias)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .outputFields(Lists.newArrayList("*"))
                .ids(Lists.newArrayList(1, 2, 3, 4))
                .filter(" fieldInt64 in [10] ")
                .build());
        Assert.assertEquals(query.getQueryResults().size(), 1);
    }

    @Test(description = "queryInPartition", groups = {"Smoke"}, dataProvider = "queryPartition")
    public void queryInPartition(List<String> partition, String filter, long expect) {
        QueryResp query = milvusClientV2.query(QueryReq.builder()
                .collectionName(CommonData.defaultFloatVectorCollection)
                .filter(filter)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .outputFields(Lists.newArrayList("*"))
                .partitionNames(partition)
                .build());
        Assert.assertEquals(query.getQueryResults().size(), expect);
    }
}
