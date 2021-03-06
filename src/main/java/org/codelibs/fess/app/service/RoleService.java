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
import org.codelibs.fess.app.pager.RolePager;
import org.codelibs.fess.es.user.cbean.RoleCB;
import org.codelibs.fess.es.user.exbhv.RoleBhv;
import org.codelibs.fess.es.user.exentity.Role;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.dbflute.cbean.result.PagingResultBean;
import org.dbflute.optional.OptionalEntity;

public class RoleService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Resource
    protected RoleBhv roleBhv;

    @Resource
    protected FessConfig fessConfig;

    public List<Role> getRoleList(final RolePager rolePager) {

        final PagingResultBean<Role> roleList = roleBhv.selectPage(cb -> {
            cb.paging(rolePager.getPageSize(), rolePager.getCurrentPageNumber());
            setupListCondition(cb, rolePager);
        });

        // update pager
        BeanUtil.copyBeanToBean(roleList, rolePager, option -> option.include(Constants.PAGER_CONVERSION_RULE));
        rolePager.setPageNumberList(roleList.pageRange(op -> {
            op.rangeSize(5);
        }).createPageNumberList());

        return roleList;
    }

    public OptionalEntity<Role> getRole(final String id) {
        return roleBhv.selectByPK(id);
    }

    public void store(final Role role) {
        setupStoreCondition(role);

        roleBhv.insertOrUpdate(role, op -> {
            op.setRefresh(true);
        });

    }

    public void delete(final Role role) {
        setupDeleteCondition(role);

        roleBhv.delete(role, op -> {
            op.setRefresh(true);
        });

    }

    protected void setupListCondition(final RoleCB cb, final RolePager rolePager) {
        if (rolePager.id != null) {
            cb.query().docMeta().setId_Equal(rolePager.id);
        }
        // TODO Long, Integer, String supported only.

        // setup condition
        cb.query().addOrderBy_Name_Asc();

        // search

    }

    protected void setupEntityCondition(final RoleCB cb, final Map<String, String> keys) {

        // setup condition

    }

    protected void setupStoreCondition(final Role role) {

        // setup condition

    }

    protected void setupDeleteCondition(final Role role) {

        // setup condition

    }

    public List<Role> getAvailableRoleList(final Integer size) {
        return roleBhv.selectList(cb -> {
            cb.query().matchAll();
            cb.paging(size, 1);
        });
    }

}
