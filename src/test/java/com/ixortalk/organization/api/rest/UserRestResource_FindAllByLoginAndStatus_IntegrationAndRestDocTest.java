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

import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import io.restassured.path.json.JsonPath;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class UserRestResource_FindAllByLoginAndStatus_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor LOGIN_REQUEST_PARAM = parameterWithName("login").description("The login of the user");
    private static final ParameterDescriptor STATUS_REQUEST_PARAM = parameterWithName("status").description("The status of the user's invitation");

    @Test
    public void asAdmin() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .param("login", userInOrganizationXInvited.getLogin())
                        .param("status", userInOrganizationXInvited.getStatus())
                        .get("/users/search/findAllByLoginAndStatus")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.users.login.flatten()")).hasSize(1).containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL);
    }

    @Test
    public void asUser() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                        .filter(
                                document("users/find-all-by-login-and-status/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(LOGIN_REQUEST_PARAM, STATUS_REQUEST_PARAM))
                        )
                        .when()
                        .request()
                        .param("login", userInOrganizationXInvited.getLogin())
                        .param("status", userInOrganizationXInvited.getStatus())
                        .get("/users/search/findAllByLoginAndStatus")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.users.login.flatten()")).hasSize(1).containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL);
    }

    @Test
    public void asOtherUser() {

        given()
                .auth().preemptive()
                .oauth2(OTHER_USER_JWT_TOKEN)
                .filter(
                        document("users/find-all-by-login-and-status/access-denied",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                requestParameters(LOGIN_REQUEST_PARAM, STATUS_REQUEST_PARAM))
                )
                .when()
                .request()
                .param("login", userInOrganizationXInvited.getLogin())
                .param("status", userInOrganizationXInvited.getStatus())
                .get("/users/search/findAllByLoginAndStatus")
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void pagingAndSorting() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                        .filter(
                                document("users/find-all-by-login-and-status/paging-and-sorting",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(
                                                LOGIN_REQUEST_PARAM,
                                                STATUS_REQUEST_PARAM,
                                                SORT_REQUEST_PARAM_DESCRIPTION,
                                                PAGE_SIZE_REQUEST_PARAM_DESCRIPTION
                                        )
                                )
                        )
                        .when()
                        .request()
                        .param("login", userInOrganizationXInvited.getLogin())
                        .param("status", userInOrganizationXInvited.getStatus())
                        .param("size", 1)
                        .param("sort", "login,desc")
                        .get("/users/search/findAllByLoginAndStatus")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.users.login.flatten()")).hasSize(1).containsOnly(USER_IN_ORGANIZATION_X_INVITED_EMAIL);
        assertThat(jsonPath.getInt("page.size")).isEqualTo(1);
        assertThat(jsonPath.getInt("page.totalElements")).isEqualTo(1);
    }

    @Test
    public void statusNotFound() {
            given()
                    .auth().preemptive()
                    .oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                    .when()
                    .request()
                    .param("login", userInOrganizationXInvited.getLogin())
                    .param("status", "NOT_EXISTENT_STATUS")
                    .get("/users/search/findAllByLoginAndStatus")
                    .then()
                    .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void loginNotFound() {

            given()
                    .auth().preemptive()
                    .oauth2(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)
                    .when()
                    .request()
                    .param("login", "randomLogin")
                    .param("status", userInOrganizationXInvited.getStatus())
                    .get("/users/search/findAllByLoginAndStatus")
                    .then()
                    .statusCode(SC_FORBIDDEN);
    }
}
