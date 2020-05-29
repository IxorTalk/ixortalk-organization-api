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
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.UserTestBuilder;
import com.ixortalk.organization.api.mail.verify.SendVerifyEmailVO;
import com.ixortalk.organization.api.mail.verify.TemplateVariables;
import org.junit.Before;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.AUTH_0_DOMAIN;
import static com.ixortalk.organization.api.TestConstants.DEFAULT_MAIL_LANGUAGE_TAG;
import static com.ixortalk.organization.api.TestConstants.VERIFY_EMAIL_LANDING_PAGE_PATH;
import static com.ixortalk.organization.api.TestConstants.VERIFY_MAIL_SUBJECT_KEY;
import static com.ixortalk.organization.api.TestConstants.VERIFY_MAIL_TEMPLATE;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL;
import static com.ixortalk.organization.api.domain.OrganizationTestBuilder.anOrganization;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class VerifyEmailController_verifyEmail_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    public static final String AUTH_0_STATE_KEY = nextString("auth0StateKey");
    public static final String USER_S_FIRST_NAME = nextString("userSFirstName");
    public static final String USER_S_LAST_NAME = nextString("userSLastName");
    public static final String USER_IN_ORGANIZATION_X_INVITED_EMAIL_TICKET_URL = "userInOrganizationXInvitedEmailTicketUrl";
    public static final String USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL_TICKET_URL = "userInOrganizationXAcceptedEmailTicketUrl";
    public static final String EMAIL_PARAM_NAME = "email";
    public static final String ACCEPT_KEY_PARAM_NAME = "acceptKey";
    public static final String FIRST_NAME_PARAM_NAME = "firstName";
    public static final String LAST_NAME_PARAM_NAME = "lastName";
    public static final String STATE_PARAM_NAME = "state";

    private String acceptKey;
    private String verificationMailSentLandingPageUrl;

    @Before
    public void before() {
        userInOrganizationXInvited.invited(now(clock));
        userInOrganizationXInvited = userRestResource.save(userInOrganizationXInvited);

        this.acceptKey = (String) getField(getField(userInOrganizationXInvited, "acceptKey"), "acceptKey");

        organizationRestResource.save(
                anOrganization()
                        .withName("Other org with user")
                        .withUsers(
                                UserTestBuilder.aUser()
                                        .withLogin(USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                                        .withStatus(Status.INVITED)
                                        .build())
                        .build()
        );

        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));

        when(auth0Users
                .createEmailVerificationTicket(
                        USER_IN_ORGANIZATION_X_INVITED_EMAIL,
                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                        ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours() * 3600))
        .thenReturn(USER_IN_ORGANIZATION_X_INVITED_EMAIL_TICKET_URL);

        verificationMailSentLandingPageUrl = ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts() + VERIFY_EMAIL_LANDING_PAGE_PATH.configValue();
    }

    @Test
    public void validEmailAcceptKey() {

        given()
                .filter(
                        document("organizations/verify-email/matched",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestParameters(
                                        parameterWithName(EMAIL_PARAM_NAME).description("The registering user's used email address"),
                                        parameterWithName(ACCEPT_KEY_PARAM_NAME).description("The acceptKey passed in when authenticating, can be empty in case the user initiated the sign up himself/herself"),
                                        parameterWithName(FIRST_NAME_PARAM_NAME).description("The registering user's first name"),
                                        parameterWithName(LAST_NAME_PARAM_NAME).description("The registering user's last name"),
                                        parameterWithName(STATE_PARAM_NAME).description("State identifier provided by Auth0 to be used when redirecting back to Auth0")
                                )
                        )
                )
                .param(EMAIL_PARAM_NAME, USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                .param(ACCEPT_KEY_PARAM_NAME, acceptKey)
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_MOVED_TEMP)
                .header(LOCATION, "https://" + AUTH_0_DOMAIN.configValue() + "/continue?state=" + AUTH_0_STATE_KEY);

        verify(auth0Users).updateAppMetadata(USER_IN_ORGANIZATION_X_INVITED_EMAIL, singletonMap("acceptKeyVerified", true));
        verify(auth0Users, never()).createEmailVerificationTicket(anyString(), anyString(), anyInt());

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void invalidAcceptKey() throws JsonProcessingException {

        given()
                .filter(
                        document("organizations/verify-email/no-match",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestParameters(
                                        parameterWithName(EMAIL_PARAM_NAME).description("The registering user's used email address"),
                                        parameterWithName(ACCEPT_KEY_PARAM_NAME).description("The acceptKey passed in when authenticating, can be empty in case the user initiated the sign up himself/herself"),
                                        parameterWithName(FIRST_NAME_PARAM_NAME).description("The registering user's first name"),
                                        parameterWithName(LAST_NAME_PARAM_NAME).description("The registering user's last name"),
                                        parameterWithName(STATE_PARAM_NAME).description("State identifier provided by Auth0 to be used when redirecting back to Auth0")
                                )
                        )
                )
                .param(EMAIL_PARAM_NAME, USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                .param(ACCEPT_KEY_PARAM_NAME, "invalidKey")
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_MOVED_TEMP)
                .header(LOCATION, verificationMailSentLandingPageUrl);

        verify(auth0Users, never()).updateAppMetadata(any(), any());

        verifyVerificationEmailSent(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_INVITED_EMAIL_TICKET_URL);
    }

    @Test
    public void noAcceptKey() throws JsonProcessingException {

        given()
                .param(EMAIL_PARAM_NAME, USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                .param(ACCEPT_KEY_PARAM_NAME, "")
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_MOVED_TEMP)
                .header(LOCATION, verificationMailSentLandingPageUrl);

        verify(auth0Users, never()).updateAppMetadata(any(), any());

        verifyVerificationEmailSent(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_INVITED_EMAIL_TICKET_URL);
    }

    @Test
    public void emailAndAcceptKeyDontMatch() throws JsonProcessingException {

        when(auth0Users
                .createEmailVerificationTicket(
                        USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL,
                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                        ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours() * 3600))
                .thenReturn(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL_TICKET_URL);

        given()
                .param(EMAIL_PARAM_NAME, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)
                .param(ACCEPT_KEY_PARAM_NAME, acceptKey)
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_MOVED_TEMP)
                .header(LOCATION, verificationMailSentLandingPageUrl);

        verify(auth0Users, never()).updateAppMetadata(any(), any());

        verifyVerificationEmailSent(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL_TICKET_URL);
    }

    @Test
    public void noEmail() {

        given()
                .param(ACCEPT_KEY_PARAM_NAME, acceptKey)
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        verifyZeroInteractions(auth0Users);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void invalidEmail() {

        given()
                .param(EMAIL_PARAM_NAME, "user@.com")
                .param(ACCEPT_KEY_PARAM_NAME, acceptKey)
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        verifyZeroInteractions(auth0Users);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void expiredAcceptKey() throws JsonProcessingException {

        setField(getField(userInOrganizationXInvited, "acceptKey"), "acceptKeyTimestamp", now(clock).minus(ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours(), HOURS).minusSeconds(1));
        userRestResource.save(userInOrganizationXInvited);

        given()
                .param(EMAIL_PARAM_NAME, USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                .param(ACCEPT_KEY_PARAM_NAME, acceptKey)
                .param(FIRST_NAME_PARAM_NAME, USER_S_FIRST_NAME)
                .param(LAST_NAME_PARAM_NAME, USER_S_LAST_NAME)
                .param(STATE_PARAM_NAME, AUTH_0_STATE_KEY)
                .get("/verify-email")
                .then()
                .statusCode(HTTP_MOVED_TEMP)
                .header(LOCATION, verificationMailSentLandingPageUrl);

        verify(auth0Users, never()).updateAppMetadata(any(), any());

        verifyVerificationEmailSent(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_INVITED_EMAIL_TICKET_URL);
    }

    private void verifyVerificationEmailSent(String emailToVerify, String verifyUrl) throws JsonProcessingException {
        mailingServiceWireMockRule.verify(1,
                postRequestedFor(
                        urlEqualTo("/mailing/send"))
                        .withRequestBody(equalToJson(
                                feignObjectMapper.writeValueAsString(
                                        new SendVerifyEmailVO(
                                                emailToVerify,
                                                DEFAULT_MAIL_LANGUAGE_TAG.configValue(),
                                                VERIFY_MAIL_SUBJECT_KEY.configValue(),
                                                VERIFY_MAIL_TEMPLATE.configValue(),
                                                new TemplateVariables(
                                                        verifyUrl,
                                                        USER_S_FIRST_NAME,
                                                        USER_S_LAST_NAME
                                                )
                                        )
                                )
                                )
                        )
        );
    }
}
