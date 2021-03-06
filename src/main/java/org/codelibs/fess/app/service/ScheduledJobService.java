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
import org.codelibs.fess.app.pager.SchedulerPager;
import org.codelibs.fess.es.config.cbean.ScheduledJobCB;
import org.codelibs.fess.es.config.exbhv.ScheduledJobBhv;
import org.codelibs.fess.es.config.exentity.ScheduledJob;
import org.codelibs.fess.job.JobScheduler;
import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.optional.OptionalEntity;

public class ScheduledJobService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Resource
    protected ScheduledJobBhv scheduledJobBhv;

    public ScheduledJobService() {
        super();
    }

    public List<ScheduledJob> getScheduledJobList(final SchedulerPager scheduledJobPager) {

        final PagingResultBean<ScheduledJob> scheduledJobList = scheduledJobBhv.selectPage(cb -> {
            cb.paging(scheduledJobPager.getPageSize(), scheduledJobPager.getCurrentPageNumber());
            setupListCondition(cb, scheduledJobPager);
        });

        // update pager
        BeanUtil.copyBeanToBean(scheduledJobList, scheduledJobPager, option -> option.include(Constants.PAGER_CONVERSION_RULE));
        scheduledJobPager.setPageNumberList(scheduledJobList.pageRange(op -> {
            op.rangeSize(5);
        }).createPageNumberList());

        return scheduledJobList;
    }

    public OptionalEntity<ScheduledJob> getScheduledJob(final String id) {
        return scheduledJobBhv.selectByPK(id);
    }

    public void delete(final ScheduledJob scheduledJob) {
        setupDeleteCondition(scheduledJob);

        scheduledJobBhv.delete(scheduledJob, op -> {
            op.setRefresh(true);
        });

    }

    @Resource
    protected JobScheduler jobScheduler;

    protected void setupListCondition(final ScheduledJobCB cb, final SchedulerPager scheduledJobPager) {
        if (scheduledJobPager.id != null) {
            cb.query().docMeta().setId_Equal(scheduledJobPager.id);
        }
        // TODO Long, Integer, String supported only.

        // setup condition
        cb.query().addOrderBy_SortOrder_Asc();
        cb.query().addOrderBy_Name_Asc();

        // search

    }

    protected void setupEntityCondition(final ScheduledJobCB cb, final Map<String, String> keys) {

        // setup condition

    }

    protected void setupStoreCondition(final ScheduledJob scheduledJob) {

        // setup condition

    }

    protected void setupDeleteCondition(final ScheduledJob scheduledJob) {

        // setup condition

    }

    public List<ScheduledJob> getScheduledJobList() {
        return scheduledJobBhv.selectList(cb -> {
            cb.query().addOrderBy_SortOrder_Asc();
            cb.query().addOrderBy_Name_Asc();
        });
    }

    public void store(final ScheduledJob scheduledJob) {
        final boolean isNew = scheduledJob.getId() == null;
        setupStoreCondition(scheduledJob);

        scheduledJobBhv.insertOrUpdate(scheduledJob, op -> {
            op.setRefresh(true);
        });
        if (!isNew) {
            jobScheduler.unregister(scheduledJob);
        }
        jobScheduler.register(scheduledJob);
    }

    public List<ScheduledJob> getCrawlerJobList() {
        return scheduledJobBhv.selectList(cb -> {
            cb.query().setCrawler_Equal(Constants.T);
            cb.query().addOrderBy_SortOrder_Asc();
            cb.query().addOrderBy_Name_Asc();
        });
    }

}
