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
import com.ixortalk.organization.api.mail.invite.SendInviteMailToOrganizationVO;
import com.ixortalk.organization.api.mail.invite.TemplateVariables;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.TestConstants.*;
import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class UserRestController_ResendInvite_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor USER_ID_PATH_PARAMETER = parameterWithName("userId").description("Primary key for the user to be accepted/declined");



    @Before
    public void before() {
        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    @Test
    public void organizationAdminResendInvite() throws JsonProcessingException {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/as-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        mailingServiceWireMockRule.verify(1,
                postRequestedFor(urlEqualTo("/mailing/send"))
                        .withRequestBody(equalToJson(
                                feignObjectMapper.writeValueAsString(
                                        new SendInviteMailToOrganizationVO(
                                                userInOrganizationXInvited.getLogin(),
                                                DEFAULT_MAIL_LANGUAGE_TAG.configValue(),
                                                INVITE_MAIL_SUBJECT_KEY.configValue(),
                                                INVITE_MAIL_TEMPLATE.configValue(),
                                                new TemplateVariables(
                                                        USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME + " " + USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME,
                                                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                                                        organizationX.getName(),
                                                        convertToHowItShouldBeSentToMailingService(userInOrganizationXInvited),
                                                        IMAGE_DOWNLOAD_URL_PREFIX + organizationX.getLogo(), true,
                                                        userRestResource.findById(userInOrganizationXInvited.getId()).get().getAcceptKey().getAcceptKey()))))));
    }

    @Test
    public void organizationAdminResendInviteForUserWithInviteLanguageEn() {
        setField(userInOrganizationXInvited, "inviteLanguage", "en");
        userRestResource.save(userInOrganizationXInvited);

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/as-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"en\"")));
    }

    @Test
    public void userResendInvite() {

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);
    }

    @Test
    public void differentOrganizationAdminResendInvite() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/as-different-organization-admin",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", userInOrganizationXInvited.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));

    }

    @Test
    public void organizationAdminResendInviteToAcceptedUser() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/accepted-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", userInOrganizationXAcceptedHavingARole.getId())
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void userDoesNotExistResendInvite() {

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/resend-invite/user-does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                pathParameters(USER_ID_PATH_PARAMETER)
                        )
                )
                .post("/users/{userId}/resend-invite", 666)
                .then()
                .statusCode(HTTP_BAD_REQUEST);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }
}
