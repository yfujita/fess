/*
 * Copyright 2012-2015 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.app.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.codelibs.core.beans.util.BeanUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.pager.ReqHeaderPager;
import org.codelibs.fess.es.config.cbean.RequestHeaderCB;
import org.codelibs.fess.es.config.exbhv.RequestHeaderBhv;
import org.codelibs.fess.es.config.exentity.RequestHeader;
import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.optional.OptionalEntity;

public class RequestHeaderService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Resource
    protected RequestHeaderBhv requestHeaderBhv;

    public RequestHeaderService() {
        super();
    }

    public List<RequestHeader> getRequestHeaderList(final ReqHeaderPager requestHeaderPager) {

        final PagingResultBean<RequestHeader> requestHeaderList = requestHeaderBhv.selectPage(cb -> {
            cb.paging(requestHeaderPager.getPageSize(), requestHeaderPager.getCurrentPageNumber());
            setupListCondition(cb, requestHeaderPager);
        });

        // update pager
        BeanUtil.copyBeanToBean(requestHeaderList, requestHeaderPager, option -> option.include(Constants.PAGER_CONVERSION_RULE));
        requestHeaderPager.setPageNumberList(requestHeaderList.pageRange(op -> {
            op.rangeSize(5);
        }).createPageNumberList());

        return requestHeaderList;
    }

    public OptionalEntity<RequestHeader> getRequestHeader(final String id) {
        return requestHeaderBhv.selectByPK(id);
    }

    public void store(final RequestHeader requestHeader) {
        setupStoreCondition(requestHeader);

        requestHeaderBhv.insertOrUpdate(requestHeader, op -> {
            op.setRefresh(true);
        });

    }

    public void delete(final RequestHeader requestHeader) {
        setupDeleteCondition(requestHeader);

        requestHeaderBhv.delete(requestHeader, op -> {
            op.setRefresh(true);
        });

    }

    protected void setupListCondition(final RequestHeaderCB cb, final ReqHeaderPager requestHeaderPager) {
        if (requestHeaderPager.id != null) {
            cb.query().docMeta().setId_Equal(requestHeaderPager.id);
        }
        // TODO Long, Integer, String supported only.

        // setup condition
        cb.query().addOrderBy_Name_Asc();

        // search

    }

    protected void setupEntityCondition(final RequestHeaderCB cb, final Map<String, String> keys) {

        // setup condition

    }

    protected void setupStoreCondition(final RequestHeader requestHeader) {

        // setup condition

    }

    protected void setupDeleteCondition(final RequestHeader requestHeader) {

        // setup condition

    }

    public List<RequestHeader> getRequestHeaderList(final String webConfigId) {
        return requestHeaderBhv.selectList(cb -> {
            cb.query().setWebConfigId_Equal(webConfigId);
        });
    }

}
