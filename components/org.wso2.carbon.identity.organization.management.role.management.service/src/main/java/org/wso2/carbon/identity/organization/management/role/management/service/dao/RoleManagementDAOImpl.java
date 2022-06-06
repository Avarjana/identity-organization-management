/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.role.management.service.dao;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.FilterOperator;
import org.wso2.carbon.identity.organization.management.role.management.service.models.FilterQueryBuilder;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Group;
import org.wso2.carbon.identity.organization.management.role.management.service.models.PatchOperation;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Permission;
import org.wso2.carbon.identity.organization.management.role.management.service.models.Role;
import org.wso2.carbon.identity.organization.management.role.management.service.models.User;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.AND_OPERATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.COMMA_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.GROUPS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.OR_OPERATOR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.PERMISSIONS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.ROLE_ACTION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants.USERS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_PERMISSION_IF_NOT_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_PERMISSION_IF_NOT_EXISTS_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_GROUP_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_GROUP_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_PERMISSION_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_UM_ORG_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_USER_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.ADD_ROLE_USER_MAPPING_INSERT_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.AND;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_GROUP_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_GROUP_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_PERMISSION_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_PERMISSION_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_ROLE_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_ROLE_NAME_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_USER_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.CHECK_USER_ROLE_MAPPING_EXISTS;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_GROUPS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_GROUPS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_PERMISSIONS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_ROLE_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_USERS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.DELETE_USERS_FROM_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_GROUPS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_GROUP_IDS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_IDS_FROM_ROLE_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSIONS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_ID_FROM_STRING;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_ID_FROM_STRING_VALUES;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_STRINGS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_PERMISSION_STRINGS_FROM_ROLE_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLES_FROM_ORGANIZATION_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_ROLE_FROM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_USERS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.GET_USER_IDS_FROM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_COUNT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_LIMIT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.TENANT_ID_APPENDER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.UM_ACTION_APPENDER;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.UPDATE_ROLE_DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ADDING_GROUP_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ADDING_PERMISSION_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ADDING_ROLE_TO_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ADDING_USER_TO_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_GROUPS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_GROUP_VALIDITY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_ROLES_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_ROLE_FROM_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_USERS_USING_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_GETTING_USER_VALIDITY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_PATCHING_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_GROUPS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_PERMISSIONS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_ROLE_FROM_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REMOVING_USERS_FROM_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_REPLACING_DISPLAY_NAME_OF_ROLE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ROLE_NAME_OR_ID_REQUIRED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of RoleManagementDAO Interface.
 */
public class RoleManagementDAOImpl implements RoleManagementDAO {

