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
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationRestResource_GetRolesInOrganization_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ResponseFieldsSnippet RESPONSE_FIELDS_SNIPPET =
            responseFields(
                    fieldWithPath("_embedded.roles[].id").type(NUMBER).description("The primary key for the role in this organization"),
                    fieldWithPath("_embedded.roles[].name").type(STRING).description("The name for the role, this is a pure functional name, not the technical role name."),
                    subsectionWithPath("_embedded.roles[]._links").ignored(),
                    fieldWithPath("_embedded.roles[].role").type(STRING).description("The technical name of the role."),
                    subsectionWithPath("_links").ignored()
            );

    @Test
    public void asAdmin() {

        JsonPath result =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}/roles", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(result.getList("_embedded.roles.name.flatten()")).hasSize(2).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asUserInOrganizationXAdminRole() {

        JsonPath result =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                        .filter(
                                document("organizations/roles/get-roles-in-org/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        RESPONSE_FIELDS_SNIPPET)
                        )
                        .when()
                        .get("/organizations/{id}/roles", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(result.getList("_embedded.roles.name.flatten()")).hasSize(2).containsOnly(FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asUserNotInOrganizationXAdminRole() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/roles/get-roles-in-org/no-access-to-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()))
                )
                .when()
                .get("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
