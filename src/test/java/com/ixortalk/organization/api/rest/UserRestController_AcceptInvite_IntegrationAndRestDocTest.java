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
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.User;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.USER_ACCEPTED_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class UserRestController_AcceptInvite_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor USER_ID_PATH_PARAMETER = parameterWithName("userId").description("Primary key for the user to be accepted/declined");

    @Test
    public void accept_AsCurrentUser() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(noContent()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/accept-invite/current-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/accept-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.ACCEPTED);
    }

    @Test
    public void accept_AsCurrentUserWithAdminFlag() {
        userInOrganizationXInvited.setAdmin(true);
        userRestResource.save(userInOrganizationXInvited);

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(noContent()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/accept-invite/current-admin-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/accept-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.ACCEPTED);
    }

    @Test
    public void accept_AsDifferentUser() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/accept-invite/different-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/accept-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.INVITED);
    }

    @Test
    public void decline_AsCurrentUser() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/decline-invite/current-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/decline-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).extracting(User::getLogin).doesNotContain(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL);
        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).isNotPresent();
    }

    @Test
    public void decline_AsDifferentUser() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/decline-invite/different-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/decline-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).extracting(User::getLogin).contains(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL);
        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.INVITED);
    }

    @Test
    public void accept_userNotFound() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/accept-invite/user-not-found",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/accept-invite", Long.MAX_VALUE)
                .then()
                .statusCode(HTTP_BAD_REQUEST);
    }

    @Test
    public void decline_userNotFound() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/decline-invite/user-not-found",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/decline-invite", Long.MAX_VALUE)
                .then()
                .statusCode(HTTP_BAD_REQUEST);
    }

    @Test
    public void readOnlyStatusCheck() throws JsonProcessingException {

        setField(userInOrganizationXInvited, "status", Status.ACCEPTED);

        given()
                .auth().preemptive().oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(userInOrganizationXInvited))
                .when()
                .put("/users/{id}", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_OK);

        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.INVITED);
    }

    @Test
    public void organizationCallbackNotFound() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(notFound()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .contentType(JSON)
                .post("/users/{userId}/accept-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NOT_FOUND);

        assertThat(userRestResource.findById(userInOrganizationXInvited.getId())).get().extracting(User::getStatus).isEqualTo(Status.INVITED);
        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).extracting(User::getLogin).contains(TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL);

        verifyZeroInteractions(auth0Roles);
    }
}
