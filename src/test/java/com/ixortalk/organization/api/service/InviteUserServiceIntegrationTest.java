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
package com.ixortalk.organization.api.service;

import com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.OrganizationApiApplication;
import com.ixortalk.organization.api.mail.InviteUserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.inject.Inject;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
        classes = {OrganizationApiApplication.class, OAuth2TestConfiguration.class},
        webEnvironment = RANDOM_PORT,
        properties = {
                "ixortalk.organization.api.mail.default-mail-language-tag=en",
        }
)
public class InviteUserServiceIntegrationTest extends AbstractSpringIntegrationTest {

    @Inject
    private InviteUserService inviteUserService;

    @MockBean
    private UserEmailProvider userEmailProvider;

    @Before
    public void setupUserEmailProvicer() {
        Mockito.when(userEmailProvider.getCurrentUsersEmail()).thenReturn(Optional.of(USER_IN_ORGANIZATION_X_CREATED_EMAIL));
    }

    @Before
    public void setupMailServiceWireMockRule() {
        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(ok()));
    }

    @Test
    public void inviteUser_givenUserWithDefinedInviteLanguage_languageIsUsedToSentMail() {
        // given
        userInOrganizationXCreated.setInviteLanguage("nl");

        // act
        inviteUserService.inviteUser(userInOrganizationXCreated, organizationX);

        // assert
        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"nl\"")));
    }

    @Test
    public void inviteUser_givenUserWithoutDefinedInviteLanguage_defaultLanguageIsUsedToSentMail() {
        // act
        inviteUserService.inviteUser(userInOrganizationXCreated, organizationX);

        // assert
        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"en\"")));
    }
}
