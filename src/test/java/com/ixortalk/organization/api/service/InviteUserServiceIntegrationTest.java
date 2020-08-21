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

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfo;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.domain.Status;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.mail.InviteUserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.inject.Inject;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.domain.UserTestBuilder.aUser;
import static java.util.Optional.of;
import static org.mockito.Mockito.when;

public class InviteUserServiceIntegrationTest extends AbstractSpringIntegrationTest {

    @Inject
    private InviteUserService inviteUserService;

    @MockBean
    private UserEmailProvider userEmailProvider;

    private static final String USER_LOGIN = "test@organization";

    @Before
    public void setupUserEmailProvicer() {
        when(auth0Users.userExists(USER_LOGIN)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_LOGIN)).thenReturn(of(new UserInfo(USER_LOGIN, "test", "invite", "language")));
        when(userEmailProvider.getCurrentUsersEmail()).thenReturn(Optional.of(USER_LOGIN));
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
        User userWithInviteLanguageNl = aUser().withLogin(USER_LOGIN).withStatus(Status.CREATED).withInviteLanguage("en").build();

        // act
        inviteUserService.inviteUser(userWithInviteLanguageNl, organizationX);

        // assert
        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"en\"")));
    }

    @Test
    public void inviteUser_givenUserWithoutDefinedInviteLanguage_defaultLanguageIsUsedToSentMail() {
        // given
        User userWithoutLanguage = aUser().withLogin(USER_LOGIN).withStatus(Status.CREATED).build();

        // act
        inviteUserService.inviteUser(userWithoutLanguage, organizationX);

        // assert
        mailingServiceWireMockRule.verify(1, postRequestedFor(urlEqualTo("/mailing/send")).withRequestBody(containing("\"languageTag\":\"nl\"")));
    }
}
