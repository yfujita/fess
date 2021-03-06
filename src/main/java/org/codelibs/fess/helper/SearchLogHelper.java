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
package org.codelibs.fess.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.codelibs.core.collection.LruHashMap;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.core.misc.DynamicProperties;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.SearchService;
import org.codelibs.fess.es.log.exbhv.ClickLogBhv;
import org.codelibs.fess.es.log.exbhv.FavoriteLogBhv;
import org.codelibs.fess.es.log.exbhv.SearchFieldLogBhv;
import org.codelibs.fess.es.log.exbhv.SearchLogBhv;
import org.codelibs.fess.es.log.exbhv.UserInfoBhv;
import org.codelibs.fess.es.log.exentity.ClickLog;
import org.codelibs.fess.es.log.exentity.SearchFieldLog;
import org.codelibs.fess.es.log.exentity.SearchLog;
import org.codelibs.fess.es.log.exentity.UserInfo;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.DocumentUtil;
import org.codelibs.fess.util.StreamUtil;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.script.Script;
import org.lastaflute.di.core.SingletonLaContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchLogHelper {
    private static final Logger logger = LoggerFactory.getLogger(SearchLogHelper.class);

    @Resource
    protected DynamicProperties crawlerProperties;

    public long userCheckInterval = 5 * 60 * 1000;// 5 min

    public int userInfoCacheSize = 1000;

    protected volatile Queue<SearchLog> searchLogQueue = new ConcurrentLinkedQueue<SearchLog>();

    protected volatile Queue<ClickLog> clickLogQueue = new ConcurrentLinkedQueue<ClickLog>();

    protected Map<String, Long> userInfoCache;

    @PostConstruct
    public void init() {
        userInfoCache = new LruHashMap<String, Long>(userInfoCacheSize);
    }

    public void addSearchLog(final SearchLog searchLog) {
        searchLogQueue.add(searchLog);
    }

    public void addClickLog(final ClickLog clickLog) {
        clickLogQueue.add(clickLog);
    }

    public void storeSearchLog() {
        if (!searchLogQueue.isEmpty()) {
            final Queue<SearchLog> queue = searchLogQueue;
            searchLogQueue = new ConcurrentLinkedQueue<SearchLog>();
            processSearchLogQueue(queue);
        }

        if (!clickLogQueue.isEmpty()) {
            final Queue<ClickLog> queue = clickLogQueue;
            clickLogQueue = new ConcurrentLinkedQueue<ClickLog>();
            processClickLogQueue(queue);
        }
    }

    public int getClickCount(final String url) {
        final ClickLogBhv clickLogBhv = ComponentUtil.getComponent(ClickLogBhv.class);
        return clickLogBhv.selectCount(cb -> {
            cb.query().setUrl_Equal(url);
        });
    }

    public long getFavoriteCount(final String url) {
        final FavoriteLogBhv favoriteLogBhv = ComponentUtil.getComponent(FavoriteLogBhv.class);
        return favoriteLogBhv.selectCount(cb -> {
            cb.query().setUrl_Equal(url);
        });
    }

    public void updateUserInfo(final String userCode) {
        final long current = System.currentTimeMillis();
        final Long time = userInfoCache.get(userCode);
        if (time == null || current - time.longValue() > userCheckInterval) {

            final UserInfoBhv userInfoBhv = ComponentUtil.getComponent(UserInfoBhv.class);

            final LocalDateTime now = ComponentUtil.getSystemHelper().getCurrentTimeAsLocalDateTime();
            userInfoBhv.selectByPK(userCode).ifPresent(userInfo -> {
                userInfo.setUpdatedAt(now);
                new Thread(() -> {
                    userInfoBhv.insertOrUpdate(userInfo);
                }).start();
            }).orElse(() -> {
                final UserInfo userInfo = new UserInfo();
                userInfo.setId(userCode);
                userInfo.setCreatedAt(now);
                userInfo.setUpdatedAt(now);
                userInfoBhv.insert(userInfo);
            });
            userInfoCache.put(userCode, current);
        }
    }

    protected void processSearchLogQueue(final Queue<SearchLog> queue) {
        final String value = crawlerProperties.getProperty(Constants.PURGE_BY_BOTS_PROPERTY, StringUtil.EMPTY);
        String[] botNames;
        if (StringUtil.isBlank(value)) {
            botNames = StringUtil.EMPTY_STRINGS;
        } else {
            botNames = value.split(",");
        }

        final List<SearchLog> searchLogList = new ArrayList<>();
        final Map<String, UserInfo> userInfoMap = new HashMap<>();
        queue.stream().forEach(searchLog -> {
            final String userAgent = searchLog.getUserAgent();
            final boolean isBot = userAgent != null && StreamUtil.of(botNames).anyMatch(botName -> userAgent.indexOf(botName) >= 0);
            if (!isBot) {
                searchLog.getUserInfo().ifPresent(userInfo -> {
                    final String code = userInfo.getId();
                    final UserInfo oldUserInfo = userInfoMap.get(code);
                    if (oldUserInfo != null) {
                        userInfo.setCreatedAt(oldUserInfo.getCreatedAt());
                    }
                    userInfoMap.put(code, userInfo);
                });
                searchLogList.add(searchLog);
            }
        });

        if (!userInfoMap.isEmpty()) {
            final List<UserInfo> insertList = new ArrayList<>(userInfoMap.values());
            final List<UserInfo> updateList = new ArrayList<>();
            final UserInfoBhv userInfoBhv = SingletonLaContainer.getComponent(UserInfoBhv.class);
            userInfoBhv.selectList(cb -> {
                cb.query().setId_InScope(userInfoMap.keySet());
            }).forEach(userInfo -> {
                final String code = userInfo.getId();
                final UserInfo entity = userInfoMap.get(code);
                entity.setId(userInfo.getId());
                entity.setCreatedAt(userInfo.getCreatedAt());
                updateList.add(entity);
                insertList.remove(entity);
            });
            userInfoBhv.batchInsert(insertList);
            userInfoBhv.batchUpdate(updateList);
            searchLogList.stream().forEach(searchLog -> {
                searchLog.getUserInfo().ifPresent(userInfo -> {
                    searchLog.setUserInfoId(userInfo.getId());
                });
            });
        }

        if (!searchLogList.isEmpty()) {
            storeSearchLogList(searchLogList);
            final SuggestHelper suggestHelper = ComponentUtil.getSuggestHelper();
            suggestHelper.indexFromSearchLog(searchLogList);
        }
    }

    private void storeSearchLogList(final List<SearchLog> searchLogList) {
        final SearchLogBhv searchLogBhv = ComponentUtil.getComponent(SearchLogBhv.class);
        final SearchFieldLogBhv searchFieldLogBhv = ComponentUtil.getComponent(SearchFieldLogBhv.class);
        searchLogBhv.batchUpdate(searchLogList, op -> {
            op.setRefresh(true);
        });
        searchLogList.stream().forEach(searchLog -> {
            final List<SearchFieldLog> fieldLogList = new ArrayList<>();
            searchLog.getSearchFieldLogList().stream().forEach(fieldLog -> {
                fieldLog.setSearchLogId(searchLog.getId());
                fieldLogList.add(fieldLog);
            });
            searchFieldLogBhv.batchInsert(fieldLogList);
        });
    }

    protected void processClickLogQueue(final Queue<ClickLog> queue) {
        final Map<String, Integer> clickCountMap = new HashMap<>();
        final List<ClickLog> clickLogList = new ArrayList<>();
        for (final ClickLog clickLog : queue) {
            try {
                final SearchLogBhv searchLogBhv = SingletonLaContainer.getComponent(SearchLogBhv.class);
                searchLogBhv.selectEntity(cb -> {
                    cb.query().setQueryId_Equal(clickLog.getQueryId());
                }).ifPresent(entity -> {
                    clickLogList.add(clickLog);
                    final String docId = clickLog.getDocId();
                    Integer countObj = clickCountMap.get(docId);
                    if (countObj == null) {
                        countObj = Integer.valueOf(1);
                    } else {
                        countObj = countObj.intValue() + 1;
                    }
                    clickCountMap.put(docId, countObj);
                }).orElse(() -> {
                    logger.warn("Not Found for SearchLog: " + clickLog);
                });
            } catch (final Exception e) {
                logger.warn("Failed to process: " + clickLog, e);
            }
        }
        if (!clickLogList.isEmpty()) {
            try {
                final ClickLogBhv clickLogBhv = SingletonLaContainer.getComponent(ClickLogBhv.class);
                clickLogBhv.batchInsert(clickLogList);
            } catch (final Exception e) {
                logger.warn("Failed to insert: " + clickLogList, e);
            }
        }

        if (!clickCountMap.isEmpty()) {
            final SearchService searchService = ComponentUtil.getComponent(SearchService.class);
            try {
                searchService.bulkUpdate(builder -> {
                    final FessConfig fessConfig = ComponentUtil.getFessConfig();
                    searchService.getDocumentListByDocIds(clickCountMap.keySet().toArray(new String[clickCountMap.size()]),
                            new String[] { fessConfig.getIndexFieldDocId() })
                            .forEach(
                                    doc -> {
                                        final String id = DocumentUtil.getValue(doc, fessConfig.getIndexFieldDocId(), String.class);
                                        final String docId = DocumentUtil.getValue(doc, fessConfig.getIndexFieldDocId(), String.class);
                                        if (id != null && docId != null && clickCountMap.containsKey(docId)) {
                                            final Integer count = clickCountMap.get(docId);
                                            final Script script =
                                                    new Script("ctx._source." + fessConfig.getIndexFieldClickCount() + "+="
                                                            + count.toString());
                                            final Map<String, Object> upsertMap = new HashMap<>();
                                            upsertMap.put(fessConfig.getIndexFieldClickCount(), 1);
                                            builder.add(new UpdateRequest(fessConfig.getIndexDocumentIndex(), fessConfig
                                                    .getIndexDocumentType(), id).script(script).upsert(upsertMap));
                                        }
                                    });
                });
            } catch (final Exception e) {
                logger.warn("Failed to update clickCounts", e);
            }
        }
    }
}
