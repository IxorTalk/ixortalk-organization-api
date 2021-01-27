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

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Users;
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.mail.MailingService;
import com.ixortalk.organization.api.mail.verify.SendVerifyEmailVO;
import com.ixortalk.organization.api.mail.verify.TemplateVariables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonMap;
import static org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory.HTTPS_SCHEME;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.newInstance;

@RestController
@Validated
public class VerifyEmailController {

    @Inject
    private Auth0Users auth0Users;

    @Inject
    private UserRestResource userRestResource;

    @Value("${ixortalk.auth0.domain}")
    private String auth0Domain;

    @Inject
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    @Inject
    private Clock clock;

    @Inject
    private MailingService mailingService;

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(HttpServletResponse response, @RequestParam String state, @RequestParam String userId, @RequestParam @NotEmpty @Email String email, @RequestParam String acceptKey, @RequestParam String firstName, @RequestParam String lastName) throws IOException {

        Optional<User> user =
                userRestResource
                        .findByLoginAndAcceptKeyAcceptKeyAndAcceptKeyAcceptKeyTimestampAfter(
                                email,
                                acceptKey,
                                now(clock).minus(ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours(), HOURS));
        if (user.isPresent()) {
            auth0Users.updateAppMetadata(email, singletonMap("acceptKeyVerified", true));
            response.sendRedirect(
                    newInstance()
                            .scheme(HTTPS_SCHEME)
                            .host(auth0Domain)
                            .path("/continue")
                            .queryParam("state", state)
                            .toUriString());
        } else {
            mailingService.send(new SendVerifyEmailVO(
                    email,
                    ixorTalkConfigProperties.getOrganization().getApi().getMail().getDefaultMailLanguageTag(),
                    ixorTalkConfigProperties.getOrganization().getApi().getMail().getVerifyMailSubjectKey(),
                    ixorTalkConfigProperties.getOrganization().getApi().getMail().getVerifyMailTemplate(),
                    new TemplateVariables(
                            auth0Users.createEmailVerificationTicket(
                                    userId,
                                    ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts(),
                                    ixorTalkConfigProperties.getOrganization().getApi().getAcceptKeyMaxAgeInHours() * 3600),
                            firstName,
                            lastName)
            ));
            response.sendRedirect(
                            ixorTalkConfigProperties.getLoadbalancer().getExternal().getUrlWithoutStandardPorts() + ixorTalkConfigProperties.getOrganization().getApi().getVerifyEmailLandingPagePath()
            );
        }
        return ok("Unable to redirect automatically");
    }
}
