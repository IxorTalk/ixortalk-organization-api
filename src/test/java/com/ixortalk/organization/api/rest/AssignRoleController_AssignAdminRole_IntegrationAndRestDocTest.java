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

import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import org.junit.Test;
import org.springframework.restdocs.request.PathParametersSnippet;

import static com.google.common.collect.Sets.newHashSet;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class AssignRoleController_AssignAdminRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {


    private static final PathParametersSnippet PATH_PARAMETERS_SNIPPET = pathParameters(
            parameterWithName("userId").description("The id (primary key) for the user to assign the role to"),
            parameterWithName("organizationId").description("The id for the the organization in which the user and role are linked, this is to retrieve the organization admin role")
    );

    @Test
    public void asAdmin() {

        given()
                .auth().preemptive().oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        verify(auth0Roles).assignRolesToUser(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL, newHashSet(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void asUserInOrganizationXAdminRole() {


        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        verify(auth0Roles).assignRolesToUser(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL, newHashSet(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void asUserInOrganizationYAdminRole() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/admin-of-org-y",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void asUserInOrganizationXUserRole() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/as-user-of-org-x",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void organizationDoesNotExist() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", 666, userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void userDoesNotExist() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/user-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), 666)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());

    }

    @Test
    public void userNotFoundInAuth0() {

        when(auth0Users.userExists(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(false);

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/assign-admin-role/organization-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .contentType(JSON)
                .post("/{organizationId}/{userId}/assign-admin-role", organizationX.getId(), userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }
}
