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
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.util.RestResourcesTransactionalHelper;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.springframework.restdocs.request.PathParametersSnippet;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static com.ixortalk.organization.api.domain.EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class UserRestResource_LinkRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Inject
    private RestResourcesTransactionalHelper restResourcesTransactionalHelper;

    private static final PathParametersSnippet PATH_PARAMETERS_SNIPPET = pathParameters(
            parameterWithName("userId").description("The id (primary key) for the user to link the role to.")
    );

    @Test
    public void linkRoleAsAdminUserAlreadyAccepted() {

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles).assignRolesToUser(TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void linkRoleAsOrganizationAdmin() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/link-role/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles).assignRolesToUser(TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));
    }

    @Test
    public void linkRoleAsUser() {

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/link-role/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void linkRoleWhenRoleDoesNotExist() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/link-role/role-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .body("/organization/roles/" + Long.MAX_VALUE)
                .post("/users/{userId}/roles", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void linkRoleWhenUserDoesNotExist() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .filter(
                        document("organizations/link-role/user-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))
                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", Long.MAX_VALUE)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void linkRoleWhenUserHasNotAccepted() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXInvited.getId()))
                .hasSize(1)
                .extracting(Role::getRole)
                .containsExactly(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }

    @Test
    public void userInfoIsNotVisibleWhenUserHasNotAccepted() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .get("/users/" + userInOrganizationXInvited.getId() + "/?projection=" + ENHANCED_USER_PROJECTION_NAME)
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract().jsonPath();

        assertThat(result.getMap("userInfo")).isNull();
    }

    @Test
    public void userInfoIsVisibleWhenUserHasAccepted() {

        JsonPath result = given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .get("/users/" + userInOrganizationXAcceptedHavingARole.getId() + "/?projection=" + ENHANCED_USER_PROJECTION_NAME)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().jsonPath();

        assertThat(result.getMap("userInfo")).containsValues(USER_IN_ORGANIZATION_X_ACCEPTED_FIRST_NAME, USER_IN_ORGANIZATION_X_ACCEPTED_LAST_NAME);
    }

    @Test
    public void userInfoIsNotVisibleWhenDifferentOrganizationRequestsInfo() {

        Long userId = organizationX.getUsers()
                .stream()
                .filter(user -> user.getLogin().equals(USER_IN_ORGANIZATION_X_AND_Y_EMAIL))
                .findAny()
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User should be present!"));
        JsonPath result = given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .get("/users/" + userId + "/?projection=" + ENHANCED_USER_PROJECTION_NAME)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().jsonPath();

        assertThat(result.getMap("userInfo")).isNull();
    }

    @Test
    public void whenRoleIsAlreadyAdded() {

        userInOrganizationXAcceptedHavingARole.getRoles().add(firstRoleInOrganizationX);
        userRestResource.save(userInOrganizationXAcceptedHavingARole);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .when()
                .filter(
                        document("organizations/link-role/role-is-already-added",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        ))

                .body("/organization/roles/" + firstRoleInOrganizationX.getId())
                .post("/users/{userId}/roles", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_CONFLICT);

        assertThat(restResourcesTransactionalHelper.getRolesOfUser(userInOrganizationXAcceptedHavingARole.getId()))
                .hasSize(2)
                .extracting(Role::getRole)
                .contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);

        verify(auth0Roles, never()).assignRolesToUser(anyString(), anySet());
    }
}