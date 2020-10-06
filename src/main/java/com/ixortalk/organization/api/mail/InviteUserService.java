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
package com.ixortalk.organization.api.mail;

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Users;
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfo;
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.mail.invite.SendInviteMailToOrganizationVO;
import com.ixortalk.organization.api.mail.invite.TemplateVariables;
import com.ixortalk.organization.api.service.ImageMethodsService;
import com.ixortalk.organization.api.service.UserEmailProvider;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Clock;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
public class InviteUserService {

    @Inject
    private MailingService mailingService;

    @Inject
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    @Inject
    private ImageMethodsService imageMethodsService;

    @Inject
    private Clock clock;

    @Inject
    private UserEmailProvider userEmailProvider;

    @Inject
    private Auth0Users auth0Users;

    public void inviteUser(User user, Organization organization) {
        UserInfo inviterUserInfo =
                auth0Users
                        .getUserInfo(userEmailProvider.getCurrentUsersEmail().orElseThrow(() -> new IllegalStateException("email claim should be present")))
                        .orElseThrow(() -> new IllegalStateException("Logged in user should always exist"));
        user.invited(now(clock));
        mailingService.send(new SendInviteMailToOrganizationVO(
                user.getLogin(),
                ofNullable(user.getInviteLanguage()).orElse(ixorTalkConfigProperties.getOrganization().getApi().getMail().getDefaultMailLanguageTag()),
                ixorTalkConfigProperties.getOrganization().getApi().getMail().getInviteMailSubjectKey(),
                ixorTalkConfigProperties.getOrganization().getApi().getMail().getInviteMailTemplate(),
                new TemplateVariables(
                        constructInviterName(inviterUserInfo),
                        ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                        organization.getName(),
                        user,
                        imageMethodsService.constructImageLink(organization.getLogo()),
                        auth0Users.userExists(user.getLogin()),
                        user.getAcceptKey().getAcceptKey())
        ));
    }

    private static String constructInviterName(UserInfo inviterUserInfo) {
        if (!inviterUserInfo.hasFirstName() && !inviterUserInfo.hasLastName()) {
            return inviterUserInfo.getEmail();
        }

        return Stream.of(inviterUserInfo.getFirstName(), inviterUserInfo.getLastName())
                .filter(s -> !isBlank(s))
                .collect(joining(" "));
    }
}
