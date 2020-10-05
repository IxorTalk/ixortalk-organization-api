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
import com.ixortalk.organization.api.util.RestResourcesTransactionalHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.PathParametersSnippet;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class UserRestResource_UnlinkRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Inject
    private RestResourcesTransactionalHelper restResourcesTransactionalHelper;

    private static final PathParametersSnippet PATH_PARAMETERS_SNIPPET = pathParameters(
            parameterWithName("userId").description("The id (primary key) for the user to unlink the role from."),
            parameterWithName("roleId").description("The id of the role to be unlinked.")
    );

    @Before
    public void before() {

        userInOrganizationXAcceptedHavingARole.getRoles().add(firstRoleInOrganizationX);
        userRestResource.save(userInOrganizationXAcceptedHavingARole);

        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0, FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void unlinkRoleAsAdmin() {

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXAcceptedHavingARole.getId(), firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId()))
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles).removeRolesFromUser(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void unlinkRoleAsOrganizationAdmin() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/unlink-role/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXAcceptedHavingARole.getId(), firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId()))
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles).removeRolesFromUser(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void unlinkRoleAsUser() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/unlink-role/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXAcceptedHavingARole.getId(), firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);


        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySetOf(String.class));
    }

    @Test
    public void unlinkRoleWhenUserDoesNotExist() {

        assertThat(userRestResource.findById(Long.MAX_VALUE)).isNotPresent();

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/unlink-role/user-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .delete("/users/{userId}/roles/{roleId}", Long.MAX_VALUE, firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySetOf(String.class));
    }

    @Test
    public void unlinkRoleWhenRoleDoesNotExist() {

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/unlink-role/role-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXAcceptedHavingARole.getId(), Long.MAX_VALUE)
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySetOf(String.class));
    }

    @Test
    public void unlinkRoleWhenUserDoesNotExistInAuth0() {

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(false);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXAcceptedHavingARole.getId(), firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .contains(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySetOf(String.class));
    }

    @Test
    public void unlinkRoleWhenUserHasNotAccepted() {

        when(auth0Roles.getUsersRoles(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0, FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .delete("/users/{userId}/roles/{roleId}", userInOrganizationXInvited.getId(), firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXInvited.getId())).isEmpty();
        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySetOf(String.class));
    }
}