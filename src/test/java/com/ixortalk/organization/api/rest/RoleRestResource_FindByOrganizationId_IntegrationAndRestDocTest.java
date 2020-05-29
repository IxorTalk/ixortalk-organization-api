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

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class RoleRestResource_FindByOrganizationId_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor ORGANIZATION_ID_REQUEST_PARAM = parameterWithName("organizationId").description("The id of the organization to get the roles from");

    @Test
    public void asAdmin() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .param("organizationId", organizationX.getId())
                        .get("/roles/search/findByOrganizationId")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(2).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void inOrganizationXAdminRole() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                        .filter(
                                document("roles/find-by-organization/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(ORGANIZATION_ID_REQUEST_PARAM))
                        )
                        .when()
                        .request()
                        .param("organizationId", organizationX.getId())
                        .get("/roles/search/findByOrganizationId")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(2).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asUser() {

        given()
                .auth().preemptive()
                .oauth2(USER_JWT_TOKEN)
                .filter(
                        document("roles/find-by-organization/no-access-to-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                requestParameters(ORGANIZATION_ID_REQUEST_PARAM))
                )
                .when()
                .request()
                .param("organizationId", organizationX.getId())
                .get("/roles/search/findByOrganizationId")
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void pagingAndSorting() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .filter(
                                document("roles/find-by-organization/paging-and-sorting",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(
                                                ORGANIZATION_ID_REQUEST_PARAM,
                                                SORT_REQUEST_PARAM_DESCRIPTION,
                                                PAGE_SIZE_REQUEST_PARAM_DESCRIPTION
                                        )
                                )
                        )
                        .when()
                        .request()
                        .param("organizationId", organizationX.getId())
                        .param("size", 1)
                        .param("sort", "name,desc")
                        .get("/roles/search/findByOrganizationId")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(1).containsOnly(SECOND_ROLE_IN_ORGANIZATION_X);
        assertThat(jsonPath.getInt("page.size")).isEqualTo(1);
        assertThat(jsonPath.getInt("page.totalElements")).isEqualTo(2);
        assertThat(jsonPath.getString("_links.next.href")).isNotNull();
    }

    @Test
    public void organizationNotFound() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .param("organizationId", Long.MAX_VALUE)
                        .get("/roles/search/findByOrganizationId")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.flatten()")).isEmpty();
    }
}
