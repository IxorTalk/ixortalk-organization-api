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

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfo;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.UserTestBuilder;
import org.junit.Before;
import org.junit.Test;

import static com.ixortalk.organization.api.domain.EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME;
import static io.restassured.RestAssured.given;
import static java.util.Optional.of;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class UserRestResource_GetUsers_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Before
    public void before() {
        when(auth0Users.getUserInfo(anyString())).thenReturn(of(new UserInfo("some", "user", "info", "object")));
    }

    @Test
    public void validateProjectionNotBroken() {

        userRestResource.save(UserTestBuilder.aUser().build());

        given()
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .when()
                .get("/users?projection=" + ENHANCED_USER_PROJECTION_NAME)
                .then()
                .statusCode(SC_OK);
    }
}
