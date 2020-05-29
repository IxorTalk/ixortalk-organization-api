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

import java.util.Objects;

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_WITHOUT_ROLES_JWT_TOKEN;
import static com.ixortalk.organization.api.domain.IdOnlyProjection.ID_ONLY_PROJECTION_NAME;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


public class OrganizationRestResource_GetAll_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Test
    public void asAdmin() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/get-all/as-admin",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()))
                        )
                        .when()
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations.name.flatten()")).containsOnly(ORGANIZATION_X, ORGANIZATION_Y);
    }

    @Test
    public void pagingAndSorting() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/get-all/sorting-on-organization-name",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestParameters(
                                                SORT_REQUEST_PARAM_DESCRIPTION,
                                                PAGE_SIZE_REQUEST_PARAM_DESCRIPTION)
                                )
                        )
                        .when()
                        .request()
                            .param("sort", "name,desc")
                            .param("size", "2")
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations.name.flatten()")).containsOnly(ORGANIZATION_X, ORGANIZATION_Y);
    }

    @Test
    public void roleNotExposed() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations.role.flatten()")).hasSize(2).allMatch(Objects::isNull);
    }

    @Test
    public void asUserInOrganizationXAdminRole() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                        .filter(
                                document("organizations/get-all/rbac",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()))
                        )
                        .when()
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations.name.flatten()")).containsOnly(ORGANIZATION_X);
    }

    @Test
    public void makeSureFindOneRequiresAdminAccessToOrganizationTooSinceTheBadgeAPIUsesItToEnforceAccess() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .when()
                .get("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void asUserWithoutRoles() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(USER_WITHOUT_ROLES_JWT_TOKEN)
                        .when()
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations")).isEmpty();
    }

    @Test
    public void idOnlyProjection() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .param("projection", ID_ONLY_PROJECTION_NAME)
                        .get("/organizations")
                        .then()
                        .statusCode(SC_OK)
                        .extract().response().jsonPath();

        assertThat(result.getList("_embedded.organizations.id.flatten()", Long.class)).containsOnly(organizationX.getId(), organizationY.getId());
    }
}
