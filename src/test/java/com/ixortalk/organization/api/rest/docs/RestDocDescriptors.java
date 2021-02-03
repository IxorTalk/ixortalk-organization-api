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
package com.ixortalk.organization.api.rest.docs;

import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

public class RestDocDescriptors {

    public static class TokenHeaderDescriptors {
        public static final HeaderDescriptor TOKEN_WITH_ORGANIZATION_ADMIN_PRIVILEGES = headerWithName(AUTHORIZATION).description("The bearer token needed to authorize this request.  Organization administrator privileges required for this call.");
        public static final HeaderDescriptor TOKEN_WITH_USER_PRIVILEGES = headerWithName(AUTHORIZATION).description("The bearer token needed to authorize this request.  A bearer token with user privileges.");
    }

    public static class PathParameters {
        public static final ParameterDescriptor ORGANIZATION_ID_PATH_PARAMETER = parameterWithName("id").description("The id of the organization.");
    }
}