    @Override
    public void createRole(String organizationId, int tenantId, Role role)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(ADD_ROLE_UM_ORG_ROLE,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, role.getDisplayName());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        }, role, false);
                if (CollectionUtils.isNotEmpty(role.getGroups())) {
                    assignRoleAttributes(role.getGroups().stream().map(Group::getGroupId).collect(Collectors.toList()),
                            role.getId(), ADD_ROLE_GROUP_MAPPING, ADD_ROLE_GROUP_MAPPING_INSERT_VALUES,
                            DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, ERROR_CODE_ADDING_GROUP_TO_ROLE);
                }
                if (CollectionUtils.isNotEmpty(role.getUsers())) {
                    assignRoleAttributes(role.getUsers().stream().map(User::getId).collect(Collectors.toList()),
                            role.getId(), ADD_ROLE_USER_MAPPING, ADD_ROLE_USER_MAPPING_INSERT_VALUES,
                            DB_SCHEMA_COLUMN_NAME_UM_USER_ID, ERROR_CODE_ADDING_USER_TO_ROLE);
                }
                if (CollectionUtils.isNotEmpty(role.getPermissions())) {
                    List<String> nonExistingPermissions = getNonExistingPermissions(role.getPermissions(), tenantId,
                            role.getId());
                    addNonExistingPermissions(nonExistingPermissions, role.getId(), tenantId);
                    List<String> permissionList = getPermissionIds(role.getPermissions(),
                            getTenantId()).stream().map(Object::toString).collect(Collectors.toList());
                    assignRoleAttributes(permissionList, role.getId(), ADD_ROLE_PERMISSION_MAPPING,
                            ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID,
                            ERROR_CODE_ADDING_PERMISSION_TO_ROLE);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ADDING_ROLE_TO_ORGANIZATION, e);
        }
    }

    @Override
    public Role getRoleById(String organizationId, String roleId, int tenantId)
            throws OrganizationManagementServerException {

        //TODO: the 3rd step should be implemented.
        //3. When getting the permissions, the unwanted, bogus permissions should be culled.
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            Role role = namedJdbcTemplate.withTransaction(template -> template.fetchSingleRecord(GET_ROLE_FROM_ID,
                    (resultSet, rowNumber) -> new Role(roleId, resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                    }
            ));
            if (Objects.nonNull(role)) {
                List<Group> groupList = getGroupsFromRoleId(roleId);
                List<User> usersList = getUsersFromRoleId(roleId);
                List<String> permissionsList = getPermissionsFromRoleId(roleId);
                if (CollectionUtils.isNotEmpty(groupList)) {
                    role.setGroups(groupList);
                }
                if (CollectionUtils.isNotEmpty(usersList)) {
                    role.setUsers(usersList);
                }
                if (CollectionUtils.isNotEmpty(permissionsList)) {
                    role.setPermissions(permissionsList);
                }
            }
            return role;
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_GETTING_ROLE_FROM_ID, e, roleId);
        }
    }

    @Override
    public List<Role> getOrganizationRoles(String organizationId, int tenantId, int limit,
                                           List<ExpressionNode> expressionNodes, List<String> operators)
            throws OrganizationManagementServerException {

        FilterQueryBuilder filterQueryBuilder = new FilterQueryBuilder();
        appendFilterQuery(expressionNodes, operators, DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, filterQueryBuilder);
        Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();
        String filterQuery = StringUtils.isNotBlank(filterQueryBuilder.getFilterQuery()) ?
                filterQueryBuilder.getFilterQuery() + AND : StringUtils.EMPTY;

        String sqlStm = GET_ROLES_FROM_ORGANIZATION_ID + filterQuery + GET_ROLES_FROM_ORGANIZATION_ID_TAIL;

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    sqlStm,
                    (resultSet, rowNumber) -> new Role(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                            namedPreparedStatement.setString(entry.getKey(), entry.getValue());
                        }
                        namedPreparedStatement.setInt(DB_SCHEMA_LIMIT, limit);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_ROLES_FROM_ORGANIZATION, e, organizationId);
        }
    }

    @Override
    public Role patchRole(String organizationId, String roleId, int tenantId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.withTransaction(template -> {
                for (PatchOperation patchOp : patchOperations) {
                    if (StringUtils.equalsIgnoreCase(patchOp.getOp(), PATCH_OP_ADD)) {
                        patchOperationAdd(roleId, patchOp.getPath(), patchOp.getValues());
                    } else if (StringUtils.equalsIgnoreCase(patchOp.getOp(), PATCH_OP_REMOVE)) {
                        /* If values are passed they should be on the path param. Therefore, if values are passed
                        with this, they would not be considered. */
                        patchOperationRemove(roleId, patchOp.getPath());
                    } else if (StringUtils.equalsIgnoreCase(patchOp.getOp(), PATCH_OP_REPLACE)) {
                        patchOperationReplace(roleId, patchOp.getPath(), patchOp.getValues());
                    }
                }
                return getRoleById(organizationId, roleId, tenantId);
            });
        } catch (TransactionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof OrganizationManagementClientException) {
                throw new OrganizationManagementClientException(cause.getMessage(),
                        ((OrganizationManagementException) cause).getDescription(),
                        ((OrganizationManagementException) cause).getErrorCode());
            } else if (cause instanceof OrganizationManagementServerException) {
                throw new OrganizationManagementServerException(cause.getMessage(),
                        ((OrganizationManagementException) cause).getDescription(),
                        ((OrganizationManagementException) cause).getErrorCode(), cause);
            }
            throw handleServerException(ERROR_CODE_PATCHING_ROLE, e, organizationId);
        }
    }

    @Override
    public void deleteRole(String organizationId, String roleId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ROLE_FROM_ORGANIZATION,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_REMOVING_ROLE_FROM_ORGANIZATION, e, roleId, organizationId);
        }
    }

    @Override
    public Role putRole(String organizationId, String roleId, Role role, int tenantId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                List<String> users = getUsersFromRoleId(roleId).stream().map(User::getId)
                        .collect(Collectors.toList());
                List<String> groups = getGroupsFromRoleId(roleId).stream().map(Group::getGroupId)
                        .collect(Collectors.toList());
                List<String> permissions = getPermissionsWithIdFromRoleId(roleId).stream()
                        .map(Permission::getId).map(Object::toString).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(users)) {
                    removeUsersFromRole(users, roleId);
                }
                if (CollectionUtils.isNotEmpty(groups)) {
                    removeGroupsFromRole(groups, roleId);
                }
                if (CollectionUtils.isNotEmpty(permissions)) {
                    removePermissionsFromRole(permissions, roleId);
                }
                template.executeUpdate(UPDATE_ROLE_DISPLAY_NAME,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, role.getDisplayName());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, role.getId());
                        });
                if (CollectionUtils.isNotEmpty(role.getUsers())) {
                    assignRoleAttributes(role.getUsers().stream().map(User::getId).collect(Collectors.toList()),
                            roleId, ADD_ROLE_USER_MAPPING, ADD_ROLE_USER_MAPPING_INSERT_VALUES,
                            DB_SCHEMA_COLUMN_NAME_UM_USER_ID, ERROR_CODE_ADDING_USER_TO_ROLE);
                }
                if (CollectionUtils.isNotEmpty(role.getGroups())) {
                    assignRoleAttributes(role.getGroups().stream().map(Group::getGroupId).collect(Collectors.toList()),
                            roleId, ADD_ROLE_GROUP_MAPPING, ADD_ROLE_GROUP_MAPPING_INSERT_VALUES,
                            DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, ERROR_CODE_ADDING_GROUP_TO_ROLE);
                }
                if (CollectionUtils.isNotEmpty(role.getPermissions())) {
                    List<String> nonExistingPermissions = getNonExistingPermissions(role.getPermissions(), tenantId,
                            role.getId());
                    addNonExistingPermissions(nonExistingPermissions, role.getId(), tenantId);
                    List<String> permissionList = getPermissionIds(role.getPermissions(),
                            getTenantId()).stream().map(Object::toString).collect(Collectors.toList());
                    assignRoleAttributes(permissionList, roleId, ADD_ROLE_PERMISSION_MAPPING,
                            ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID,
                            ERROR_CODE_ADDING_PERMISSION_TO_ROLE);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_PATCHING_ROLE, e, organizationId);
        }
        return role;
    }

    @Override
    public boolean checkRoleExists(String organizationId, String roleId, String roleName)
            throws OrganizationManagementException {

        if ((StringUtils.isBlank(roleId) && StringUtils.isBlank(roleName)) ||
                (StringUtils.isNotBlank(roleId) && StringUtils.isNotBlank(roleName))) {
            throw handleClientException(ERROR_CODE_ROLE_NAME_OR_ID_REQUIRED, organizationId);
        }
        String roleAttribute = StringUtils.isBlank(roleId) ? roleName : roleId;
        String roleParameter = StringUtils.isBlank(roleId) ? DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME :
                DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID;
        String stm = StringUtils.isBlank(roleId) ? CHECK_ROLE_NAME_EXISTS : CHECK_ROLE_EXISTS;
        ErrorMessages errorMessage = StringUtils.isBlank(roleId) ?
                ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_NAME :
                ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_ID;

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            int roleCount = namedJdbcTemplate.fetchSingleRecord(stm,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(roleParameter, roleAttribute);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ORG_ID, organizationId);
                    });
            return roleCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(errorMessage, e, roleId, organizationId);
        }
    }

    @Override
    public boolean checkUserExists(String userId, int tenantId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            int value = namedJdbcTemplate.fetchSingleRecord(CHECK_USER_EXISTS,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID, userId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                    });
            return value > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_USER_VALIDITY, e, userId);
        }
    }

    @Override
    public boolean checkGroupExists(String groupId, int tenantId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            int value = namedJdbcTemplate.fetchSingleRecord(CHECK_GROUP_EXISTS,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, groupId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                    });
            return value > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_GROUP_VALIDITY, e, groupId);
        }
    }

    /**
     * Check whether the permissions are in the UM_ORG_PERMISSION table and select the permission that are
     * not in the UM_ORG_PERMISSION table by returning them.
     *
     * @param permissions The list of permissions.
     * @param tenantId    The tenant ID.
     * @param roleId      The role ID.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs
     *                                               during checking whether the permissions exist or not.
     */
    private List<String> getNonExistingPermissions(List<String> permissions, int tenantId, String roleId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<String> nonExistingPermissions = new ArrayList<>();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                for (String permission : permissions) {
                    int value = namedJdbcTemplate.fetchSingleRecord(CHECK_PERMISSION_EXISTS,
                            (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID, permission);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                            });
                    if (value == 0) {
                        nonExistingPermissions.add(permission);
                    }
                }
                return null;
            });
            return nonExistingPermissions;
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Add non-existing permissions.
     *
     * @param permissionList The list of permissions.
     * @param roleId         The ID of role.
     * @param tenantId       The ID of the tenant.
     * @throws OrganizationManagementServerException This exception is thrown when an error occurs while adding
     *                                               permissions.
     */
    private void addNonExistingPermissions(List<String> permissionList, String roleId, int tenantId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfPermissions = permissionList.size();
        try {
            if (CollectionUtils.isNotEmpty(permissionList)) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeInsert(buildQueryForInsertAndRemoveValues(numberOfPermissions,
                            ADD_PERMISSION_IF_NOT_EXISTS,
                            ADD_PERMISSION_IF_NOT_EXISTS_VALUES, COMMA_SEPARATOR), (namedPreparedStatement -> {
                        for (int i = 0; i < numberOfPermissions; i++) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + i,
                                    permissionList.get(i));
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION + i, ROLE_ACTION);
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + i, tenantId);
                        }
                    }), permissionList, false);
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ADDING_PERMISSION_TO_ROLE, e, roleId);
        }
    }

    /**
     * The replace patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @param values The value for the patch operation.
     * @throws OrganizationManagementException The exception is thrown when an error occurs during patch
     *                                         operation.
     */
    private void patchOperationReplace(String roleId, String path, List<String> values)
            throws OrganizationManagementException {

        if (StringUtils.equals(path, DISPLAY_NAME)) {
            replaceDisplayName(values.get(0), roleId);
        } else if (StringUtils.equalsIgnoreCase(path, USERS)) {
            List<User> users = getUsersFromRoleId(roleId);
            if (CollectionUtils.isNotEmpty(users)) {
                removeUsersFromRole(users.stream().map(User::getId).collect(Collectors.toList()), roleId);
            }
            assignRoleAttributes(values, roleId, ADD_ROLE_USER_MAPPING, ADD_ROLE_USER_MAPPING_INSERT_VALUES,
                    DB_SCHEMA_COLUMN_NAME_UM_USER_ID, ERROR_CODE_ADDING_USER_TO_ROLE);
        } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
            List<Group> groups = getGroupsFromRoleId(roleId);
            if (CollectionUtils.isNotEmpty(groups)) {
                removeGroupsFromRole(groups.stream().map(Group::getGroupId)
                        .collect(Collectors.toList()), roleId);
            }
            assignRoleAttributes(values, roleId, ADD_ROLE_GROUP_MAPPING, ADD_ROLE_GROUP_MAPPING_INSERT_VALUES,
                    DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, ERROR_CODE_ADDING_GROUP_TO_ROLE);
        } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
            List<Permission> permissions = getPermissionsWithIdFromRoleId(roleId);
            if (CollectionUtils.isNotEmpty(permissions)) {
                removePermissionsFromRole(permissions.stream().map(Permission::getId)
                        .map(Object::toString).collect(Collectors.toList()), roleId);
            }
            List<String> nonExistingPermissions = getNonExistingPermissions(values, getTenantId(), roleId);
            addNonExistingPermissions(nonExistingPermissions, roleId, getTenantId());
            List<String> permissionList = getPermissionIds(values,
                    getTenantId()).stream().map(Object::toString).collect(Collectors.toList());
            assignRoleAttributes(permissionList, roleId, ADD_ROLE_PERMISSION_MAPPING,
                    ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID,
                    ERROR_CODE_ADDING_PERMISSION_TO_ROLE);
        }
    }

    /**
     * The remove patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @throws OrganizationManagementException The error is thrown when and error occurs during patch operation.
     */
    private void patchOperationRemove(String roleId, String path) throws OrganizationManagementException {

        String patchPath = path;
        if (patchPath.contains("[")) {
            patchPath = patchPath.split("\\[")[0];
        }
        patchPath = StringUtils.strip(patchPath);

        //get the values associated with the path.
        String pathValues = StringUtils.strip(path.split("\\[")[1].replace("]", ""))
                .toLowerCase();

        if (StringUtils.isNotBlank(pathValues)) {
            if (StringUtils.equalsIgnoreCase(patchPath, GROUPS)) {
                patchRemoveOpWithFilters(roleId, pathValues, GROUPS, DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID,
                        GET_GROUP_IDS_FROM_ROLE_ID, GET_IDS_FROM_ROLE_ID_TAIL,
                        DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, ERROR_CODE_GETTING_GROUPS_USING_ROLE_ID);
            } else if (StringUtils.equalsIgnoreCase(patchPath, USERS)) {
                patchRemoveOpWithFilters(roleId, pathValues, USERS, DB_SCHEMA_COLUMN_NAME_UM_USER_ID,
                        GET_USER_IDS_FROM_ROLE_ID, GET_IDS_FROM_ROLE_ID_TAIL, DB_SCHEMA_COLUMN_NAME_UM_USER_ID,
                        ERROR_CODE_GETTING_USERS_USING_ROLE_ID);
            } else if (StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS)) {
                patchRemoveOpWithFilters(roleId, pathValues, PERMISSIONS, DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID,
                        GET_PERMISSION_STRINGS_FROM_ROLE_ID, GET_PERMISSION_STRINGS_FROM_ROLE_ID_TAIL,
                        DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID, ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID);
            }
        } else if (StringUtils.equals(patchPath, StringUtils.strip(path))) {
            if (StringUtils.equalsIgnoreCase(patchPath, USERS)) {
                List<String> userIdList = getUsersFromRoleId(roleId).stream().map(User::getId)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(userIdList)) {
                    removeUsersFromRole(userIdList, roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(patchPath, GROUPS)) {
                List<String> groupIdList = getGroupsFromRoleId(roleId).stream().map(Group::getGroupId)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(groupIdList)) {
                    removeGroupsFromRole(groupIdList, roleId);
                }
            } else if (StringUtils.equalsIgnoreCase(patchPath, PERMISSIONS)) {
                List<String> permissionIdList = getPermissionIds(getPermissionsFromRoleId(roleId),
                        getTenantId()).stream().map(Object::toString).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(permissionIdList)) {
                    removePermissionsFromRole(permissionIdList, roleId);
                }
            }
        }
    }

    /**
     * The add patch operation.
     *
     * @param roleId The role ID.
     * @param path   The path of the patch operation.
     * @param values The value for the patch operation.
     * @throws OrganizationManagementException The exception is thrown when an error occurs during patch operation.
     */
    private void patchOperationAdd(String roleId, String path, List<String> values)
            throws OrganizationManagementException {

        if (StringUtils.equalsIgnoreCase(path, USERS)) {
            List<String> newUserList = findNonAssignedAttributeValues(roleId, CHECK_USER_ROLE_MAPPING_EXISTS,
                    DB_SCHEMA_COLUMN_NAME_UM_USER_ID, false, values);
            if (CollectionUtils.isNotEmpty(newUserList)) {
                assignRoleAttributes(newUserList, roleId, ADD_ROLE_USER_MAPPING,
                        ADD_ROLE_USER_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_USER_ID,
                        ERROR_CODE_ADDING_USER_TO_ROLE);
            }
        } else if (StringUtils.equalsIgnoreCase(path, GROUPS)) {
            List<String> newGroupList = findNonAssignedAttributeValues(roleId, CHECK_GROUP_ROLE_MAPPING_EXISTS,
                    DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID, false, values);
            if (CollectionUtils.isNotEmpty(newGroupList)) {
                assignRoleAttributes(newGroupList, roleId, ADD_ROLE_GROUP_MAPPING,
                        ADD_ROLE_GROUP_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID,
                        ERROR_CODE_ADDING_GROUP_TO_ROLE);
            }
        } else if (StringUtils.equalsIgnoreCase(path, PERMISSIONS)) {
            List<String> newPermissionList = findNonAssignedAttributeValues(roleId,
                    CHECK_PERMISSION_ROLE_MAPPING_EXISTS, DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID, true,
                    values);
            if (CollectionUtils.isNotEmpty(newPermissionList)) {
                List<String> permissionList = getPermissionIds(newPermissionList, getTenantId()).stream()
                        .map(Object::toString).collect(Collectors.toList());
                assignRoleAttributes(permissionList, roleId, ADD_ROLE_PERMISSION_MAPPING,
                        ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES, DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID,
                        ERROR_CODE_ADDING_PERMISSION_TO_ROLE);
            }
        } else if (StringUtils.equals(path, DISPLAY_NAME)) {
            replaceDisplayName(values.get(0), roleId);
        }
    }

    /**
     * Find the non-assigned attribute values for a role.
     *
     * @param roleId          The ID of the role.
     * @param query           The query for checking the existence of an attribute.
     * @param attributeColumn The column mapping the attribute ID.
     * @param isPermission    If it is a permission then it is true, else false.
     * @param values          The list of attribute values to be checked on.
     * @return A list of non-assigned attribute values.
     * @throws OrganizationManagementException Throw an exception if an error occurs while retrieving non-assigned
     *                                         attributes.
     */
    private List<String> findNonAssignedAttributeValues(String roleId, String query, String attributeColumn,
                                                        boolean isPermission, List<String> values)
            throws OrganizationManagementException {

        List<String> newAttributeValueList = new ArrayList<>();
        for (String value : values) {
            if (!checkRoleAttributeMapping(value, roleId, query, attributeColumn, isPermission)) {
                newAttributeValueList.add(value);
            }
        }
        return newAttributeValueList;
    }

    /**
     * Check the attribute values mapped to a role (groups, users and permissions).
     *
     * @param attributeId     The ID of the attribute.
     * @param roleId          The role ID.
     * @param query           The query to check whether the attribute exists or not.
     * @param attributeColumn The column name for checking the attribute ID.
     * @param isPermission    If the attribute is a permission it is true, else false.
     * @return If the attribute exists return true, else false.
     * @throws OrganizationManagementServerException Throws a server exception if an error occurs while checking the
     *                                               attribute is mapped to the role.
     */
    private boolean checkRoleAttributeMapping(String attributeId, String roleId, String query, String attributeColumn,
                                              boolean isPermission)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            int value = namedJdbcTemplate.fetchSingleRecord(query,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(attributeColumn, attributeId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                        if (isPermission) {
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, getTenantId());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                        }
                    });
            return value > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_PATCHING_ROLE, e, roleId);
        }
    }

    /**
     * Appending the filter query for filter query builder.
     *
     * @param expressionNodes    The list of expression nodes.
     * @param operators          The list of operators.
     * @param dbColumnName       The database column name for filtering.
     * @param filterQueryBuilder The filterQueryBuilder object.
     */
    private void appendFilterQuery(List<ExpressionNode> expressionNodes, List<String> operators,
                                   String dbColumnName, FilterQueryBuilder filterQueryBuilder) {

        if (CollectionUtils.isEmpty(expressionNodes)) {
            filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
        } else {
            StringBuilder filter = new StringBuilder();
            for (int i = 0; i < expressionNodes.size(); i++) {
                ExpressionNode expressionNode = expressionNodes.get(i);
                String operation = expressionNode.getOperation();
                String value = expressionNode.getValue();
                if (StringUtils.isNotBlank(dbColumnName) && StringUtils.isNotBlank(value) &&
                        StringUtils.isNotBlank(operation)) {
                    FilterOperator operator = FilterOperator.valueOf(operation.trim().toUpperCase());
                    filter.append(dbColumnName).append(applyFilterOperation(i, operator));
                    if (i != expressionNodes.size() - 1) {
                        String op = operators.get(i);
                        if (StringUtils.equalsIgnoreCase(op, AND_OPERATOR)) {
                            filter.append(AND);
                        } else if (StringUtils.equalsIgnoreCase(op, OR_OPERATOR)) {
                            filter.append(OR);
                        }
                    }
                    filterQueryBuilder.setFilterAttributeValue(operator.getPrefix() + value + operator.getSuffix());
                }
            }
            filterQueryBuilder.setFilterQuery(StringUtils.isBlank(filter.toString()) ? StringUtils.EMPTY :
                    filter.toString());
        }
    }

    /**
     * The filtering for patch operation, remove operation.
     *
     * @param roleId          The ID of the role.
     * @param pathValues      The values passed along with path.
     * @param basePath        The patch operation path.
     * @param filterDbColumn  The database column for filter.
     * @param baseQuery       The query for filtering.
     * @param tailQuery       The tail query for filtering.
     * @param grabberDbColumn The database column for getting data.
     * @param errorMessage    The error message.
     * @throws OrganizationManagementException This exception is thrown when an error occurs while retrieving
     *                                         the values.
     */
    private void patchRemoveOpWithFilters(String roleId, String pathValues, String basePath, String filterDbColumn,
                                          String baseQuery, String tailQuery, String grabberDbColumn,
                                          ErrorMessages errorMessage) throws OrganizationManagementException {

        List<ExpressionNode> expressionNodes = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        try {
            FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(pathValues);
            Node rootNode = filterTreeBuilder.buildTree();
            Utils.setExpressionNodeAndOperatorLists(rootNode, expressionNodes, operators, false);
            FilterQueryBuilder filterQueryBuilder = new FilterQueryBuilder();
            appendFilterQuery(expressionNodes, operators, filterDbColumn, filterQueryBuilder);
            Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();
            String filterQuery = StringUtils.isNotBlank(filterQueryBuilder.getFilterQuery()) ?
                    filterQueryBuilder.getFilterQuery() + AND : StringUtils.EMPTY;
            String query = baseQuery + filterQuery + tailQuery;
            List<String> list = patchRemoveOpDataGrabber(query, roleId, grabberDbColumn, filterAttributeValue,
                    errorMessage);
            if (CollectionUtils.isNotEmpty(list)) {
                if (StringUtils.equalsIgnoreCase(basePath, GROUPS)) {
                    removeGroupsFromRole(list, roleId);
                } else if (StringUtils.equalsIgnoreCase(basePath, USERS)) {
                    removeUsersFromRole(list, roleId);
                } else if (StringUtils.equalsIgnoreCase(basePath, PERMISSIONS)) {
                    removePermissionsFromRole(list, roleId);
                }
            }
        } catch (IOException | IdentityException e) {
            throw handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
    }

    /**
     * Data grabber for patch operation - remove operation.
     *
     * @param query                The query to run.
     * @param roleId               The ID of the role.
     * @param dbColumn             The column name of the result set the values are needed.
     * @param filterAttributeValue The Map object containing filter attribute values.
     * @param errorMessage         The corresponding error message.
     * @return The list of IDs.
     * @throws OrganizationManagementServerException Throw an exception if an error occurs while retrieving data.
     */
    private List<String> patchRemoveOpDataGrabber(String query, String roleId, String dbColumn,
                                                  Map<String, String> filterAttributeValue, ErrorMessages errorMessage)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(query, (resultSet, rowNumber) -> resultSet.getString(dbColumn),
                    namedPreparedStatement -> {
                        for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                            namedPreparedStatement.setString(entry.getKey(), entry.getValue());
                        }
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(errorMessage, e, roleId);
        }
    }

    /**
     * Get the users assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of users.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs while
     *                                               retrieving the users assigned for a particular role.
     */
    private List<User> getUsersFromRoleId(String roleId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_USERS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new User(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID)),
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_USERS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Get the groups assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of groups.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               retrieving groups assigned to a particular role.
     */
    private List<Group> getGroupsFromRoleId(String roleId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_GROUPS_FROM_ROLE_ID,
                    (resultSet, rowNumber) ->
                            new Group(resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID),
                                    resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME)),
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_GROUPS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Get the permissions assigned for a particular role.
     *
     * @param roleId The role ID.
     * @return The list of permissions.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               retrieving permissions from a particular role.
     */
    private List<String> getPermissionsFromRoleId(String roleId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    GET_PERMISSIONS_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID),
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Get permission strings and their ids assigned to a role.
     *
     * @param roleId The role ID.
     * @return The list of permissions.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               retrieving the permissions.
     */
    private List<Permission> getPermissionsWithIdFromRoleId(String roleId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID,
                    (resultSet, rowNumber) -> new Permission(resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID)),
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID, e, roleId);
        }
    }

    /**
     * Assigning a list of groups, users or permissions.
     *
     * @param valueList    The list containing group IDs, user IDs or permission strings.
     * @param roleId       The role ID.
     * @param baseQuery    The base query for inserting values.
     * @param mappingQuery The mapping query for inserting values.
     * @param dbColumnName The group ID, user ID or permission resource ID column.
     * @param errorMessage The error message corresponding to the groups or users.
     * @throws OrganizationManagementServerException The exception is thrown if an error occurs while inserting values.
     */
    private void assignRoleAttributes(List<String> valueList, String roleId, String baseQuery, String mappingQuery,
                                      String dbColumnName, ErrorMessages errorMessage)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfGroups = valueList.size();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(buildQueryForInsertAndRemoveValues(numberOfGroups, baseQuery,
                                mappingQuery, COMMA_SEPARATOR),
                        namedPreparedStatement -> {
                            for (int i = 0; i < numberOfGroups; i++) {
                                namedPreparedStatement.setString(dbColumnName + i,
                                        valueList.get(i));
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i,
                                        roleId);

                            }
                        }, valueList, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(errorMessage, e, roleId);
        }
    }

    /**
     * Retrieve the IDs of the permission strings.
     *
     * @param permissionStrings The list of permissions strings.
     * @param tenantId          The tenant ID.
     * @return The list of permission Ids.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs during
     *                                               getting IDs from permissions.
     */
    private List<Integer> getPermissionIds(List<String> permissionStrings, int tenantId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfPermissions = permissionStrings.size();
        try {
            return namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    buildQueryForGettingPermissionIdsFromString(numberOfPermissions),
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID),
                    namedPreparedStatement -> {
                        for (int i = 0; i < numberOfPermissions; i++) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + i,
                                    permissionStrings.get(i));
                        }
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ACTION, ROLE_ACTION);
                    }));
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING, e);
        }
    }

    /**
     * Remove the groups assigned to a role.
     *
     * @param groupList The list of group IDs.
     * @param roleId    The role ID.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs while
     *                                               removing the assigned groups of a role.
     */
    private void removeGroupsFromRole(List<String> groupList, String roleId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfGroups = groupList.size();
        try {
            if (numberOfGroups > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForInsertAndRemoveValues(numberOfGroups, DELETE_GROUPS_FROM_ROLE,
                                    DELETE_GROUPS_FROM_ROLE_MAPPING, OR),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfGroups; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i,
                                            roleId);
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + i,
                                            groupList.get(i));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_REMOVING_GROUPS_FROM_ROLE, e, roleId);
        }
    }

    /**
     * Remove the assigned permissions from a role.
     *
     * @param permissionList The list of permissions.
     * @param roleId         The role ID.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs while removing
     *                                               the assigned permissions from a role.
     */
    private void removePermissionsFromRole(List<String> permissionList, String roleId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfPermissions = permissionList.size();
        try {
            if (numberOfPermissions > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForInsertAndRemoveValues(numberOfPermissions,
                                    DELETE_PERMISSIONS_FROM_ROLE, DELETE_PERMISSIONS_FROM_ROLE_MAPPING, OR),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfPermissions; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + i,
                                            Integer.parseInt(permissionList.get(i)));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_REMOVING_PERMISSIONS_FROM_ROLE, e);
        }
    }

    /**
     * Remove the assigned users from a role.
     *
     * @param usersList The list of users.
     * @param roleId    The role ID.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs while removing
     *                                               the assigned users from a role.
     */
    private void removeUsersFromRole(List<String> usersList, String roleId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int numberOfUsers = usersList.size();
        try {
            if (numberOfUsers > 0) {
                namedJdbcTemplate.withTransaction(template -> {
                    template.executeUpdate(buildQueryForInsertAndRemoveValues(numberOfUsers, DELETE_USERS_FROM_ROLE,
                                    DELETE_USERS_FROM_ROLE_MAPPING, OR),
                            namedPreparedStatement -> {
                                for (int i = 0; i < numberOfUsers; i++) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + i, roleId);
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_USER_ID + i,
                                            usersList.get(i));
                                }
                            });
                    return null;
                });
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_REMOVING_USERS_FROM_ROLE, e, roleId);
        }
    }

    /**
     * Query builder for inserting and removing groups, users and permissions.
     *
     * @param numberOfElements Number of elements in the list.
     * @param baseQuery        The base insert or remove query.
     * @param mappingQuery     The query containing mapping values.
     * @param mappingConnector The mapping value connector.
     * @return The final query string for inserting or removing values.
     */
    private String buildQueryForInsertAndRemoveValues(int numberOfElements, String baseQuery, String mappingQuery,
                                                      String mappingConnector) {

        StringBuilder sb = new StringBuilder(baseQuery);
        for (int i = 0; i < numberOfElements; i++) {
            sb.append(String.format(mappingQuery, i));
            if (i != numberOfElements - 1) {
                sb.append(mappingConnector);
            }
        }
        return sb.toString();
    }

    /**
     * A query builder to get the permission IDs from permission strings.
     *
     * @param numberOfElements Number of elements to be included in the query.
     * @return The combined query.
     */
    private String buildQueryForGettingPermissionIdsFromString(int numberOfElements) {

        StringBuilder sb = new StringBuilder(GET_PERMISSION_ID_FROM_STRING);
        sb.append("(");
        for (int i = 0; i < numberOfElements; i++) {
            sb.append(String.format(GET_PERMISSION_ID_FROM_STRING_VALUES, i));
            if (i != numberOfElements - 1) {
                sb.append(OR);
            }
        }
        sb.append(")").append(AND).append(TENANT_ID_APPENDER).append(AND).append(UM_ACTION_APPENDER);
        return sb.toString();
    }

    /**
     * Applying the filter operation for cursor-based pagination.
     *
     * @param count    The number of elements.
     * @param operator The filter operator.
     * @return The filter.
     */
    private String applyFilterOperation(int count, FilterOperator operator) {

        return operator.applyFilterBuilder(count);
    }

    /**
     * Patch operation to replace the display name.
     *
     * @param displayName The display name of a role.
     * @param roleId      The role ID.
     * @throws OrganizationManagementServerException The server exception is thrown when an error occurs while patching
     *                                               the display name.
     */
    private void replaceDisplayName(String displayName, String roleId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ROLE_DISPLAY_NAME,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME, displayName);
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID, roleId);
                        });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_REPLACING_DISPLAY_NAME_OF_ROLE, e, displayName, roleId);
        }
    }
}
