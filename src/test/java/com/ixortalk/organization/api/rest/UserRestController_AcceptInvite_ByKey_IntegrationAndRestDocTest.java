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
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.RoleTestBuilder;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.dto.UserInOrganizationDTO;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.USER_ACCEPTED_CALLBACK_PATH;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class UserRestController_AcceptInvite_ByKey_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor USER_ID_PATH_PARAMETER = parameterWithName("userId").description("Primary key for the user to be accepted/declined");
    private static final ParameterDescriptor ACCEPT_KEY_PATH_PARAMETER = parameterWithName("acceptKey").description("Accept key being provided to the user to accept it's invitation");

    private static final String ROLE_TO_BE_ASSIGNED_AFTER_ACCEPT = "ROLE_TO_BE_ASSGND_AFTER_ACC";

    private String acceptKey;

    @Before
    public void before() {
        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(noContent()));

        userInOrganizationXInvited.invited(now(clock));
        userInOrganizationXInvited.getRoles().add(RoleTestBuilder.aRole().withRole(ROLE_TO_BE_ASSIGNED_AFTER_ACCEPT).build());
        userInOrganizationXInvited = userRestResource.save(userInOrganizationXInvited);

        this.acceptKey = (String) getField(getField(userInOrganizationXInvited, "acceptKey"), "acceptKey");
    }

    @Test
    public void ok() throws JsonProcessingException {

        given()
                .filter(
                        document("organizations/accept-by-key/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(USER_ID_PATH_PARAMETER, ACCEPT_KEY_PATH_PARAMETER)
                        )
                )
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXInvited.getId(), acceptKey)
                .then()
                .statusCode(HTTP_NO_CONTENT);

        User actual = userRestResource.findById(userInOrganizationXInvited.getId()).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(actual.isAccepted()).isTrue();
        assertThat(getField(actual, "acceptKey")).isNull();

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(new UserInOrganizationDTO(userInOrganizationXInvited.getLogin(), organizationX.getId())))));
        verify(auth0Users).unblockUser(userInOrganizationXInvited.getLogin());
        verify(auth0Roles).assignRolesToUser(userInOrganizationXInvited.getLogin(), newHashSet(ROLE_TO_BE_ASSIGNED_AFTER_ACCEPT));
    }

    @Test
    public void alreadyAccepted() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXAcceptedHavingARole.getId(), "anOldAcceptKeyNotValidAnymoreAtThisPoint")
                .then()
                .statusCode(HTTP_NO_CONTENT);

        User actual = userRestResource.findById(userInOrganizationXAcceptedHavingARole.getId()).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(actual.isAccepted()).isTrue();
        assertThat(getField(actual, "acceptKey")).isNull();

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue())));
        verifyZeroInteractions(auth0Roles);
    }

    @Test
    public void keysDontExpire() throws JsonProcessingException {
        setField(getField(userInOrganizationXInvited, "acceptKey"), "acceptKeyTimestamp", now(clock).minus(ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours(), HOURS).minusSeconds(1));
        userRestResource.save(userInOrganizationXInvited);

        given()
                .filter(
                        document("organizations/accept-by-key/expired-key",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(USER_ID_PATH_PARAMETER, ACCEPT_KEY_PATH_PARAMETER)
                        )
                )
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXInvited.getId(), acceptKey)
                .then()
                .statusCode(HTTP_NO_CONTENT);

        User actual = userRestResource.findById(userInOrganizationXInvited.getId()).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(actual.isAccepted()).isTrue();
        assertThat(getField(actual, "acceptKey")).isNull();

        organizationCallbackApiWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                        .andMatching(retrievedAdminTokenAuthorizationHeader())
                        .withRequestBody(equalToJson(feignObjectMapper.writeValueAsString(new UserInOrganizationDTO(userInOrganizationXInvited.getLogin(), organizationX.getId())))));
        verify(auth0Users).unblockUser(userInOrganizationXInvited.getLogin());
        verify(auth0Roles).assignRolesToUser(userInOrganizationXInvited.getLogin(), newHashSet(ROLE_TO_BE_ASSIGNED_AFTER_ACCEPT));
    }

    @Test
    public void invalidKey() {

        given()
                .filter(
                        document("organizations/accept-by-key/invalid-key",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(USER_ID_PATH_PARAMETER, ACCEPT_KEY_PATH_PARAMETER)
                        )
                )
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXInvited.getId(), "incorrect-key")
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue())));
        verifyZeroInteractions(auth0Roles);
    }

    @Test
    public void userNotFound() {

        given()
                .filter(
                        document("organizations/accept-invite/user-not-found",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(USER_ID_PATH_PARAMETER, ACCEPT_KEY_PATH_PARAMETER)
                        )
                )
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", Long.MAX_VALUE, acceptKey)
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue())));
        verifyZeroInteractions(auth0Roles);
    }

    @Test
    public void notAuthorized() {

        given()
                .filter(
                        document("organizations/accept-by-key/not-authorized",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(USER_ID_PATH_PARAMETER, ACCEPT_KEY_PATH_PARAMETER)
                        )
                )
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXInvited.getId(), acceptKey)
                .then()
                .statusCode(HTTP_FORBIDDEN);

        User actual = userRestResource.findById(userInOrganizationXInvited.getId()).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(actual.isAccepted()).isFalse();
        assertThat(actual.getAcceptKey().getAcceptKey()).isEqualTo(acceptKey);

        organizationCallbackApiWireMockRule.verify(0, postRequestedFor(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue())));
        verifyZeroInteractions(auth0Roles);
    }

    @Test
    public void organizationCallbackNotFound() {

        organizationCallbackApiWireMockRule.stubFor(post(urlEqualTo("/org-callback-api" + USER_ACCEPTED_CALLBACK_PATH.configValue()))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(notFound()));

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                .post("/users/{userId}/{acceptKey}/accept-invite", userInOrganizationXInvited.getId(), acceptKey)
                .then()
                .statusCode(HTTP_NOT_FOUND);

        User actual = userRestResource.findById(userInOrganizationXInvited.getId()).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(actual.isAccepted()).isFalse();
        assertThat(actual.getAcceptKey().getAcceptKey()).isEqualTo(acceptKey);

        verifyZeroInteractions(auth0Roles);
    }
}
