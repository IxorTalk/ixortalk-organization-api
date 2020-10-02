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
import com.ixortalk.organization.api.domain.AcceptKey;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.mail.invite.SendInviteMailToOrganizationVO;
import com.ixortalk.organization.api.mail.invite.TemplateVariables;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfoTestBuilder.aUserInfo;
import static com.ixortalk.organization.api.TestConstants.*;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.PathParameters.ORGANIZATION_ID_PATH_PARAMETER;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.TokenHeaderDescriptors.TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.Instant.now;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class OrganizationRestController_UsersUsed_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Before
    public void before() {
        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/users-used/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                pathParameters(ORGANIZATION_ID_PATH_PARAMETER),
                                requestFields(
                                        fieldWithPath("[]").description("A list of logins that have been used")
                                )
                        ))
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin(), userInOrganizationXInvited.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        AcceptKey acceptKey = userRestResource.findById(userInOrganizationXCreated.getId()).map(User::getAcceptKey).orElseThrow(() -> new IllegalStateException("User should be present"));
        assertThat(acceptKey).isNotNull();
        assertThat(acceptKey.getAcceptKey()).isNotNull();
        assertThat((Instant) getField(acceptKey, "acceptKeyTimestamp")).isEqualTo(now(clock));

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(equalToJson(
                                feignObjectMapper.writeValueAsString(
                                        new SendInviteMailToOrganizationVO(
                                                userInOrganizationXCreated.getLogin(),
                                                DEFAULT_MAIL_LANGUAGE_TAG.configValue(),
                                                INVITE_MAIL_SUBJECT_KEY.configValue(),
                                                INVITE_MAIL_TEMPLATE.configValue(),
                                                new TemplateVariables(
                                                        USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME + " " + USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME,
                                                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                                                        organizationX.getName(),
                                                        convertToHowItShouldBeSentToMailingService(userInOrganizationXCreated),
                                                        IMAGE_DOWNLOAD_URL_PREFIX +  organizationX.getLogo(),
                                                        true,
                                                        acceptKey.getAcceptKey()
                                                        ))))));

    }

    @Test
    public void asAdminForUserWithInviteLanguageEn() throws JsonProcessingException {
        setField(userInOrganizationXCreated, "inviteLanguage", "en");
        userRestResource.save(userInOrganizationXCreated);

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/users-used/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                pathParameters(ORGANIZATION_ID_PATH_PARAMETER),
                                requestFields(
                                        fieldWithPath("[]").description("A list of logins that have been used")
                                )
                        ))
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"en\"")));
    }

    @Test
    public void asUser() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .filter(
                        document("organizations/users-used/not-an-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES),
                                pathParameters(ORGANIZATION_ID_PATH_PARAMETER),
                                requestFields(
                                        fieldWithPath("[]").description("An list of logins that have been used")
                                )
                        ))
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void whenOrganizationDoesNotExist() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", Long.MAX_VALUE)
                .then()
                .statusCode(SC_FORBIDDEN);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void whenUserDoesNotExist() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(Long.MAX_VALUE)))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void whenUserIsAlreadyInvited() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXInvited.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void whenUserHasAlreadyAccepted() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXAcceptedHavingARole.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void whenUserDoesNotExistInAuth0() throws JsonProcessingException {

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_CREATED_EMAIL)).thenReturn(false);

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(equalToJson(
                                feignObjectMapper.writeValueAsString(
                                        new SendInviteMailToOrganizationVO(
                                                userInOrganizationXCreated.getLogin(),
                                                DEFAULT_MAIL_LANGUAGE_TAG.configValue(),
                                                INVITE_MAIL_SUBJECT_KEY.configValue(),
                                                INVITE_MAIL_TEMPLATE.configValue(),
                                                new TemplateVariables(
                                                        USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME + " " + USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME,
                                                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                                                        organizationX.getName(),
                                                        convertToHowItShouldBeSentToMailingService(userInOrganizationXCreated),
                                                        IMAGE_DOWNLOAD_URL_PREFIX + organizationX.getLogo(),
                                                        false,
                                                        userRestResource.findById(userInOrganizationXCreated.getId()).map(User::getAcceptKey).orElseThrow(() -> new IllegalStateException("User should be present")).getAcceptKey()))))));
    }

    @Test
    public void whenUserIsNotInOrganization() throws JsonProcessingException {

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationY.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void inviterHasNoFirstName() throws JsonProcessingException {

        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(of(aUserInfo().withEmail(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL).withFirstName(null).withLastName(USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME).build()));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(
                                matchingJsonPath("$.additionalVariables.organizationInviter.name", equalTo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME))));
    }

    @Test
    public void inviterHasNoLastName() throws JsonProcessingException {

        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(of(aUserInfo().withEmail(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL).withFirstName(USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME).withLastName(null).build()));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(
                                matchingJsonPath("$.additionalVariables.organizationInviter.name", equalTo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME))));
    }

    @Test
    public void inviterHasNeitherFirstNorLastName() throws JsonProcessingException {

        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(of(aUserInfo().withEmail(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL).withFirstName(null).withLastName(null).build()));

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(newArrayList(userInOrganizationXCreated.getLogin())))
                .post("/organizations/{id}/users/used", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(
                                matchingJsonPath("$.additionalVariables.organizationInviter.name", equalTo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL))));
    }
}
