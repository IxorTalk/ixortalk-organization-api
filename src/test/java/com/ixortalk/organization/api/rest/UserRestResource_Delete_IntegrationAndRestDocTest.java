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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.dto.UserInOrganizationDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.PathParametersSnippet;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.USER_REMOVED_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class UserRestResource_Delete_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final PathParametersSnippet PATH_PARAMETERS_SNIPPET = pathParameters(
            parameterWithName("userId").description("The id of the user to delete.")
    );

    private UserInOrganizationDTO userInOrganizationDTO;

    @Before
    public void before() {

        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0, FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME, ROLE_IN_ORGANIZATION_Y_ROLE_NAME));

        userInOrganizationDTO = new UserInOrganizationDTO(userInOrganizationXInvited.getLogin(), organizationX.getId());

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers - 1)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles).removeRolesFromUser(USER_IN_ORGANIZATION_X_INVITED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(userInOrganizationDTO))));
    }

    @Test
    public void asOrganizationAdminX() throws JsonProcessingException {
        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers - 1)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles).removeRolesFromUser(USER_IN_ORGANIZATION_X_INVITED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(userInOrganizationDTO))));
    }

    @Test
    public void asOrganizationAdminY() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/different-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void asUserFromOrganizationX() {

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void asOrganizationAdminXButUserNotInOrganization() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/user-not-in-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationY.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers)
                .extracting(User::getLogin)
                .contains(USER_IN_ORGANIZATION_X_INVITED_EMAIL);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationY.getId()))
                .hasSize(organizationYInitialNumberOfUsers)
                .extracting(User::getLogin)
                .contains(USER_IN_ORGANIZATION_Y_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void userDoesNotExist() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/user-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", 666)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void userNotFoundInAuth0() throws JsonProcessingException {

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(false);

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers - 1)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(userInOrganizationDTO))));
    }

    @Test
    public void whenOrganizationCallbackFails() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(serverError()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_INTERNAL_ERROR);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).hasSize(organizationXInitialNumberOfUsers).extracting(User::getLogin).contains(USER_IN_ORGANIZATION_X_INVITED_EMAIL);
    }

    @Test
    public void whenOrganizationCallbackNotFound() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(notFound()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).hasSize(organizationXInitialNumberOfUsers).extracting(User::getLogin).contains(USER_IN_ORGANIZATION_X_INVITED_EMAIL);
    }

    @Test
    public void asCurrentUser() throws JsonProcessingException {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/self",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers - 1)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles).removeRolesFromUser(USER_IN_ORGANIZATION_X_INVITED_EMAIL, newHashSet(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(userInOrganizationDTO))));
    }

    @Test
    public void asCurrentUserButNotExistAnymore() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/self-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", 666)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers)
                .extracting(User::getLogin)
                .containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }

    @Test
    public void asCurrentUserButNotInOrganizationAnymore() {

        organizationX.removeUser(userInOrganizationXInvited);
        organizationRestResource.save(organizationX);

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .when()
                .filter(
                        document("organizations/delete-user/self-not-in-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                PATH_PARAMETERS_SNIPPET
                        )
                )
                .contentType(JSON)
                .delete("/users/{userId}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        verify(auth0Roles, never()).removeRolesFromUser(anyString(), anySet());

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_REMOVED_CALLBACK_PATH.configValue())));
    }
}
