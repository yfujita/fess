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
package org.codelibs.fess.dict;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.codelibs.core.io.FileUtil;
import org.codelibs.core.misc.DynamicProperties;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.codelibs.fess.Constants;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DictionaryManager {
    private static final Logger logger = LoggerFactory.getLogger(DictionaryManager.class);

    @Resource
    protected DynamicProperties crawlerProperties;

    protected List<DictionaryCreator> creatorList = new ArrayList<>();

    @PostConstruct
    public void init() {
        creatorList.forEach(creator -> {
            creator.setDictionaryManager(this);
        });
    }

    public DictionaryFile<? extends DictionaryItem>[] getDictionaryFiles() {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        try (CurlResponse response =
                Curl.get(fessConfig.getElasticsearchUrl() + "/_configsync/file").param("fields", "path,@timestamp").execute()) {
            final Map<String, Object> contentMap = response.getContentAsMap();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> fileList = (List<Map<String, Object>>) contentMap.get("file");
            return fileList
                    .stream()
                    .map(fileMap -> {
                        try {
                            final String path = fileMap.get("path").toString();
                            final Date timestamp =
                                    new SimpleDateFormat(Constants.DATE_FORMAT_ISO_8601_EXTEND_UTC).parse(fileMap.get("@timestamp")
                                            .toString());
                            for (final DictionaryCreator creator : creatorList) {
                                final DictionaryFile<? extends DictionaryItem> file = creator.create(path, timestamp);
                                if (file != null) {
                                    return file;
                                }
                            }
                        } catch (final Exception e) {
                            logger.warn("Failed to load " + fileMap, e);
                        }
                        return null;
                    }).filter(file -> file != null).toArray(n -> new DictionaryFile<?>[n]);
        } catch (final IOException e) {
            throw new DictionaryException("Failed to access dictionaries", e);
        }
    }

    public OptionalEntity<DictionaryFile<? extends DictionaryItem>> getDictionaryFile(final String id) {
        for (final DictionaryFile<? extends DictionaryItem> dictFile : getDictionaryFiles()) {
            if (dictFile.getId().equals(id)) {
                return OptionalEntity.of(dictFile);
            }
        }
        return OptionalEntity.empty();
    }

    public void store(final DictionaryFile<? extends DictionaryItem> dictFile, final File file) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        getDictionaryFile(dictFile.getId()).ifPresent(currentFile -> {
            if (currentFile.getTimestamp().getTime() > dictFile.getTimestamp().getTime()) {
                throw new DictionaryException(dictFile.getPath() + " was updated.");
            }

            // TODO use stream
                try (CurlResponse response =
                        Curl.post(fessConfig.getElasticsearchUrl() + "/_configsync/file").param("path", dictFile.getPath())
                                .body(FileUtil.readUTF8(file)).execute()) {
                    final Map<String, Object> contentMap = response.getContentAsMap();
                    if (!Constants.TRUE.equalsIgnoreCase(contentMap.get("acknowledged").toString())) {
                        throw new DictionaryException("Failed to update " + dictFile.getPath());
                    }
                } catch (final IOException e) {
                    throw new DictionaryException("Failed to update " + dictFile.getPath(), e);
                }

            }).orElse(() -> {
            throw new DictionaryException(dictFile.getPath() + " does not exist.");
        });
    }

    public InputStream getContentInputStream(final DictionaryFile<? extends DictionaryItem> dictFile) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        try {
            return Curl.get(fessConfig.getElasticsearchUrl() + "/_configsync/file").param("path", dictFile.getPath()).execute()
                    .getContentAsStream();
        } catch (final IOException e) {
            throw new DictionaryException("Failed to access " + dictFile.getPath(), e);
        }
    }

    public void addCreator(final DictionaryCreator creator) {
        creatorList.add(creator);
    }

}
