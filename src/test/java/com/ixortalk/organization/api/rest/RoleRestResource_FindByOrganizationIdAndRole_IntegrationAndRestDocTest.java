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
import com.ixortalk.organization.api.config.TestConstants;
import io.restassured.path.json.JsonPath;
import org.junit.Test;
import org.springframework.restdocs.request.ParameterDescriptor;

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class RoleRestResource_FindByOrganizationIdAndRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ParameterDescriptor ORGANIZATION_ID_REQUEST_PARAM = parameterWithName("organizationId").description("The id of the organization to get the roles from");
    private static final ParameterDescriptor ROLE_REQUEST_PARAM = parameterWithName("role").description("The rolename of (or part of) the role to be filtered on");

    @Test
    public void asAdmin() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role="+firstRoleInOrganizationX.getName())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(1).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asAdminPartOfRoleNameUnambiguous() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .filter(
                                document("roles/find-by-organization-and-role/disambiguous",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(ORGANIZATION_ID_REQUEST_PARAM, ROLE_REQUEST_PARAM))
                        )
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role=rst")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(1).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asAdminPartOfRoleNameAmbiguous() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role=role in")
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
                        .oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("roles/find-by-organization-and-role/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(ORGANIZATION_ID_REQUEST_PARAM, ROLE_REQUEST_PARAM))
                        )
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role="+firstRoleInOrganizationX.getName())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(1).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asUser() {

        given()
                .auth().preemptive()
                .oauth2(TestConstants.USER_JWT_TOKEN)
                .filter(
                        document("roles/find-by-organization-and-role/no-access-to-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                requestParameters(ORGANIZATION_ID_REQUEST_PARAM, ROLE_REQUEST_PARAM))
                )
                .when()
                .request()
                .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role="+firstRoleInOrganizationX.getName())
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
                                document("roles/find-by-organization-and-role/paging-and-sorting",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(
                                                ORGANIZATION_ID_REQUEST_PARAM,
                                                ROLE_REQUEST_PARAM,
                                                SORT_REQUEST_PARAM_DESCRIPTION,
                                                PAGE_SIZE_REQUEST_PARAM_DESCRIPTION
                                        )
                                )
                        )
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role="+firstRoleInOrganizationX.getName()+"&size=1&sort=name,desc")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.name.flatten()")).hasSize(1).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X);
        assertThat(jsonPath.getInt("page.size")).isEqualTo(1);
        assertThat(jsonPath.getInt("page.totalElements")).isEqualTo(1);
    }

    @Test
    public void organizationNotFound() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+Long.MAX_VALUE+"&role="+firstRoleInOrganizationX.getName())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.flatten()")).isEmpty();
    }

    @Test
    public void roleNotFound() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .request()
                        .get("/roles/search/findByOrganizationIdAndRole?organizationId="+organizationX.getId()+"&role=ROLE_DOES_NOT_EXIST")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("_embedded.roles.flatten()")).isEmpty();
    }
}
