/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.organization.api.rest;

import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.Role;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class RoleRestResource_RemoveRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Inject
    protected RoleRestResource roleRestResource;

    @Before
    public void before() {
        setField(userInOrganizationXAcceptedHavingARole, "roles", newArrayList(firstRoleInOrganizationX, secondRoleInOrganizationX));
        userRestResource.save(userInOrganizationXAcceptedHavingARole);

        when(auth0Roles.getUsersRoles(TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0, FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }
    @Test
    public void asOrganizationAdminX() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/delete-role/as-admin-from-organization-x",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(JSON)
                .delete("/roles/{id}", firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(roleRestResource.findById(firstRoleInOrganizationX.getId())).isNotPresent();

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .contains(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles).deleteRole(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);
    }

    @Test
    public void asOrganizationAdminY() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/delete-role/as-admin-from-organization-y",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(JSON)
                .delete("/roles/{id}", firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(roleRestResource.findById(firstRoleInOrganizationX.getId()))
                .get()
                .extracting(Role::getRole)
                .isEqualTo(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).deleteRole(anyString());
    }

    @Test
    public void asUser() {

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .filter(
                        document("organizations/delete-role/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(JSON)
                .delete("/roles/{id}", firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(roleRestResource.findById(firstRoleInOrganizationX.getId()))
                .get()
                .extracting(Role::getRole)
                .isEqualTo(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).deleteRole(anyString());
    }

    @Test
    public void whenRoleDoesNotExist() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/delete-role/role-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(JSON)
                .delete("/roles/{id}", Long.MAX_VALUE)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        verify(auth0Roles, never()).deleteRole(anyString());
    }
}
