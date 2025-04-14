package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticConfig;
import com.atguigu.gulimall.search.costant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.atguigu.gulimall.search.costant.EsConstant.PRODUCT_INDEX;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    ProductFeignService productFeignService;

    //去es中查询
    @Override
    public SearchResult search(SearchParam param) {
        //1、动态生成dsl语句
        SearchResult result = null;

        //1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2、执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticConfig.COMMON_OPTIONS);

            //3、分析响应数据封装成需要的格式
            result = buildSearchResult(response,param);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 准备检索请求
     * 模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存），排序，分页，高亮，聚合分析
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();//构建DSL语句

        //查询：模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）
        //1、构建boolquery
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        //1、1、must - 模糊匹配
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        //1、2、bool - filter - 按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        //1、2、bool - filter - 按照品牌id查询
        if (param.getBrandId() != null && !param.getBrandId().isEmpty()) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        //1、2、bool - filter - 按照所有指定的属性进行查询
        if (param.getAttrs() != null && !param.getAttrs().isEmpty()) {
            for (String attr : param.getAttrs()) {
                //attrs=1.5寸：8寸&attrs=2G:4G
                BoolQueryBuilder nestedboolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];//属性id
                String[] attrValues = s[1].split(":");//属性值
                nestedboolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedboolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //每一个都必须生产一个nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedboolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        //1、2、bool - filter - 按照是否有库存查询
        if (param.getHasStock()!= null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }


        //1、2、bool - filter - 按照价格区间查询
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                //区间
                rangeQueryBuilder.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQueryBuilder.lte(s[0]);
                }
                if (param.getSkuPrice().endsWith("_")) {
                    rangeQueryBuilder.gte(s[0]);
                }
            }


            boolQuery.filter(rangeQueryBuilder);
        }

        //把以前的所有条件都拿来进行封装
        sourceBuilder.query(boolQuery);

        //排序，分页，高亮
        //2.1、1排序
        if (StringUtils.isNotEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }
        //2.2分页 pageSize:5
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3、高亮
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        //聚合分析
        //1、品牌聚合分析
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        //1、1、品牌子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        //TODO 1、聚合品牌信息
        sourceBuilder.aggregation(brand_agg);

        //2、分类聚合分析
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        //2、1、分类子聚合
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        //TODO 2、聚合分类信息
        sourceBuilder.aggregation(catalog_agg);

        //3、属性聚合分析
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合出当前所有attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");

        //聚合分析出当前attr_id对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析出当前attr_id对应的所有可能的属性值
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        //TODO 3、聚合attr属性信息
        attr_agg.subAggregation(attr_id_agg);

        sourceBuilder.aggregation(attr_agg);


        String s = sourceBuilder.toString();
        System.out.println("构建的DSL语句：" + s);

        SearchRequest searchRequest = new SearchRequest(new String[]{PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 构建结果数据
     */
    private SearchResult buildSearchResult(SearchResponse response,SearchParam param) {
        SearchResult result = new SearchResult();
        //1、返回所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits != null && hits.getHits().length > 0){
            for (SearchHit hit : hits){
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel= JSON.parseObject(sourceAsString, SkuEsModel.class);
                HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    esModel.setSkuTitle(skuTitle.getFragments()[0].string());
                }
                esModels.add(esModel);
            }
            result.setProducts(esModels);
        }

        //2、当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //1、得到属性id
            long attrId=bucket.getKeyAsNumber().longValue();
            //2、得到属性名
            String attrName=((ParsedStringTerms)bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //3、得到属性值
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = ((Terms.Bucket) item).getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);


            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
        //3、当前所有商品所在的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()){
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //1、得到品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            //2、得到品牌的名字
            String name_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();

            //3、得到品牌图片
            String img_agg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(name_agg);
            brandVo.setBrandImg(img_agg);

            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);
        //4、当前所有商品所在的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets){
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            //得到分类名
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catelog_name = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catelog_name);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

//        =======以上从聚合信息中获取========

//        //5、分页信息-页码
        result.setPageNum(param.getPageNum());
//        //6、分页信息-总记录数
        assert hits.getTotalHits() != null;
        long total = hits.getTotalHits().value;
        result.setTotal(total);
//        //7、分页信息-总页码
        int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int) total / EsConstant.PRODUCT_PAGESIZE : ((int) total / EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages(totalPages);

        ArrayList<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++){
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 8、构建面包屑导航
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> navVoList = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();

                // 解析属性参数 (格式: attrId_value1:value2)
                String[] s = attr.split("_");
                if(s.length < 2) return navVo;  // 跳过格式错误的参数

                Long attrId = Long.parseLong(s[0]);
                String[] attrValues = s[1].split(":");

                // 远程获取属性名
                R<AttrResponseVo> r = productFeignService.attrInfo(attrId);
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {});
                    navVo.setNavName(data.getAttrName());
                    // 设置显示值 (格式: 属性名: 值1,值2)
                    navVo.setNavValue(String.join(",", attrValues));
                } else {
                    // Feign调用失败时的备选方案
                    navVo.setNavName("属性" + attrId);
                    navVo.setNavValue(String.join(",", attrValues));
                }

                // 生成移除当前属性后的链接
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                result.getAttrIds().add(attrId);
                return navVo;
            }).collect(Collectors.toList());

            result.setNavs(navVoList);
        }
        //品牌
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            List<SearchResult.NavVo> navs=result.getNavs();
            SearchResult.NavVo navVo=new SearchResult.NavVo();

            navVo.setNavName("品牌");
            //远程查询所有品牌
            R<List<BrandVo>> r = productFeignService.brandsInfo(param.getBrandId());

            if (r.getCode() == 0){
                List<BrandVo> brands = r.getData("brands", new TypeReference<List<BrandVo>>() {
                });
                StringBuilder buffer = new StringBuilder();
                String replace="";
                for (BrandVo brandVo : brands) {
                    buffer.append(brandVo.getBrandName()+";");
                    replace=replaceQueryString(param, brandVo.getBrandId()+"", "brandId");

                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            }
            navs.add(navVo);
        }
        //TODO 分类



        return result;
    }

    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null ;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode= encode.replace("+", "%20");//浏览器对空格编码和Java不一样
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return param.get_queryString().replace("&"+ key + "=" + encode, "");

    }
}
