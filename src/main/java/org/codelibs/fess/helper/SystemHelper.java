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

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.RoleTypeService;
import org.codelibs.fess.app.web.base.login.FessLoginAssist;
import org.codelibs.fess.crawler.util.CharUtil;
import org.codelibs.fess.es.config.exentity.RoleType;
import org.codelibs.fess.util.ComponentUtil;
import org.lastaflute.di.core.SingletonLaContainer;
import org.lastaflute.web.util.LaRequestUtil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.icu.util.ULocale;

public class SystemHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Set<String> adminRoleSet = new HashSet<>();

    private String[] crawlerJavaOptions = new String[] { "-Djava.awt.headless=true", "-server", "-Xmx256m", "-XX:MaxMetaspaceSize=128m",
            "-XX:CompressedClassSpaceSize=32m", "-XX:-UseGCOverheadLimit", "-XX:+UseConcMarkSweepGC",
            "-XX:CMSInitiatingOccupancyFraction=75", "-XX:+UseParNewGC", "-XX:+UseTLAB", "-XX:+DisableExplicitGC" };

    private String javaCommandPath = "java";

    private String filterPathEncoding = Constants.UTF_8;

    private boolean useOwnTmpDir = true;

    private String[] supportedHelpLangs = new String[] {};

    private final Map<String, String> designJspFileNameMap = new HashMap<String, String>();

    private String[] supportedUploadedJSExtentions = new String[] { "js" };

    private String[] supportedUploadedCssExtentions = new String[] { "css" };

    private String[] supportedUploadedMediaExtentions = new String[] { "jpg", "jpeg", "gif", "png", "swf" };

    private int maxTextLength = 4000;

    private final AtomicBoolean forceStop = new AtomicBoolean(false);

    protected String[] supportedLanguages = new String[] { "ar", "bg", "ca", "da", "de", "el", "en", "es", "eu", "fa", "fi", "fr", "ga",
            "gl", "hi", "hu", "hy", "id", "it", "ja", "lv", "ko", "nl", "no", "pt", "ro", "ru", "sv", "th", "tr", "zh_CN", "zh_TW", "zh" };

    protected LoadingCache<String, List<Map<String, String>>> langItemsCache;

    @PostConstruct
    public void init() {
        langItemsCache =
                CacheBuilder.newBuilder().maximumSize(20).expireAfterAccess(1, TimeUnit.HOURS)
                        .build(new CacheLoader<String, List<Map<String, String>>>() {
                            @Override
                            public List<Map<String, String>> load(final String key) throws Exception {
                                final ULocale uLocale = new ULocale(key);
                                final Locale displayLocale = uLocale.toLocale();
                                final List<Map<String, String>> langItems = new ArrayList<>(supportedLanguages.length);
                                final String msg = ComponentUtil.getMessageManager().getMessage(displayLocale, "labels.allLanguages");
                                final Map<String, String> defaultMap = new HashMap<>(2);
                                defaultMap.put(Constants.ITEM_LABEL, msg);
                                defaultMap.put(Constants.ITEM_VALUE, "all");
                                langItems.add(defaultMap);

                                for (final String lang : supportedLanguages) {
                                    final Locale locale = LocaleUtils.toLocale(lang);
                                    final String label = locale.getDisplayName(displayLocale);
                                    final Map<String, String> map = new HashMap<>(2);
                                    map.put(Constants.ITEM_LABEL, label);
                                    map.put(Constants.ITEM_VALUE, lang);
                                    langItems.add(map);
                                }
                                return langItems;
                            }
                        });
    }

    public String getUsername() {
        return ComponentUtil.getComponent(FessLoginAssist.class).getSessionUserBean().map(user -> {
            return user.getUserId();
        }).orElse(Constants.GUEST_USER);
    }

    public Date getCurrentTime() {
        return new Date();
    }

    public long getCurrentTimeAsLong() {
        return System.currentTimeMillis();
    }

    public LocalDateTime getCurrentTimeAsLocalDateTime() {
        return LocalDateTime.now();
    }

    public String getLogFilePath() {
        final String value = System.getProperty("fess.log.path");
        if (value != null) {
            return value;
        } else {
            final String userDir = System.getProperty("user.dir");
            final File targetDir = new File(userDir, "target");
            return new File(targetDir, "logs").getAbsolutePath();
        }
    }

    public String encodeUrlFilter(final String path) {
        if (filterPathEncoding == null || path == null) {
            return path;
        }

        try {
            final StringBuilder buf = new StringBuilder();
            for (int i = 0; i < path.length(); i++) {
                final char c = path.charAt(i);
                if (CharUtil.isUrlChar(c) || c == '^' || c == '{' || c == '}' || c == '|' || c == '\\') {
                    buf.append(c);
                } else {
                    buf.append(URLEncoder.encode(String.valueOf(c), filterPathEncoding));
                }
            }
            return buf.toString();
        } catch (final UnsupportedEncodingException e) {
            return path;
        }
    }

    public String getHelpLink(final String name) {
        final String url = ComponentUtil.getFessConfig().getOnlineHelpBaseLink() + name + "-guide.html";
        return LaRequestUtil
                .getOptionalRequest()
                .map(request -> {
                    final Locale locale = request.getLocale();
                    if (locale != null) {
                        final String lang = locale.getLanguage();
                        for (final String l : supportedHelpLangs) {
                            if (l.equals(lang)) {
                                return url.replaceFirst("\\{lang\\}", lang).replaceFirst("\\{version\\}",
                                        Constants.MAJOR_VERSION + "." + Constants.MINOR_VERSION);
                            }
                        }
                    }
                    return getDefaultHelpLink(url);
                }).orElseGet(() -> getDefaultHelpLink(url));
    }

    private String getDefaultHelpLink(final String url) {
        return url.replaceFirst("/\\{lang\\}/", "/").replaceFirst("\\{version\\}", Constants.MAJOR_VERSION + "." + Constants.MINOR_VERSION);
    }

    public void addDesignJspFileName(final String key, final String value) {
        designJspFileNameMap.put(key, value);
    }

    public String getDesignJspFileName(final String fileName) {
        return designJspFileNameMap.get(fileName);
    }

    public Set<String> getAdminRoleSet() {
        return adminRoleSet;
    }

    public void addAdminRoles(final Collection<String> adminRoles) {
        adminRoleSet.addAll(adminRoles);
    }

    public Set<String> getAuthenticatedRoleSet() {
        final RoleTypeService roleTypeService = SingletonLaContainer.getComponent(RoleTypeService.class);
        final List<RoleType> roleTypeList = roleTypeService.getRoleTypeList();

        final Set<String> roleList = new HashSet<>(roleTypeList.size() + adminRoleSet.size());
        for (final RoleType roleType : roleTypeList) {
            roleList.add(roleType.getValue());
        }

        // system roles
        roleList.addAll(adminRoleSet);

        return roleList;
    }

    public String[] getCrawlerJavaOptions() {
        return crawlerJavaOptions;
    }

    public void setCrawlerJavaOptions(final String[] crawlerJavaOptions) {
        this.crawlerJavaOptions = crawlerJavaOptions;
    }

    public String getJavaCommandPath() {
        return javaCommandPath;
    }

    public void setJavaCommandPath(final String javaCommandPath) {
        this.javaCommandPath = javaCommandPath;
    }

    /**
     * @return the filterPathEncoding
     */
    public String getFilterPathEncoding() {
        return filterPathEncoding;
    }

    /**
     * @param filterPathEncoding the filterPathEncoding to set
     */
    public void setFilterPathEncoding(final String filterPathEncoding) {
        this.filterPathEncoding = filterPathEncoding;
    }

    /**
     * @return the useOwnTmpDir
     */
    public boolean isUseOwnTmpDir() {
        return useOwnTmpDir;
    }

    /**
     * @param useOwnTmpDir the useOwnTmpDir to set
     */
    public void setUseOwnTmpDir(final boolean useOwnTmpDir) {
        this.useOwnTmpDir = useOwnTmpDir;
    }

    /**
     * @return the supportedHelpLangs
     */
    public String[] getSupportedHelpLangs() {
        return supportedHelpLangs;
    }

    /**
     * @param supportedHelpLangs the supportedHelpLangs to set
     */
    public void setSupportedHelpLangs(final String[] supportedHelpLangs) {
        this.supportedHelpLangs = supportedHelpLangs;
    }

    public String[] getSupportedUploadedJSExtentions() {
        return supportedUploadedJSExtentions;
    }

    public void setSupportedUploadedJSExtentions(final String[] supportedUploadedJSExtentions) {
        this.supportedUploadedJSExtentions = supportedUploadedJSExtentions;
    }

    public String[] getSupportedUploadedCssExtentions() {
        return supportedUploadedCssExtentions;
    }

    public void setSupportedUploadedCssExtentions(final String[] supportedUploadedCssExtentions) {
        this.supportedUploadedCssExtentions = supportedUploadedCssExtentions;
    }

    public String[] getSupportedUploadedMediaExtentions() {
        return supportedUploadedMediaExtentions;
    }

    public void setSupportedUploadedMediaExtentions(final String[] supportedUploadedMediaExtentions) {
        this.supportedUploadedMediaExtentions = supportedUploadedMediaExtentions;
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    public void setMaxTextLength(final int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    public boolean isForceStop() {
        return forceStop.get();
    }

    public void setForceStop(final boolean b) {
        forceStop.set(true);
    }

    public String generateDocId(final Map<String, Object> map) {
        return UUID.randomUUID().toString().replace("-", StringUtil.EMPTY);
    }

    public String abbreviateLongText(final String str) {
        return StringUtils.abbreviate(str, maxTextLength);
    }

    public String normalizeLang(final String value) {
        if (StringUtil.isBlank(value)) {
            return null;
        }

        final String localeName = value.trim().toLowerCase(Locale.ENGLISH).replace("-", "_");

        for (final String supportedLang : supportedLanguages) {
            if (localeName.startsWith(supportedLang.toLowerCase(Locale.ENGLISH))) {
                return supportedLang;
            }
        }
        return null;
    }

    public List<Map<String, String>> getLanguageItems(final Locale locale) {
        try {
            final String localeStr = locale.toString();
            return langItemsCache.get(localeStr);
        } catch (final ExecutionException e) {
            final List<Map<String, String>> langItems = new ArrayList<>(supportedLanguages.length);
            final String msg = ComponentUtil.getMessageManager().getMessage(locale, "labels.allLanguages");
            final Map<String, String> defaultMap = new HashMap<>(2);
            defaultMap.put(Constants.ITEM_LABEL, msg);
            defaultMap.put(Constants.ITEM_VALUE, "all");
            langItems.add(defaultMap);
            return langItems;
        }
    }

    public String[] getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(final String[] supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }
}
