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

import com.ixortalk.autoconfigure.oauth2.feign.ServiceToServiceFeignConfiguration;
import com.ixortalk.organization.api.mail.invite.SendInviteMailToOrganizationVO;
import com.ixortalk.organization.api.mail.verify.SendVerifyEmailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "mailservice",url = "${ixortalk.server.mailing-service.url}", configuration = ServiceToServiceFeignConfiguration.class, decode404 = true)
public interface MailingService {

    @PostMapping(path = "/send", consumes = APPLICATION_JSON_VALUE)
    void send(@RequestBody SendInviteMailToOrganizationVO sendInviteMailToOrganizationVO);

    @PostMapping(path = "/send", consumes = APPLICATION_JSON_VALUE)
    void send(@RequestBody SendVerifyEmailVO sendVerifyEmailVO);
}