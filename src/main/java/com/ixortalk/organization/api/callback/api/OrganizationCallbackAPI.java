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
package com.ixortalk.organization.api.callback.api;

import com.ixortalk.autoconfigure.oauth2.feign.ServiceToServiceFeignConfiguration;
import com.ixortalk.organization.api.rest.dto.DeviceInOrganizationDTO;
import com.ixortalk.organization.api.rest.dto.UserInOrganizationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "organization-callback-api", url = "${ixortalk.organization.callback-api.url}", configuration = ServiceToServiceFeignConfiguration.class)
public interface OrganizationCallbackAPI {

    @PostMapping(path = "${ixortalk.organization.callback-api.paths.user-accepted:/organization/user-accepted}", consumes = APPLICATION_JSON_VALUE)
    void userAccepted(@RequestBody UserInOrganizationDTO userInOrganizationDTO);

    @PostMapping(path = "${ixortalk.organization.callback-api.paths.user-removed:/organization/user-removed}", consumes = APPLICATION_JSON_VALUE)
    void userRemoved(@RequestBody UserInOrganizationDTO userInOrganizationDTO);

    @PostMapping(path = "${ixortalk.organization.callback-api.paths.device-removed:/organization/device-removed}", consumes = APPLICATION_JSON_VALUE)
    void deviceRemoved(@RequestBody DeviceInOrganizationDTO deviceInOrganizationDTO);

    @PostMapping(path = "${ixortalk.organization.callback-api.paths.organization-removed:/organization/organization-removed}")
    void organizationRemoved(@RequestParam("organizationId") Long organizationId);

    @GetMapping(path = "${ixortalk.organization.callback-api.paths.pre-delete-check:/organization/pre-delete-check}")
    ResponseEntity<?> organizationPreDeleteCheck(@RequestParam("organizationId") Long organizationId);
}
