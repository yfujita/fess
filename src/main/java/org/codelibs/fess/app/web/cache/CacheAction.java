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
package org.codelibs.fess.app.web.cache;

import java.util.Map;

import org.codelibs.fess.Constants;
import org.codelibs.fess.app.web.base.FessSearchAction;
import org.codelibs.fess.util.DocumentUtil;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.lastaflute.web.Execute;
import org.lastaflute.web.response.StreamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheAction extends FessSearchAction {

    // ===================================================================================
    //                                                                            Constant
    //
    private static final Logger logger = LoggerFactory.getLogger(CacheAction.class);

    // ===================================================================================
    //                                                                           Attribute
    //

    // ===================================================================================
    //                                                                               Hook
    //                                                                              ======

    // ===================================================================================
    //                                                                      Search Execute
    //                                                                      ==============
    @Execute
    public StreamResponse index(final CacheForm form) {
        searchAvailable();

        Map<String, Object> doc = null;
        try {
            doc = fessEsClient.getDocument(fessConfig.getIndexDocumentIndex(), fessConfig.getIndexDocumentType(), queryRequestBuilder -> {
                final TermQueryBuilder termQuery = QueryBuilders.termQuery(fessConfig.getIndexFieldDocId(), form.docId);
                queryRequestBuilder.setQuery(termQuery);
                queryRequestBuilder.addFields(queryHelper.getCacheResponseFields());
                return true;
            }).get();
        } catch (final Exception e) {
            logger.warn("Failed to request: " + form.docId, e);
        }
        if (doc == null) {
            throwValidationError(messages -> {
                messages.addErrorsDocidNotFound(GLOBAL, form.docId);
            }, () -> asHtml(path_ErrorJsp));
        }

        final String content = viewHelper.createCacheContent(doc, form.hq);
        if (content == null) {
            throwValidationError(messages -> {
                messages.addErrorsDocidNotFound(GLOBAL, form.docId);
            }, () -> asHtml(path_ErrorJsp));
        }

        return asStream(DocumentUtil.getValue(doc, fessConfig.getIndexFieldDocId(), String.class)).contentType("text/html; charset=UTF-8")
                .data(content.getBytes(Constants.CHARSET_UTF_8));
    }

}