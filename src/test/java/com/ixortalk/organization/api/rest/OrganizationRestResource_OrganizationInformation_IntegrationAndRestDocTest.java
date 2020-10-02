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
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static com.ixortalk.organization.api.rest.docs.RestDocDescriptors.PathParameters.ORGANIZATION_ID_PATH_PARAMETER;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.setField;


public class OrganizationRestResource_OrganizationInformation_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ResponseFieldsSnippet ORGANIZATION_RESPONSE_FIELDS =
            responseFields(
                    fieldWithPath("id").type(NUMBER).description("The id of the organization"),
                    fieldWithPath("name").type(STRING).description("The organization's name"),
                    fieldWithPath("address.streetAndNumber").type(STRING).description("The street for the address (required)"),
                    fieldWithPath("address.postalCode").type(STRING).description("The postcal code for the address (required)"),
                    fieldWithPath("address.city").type(STRING).description("The city for the address (required)"),
                    fieldWithPath("address.country").type(STRING).description("The country for the address (required)"),
                    fieldWithPath("phoneNumber").type(STRING).optional().description("An (optional) phone number"),
                    fieldWithPath("emailAddress").type(STRING).optional().description("An (optional) email address"),
                    fieldWithPath("image").type(STRING).optional().description("An (optional) URL for an image for this organization"),
                    fieldWithPath("logo").type(STRING).optional().description("An (optional) URL for a logo for this organization"),
                    subsectionWithPath("_links").type(OBJECT).ignored()
            );

    @Test
    public void asAdmin() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .contentType(JSON)
                        .when()
                        .get("/organization-information/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getLong("id")).isEqualTo(organizationX.getId());
        assertThat(jsonPath.getString("name")).isEqualTo(organizationX.getName());
        assertThat(jsonPath.getString("address.streetAndNumber")).isEqualTo(organizationX.getAddress().getStreetAndNumber());
    }

    @Test
    public void asUser() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_JWT_TOKEN)
                        .contentType(JSON)
                        .filter(
                                document("organizations/organization-information/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        ORGANIZATION_RESPONSE_FIELDS,
                                        pathParameters(ORGANIZATION_ID_PATH_PARAMETER)
                                ))
                        .when()
                        .get("/organization-information/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getLong("id")).isEqualTo(organizationX.getId());
        assertThat(jsonPath.getString("name")).isEqualTo(organizationX.getName());
        assertThat(jsonPath.getString("address.streetAndNumber")).isEqualTo(organizationX.getAddress().getStreetAndNumber());
    }

    @Test
    public void cannotChangeOrganizationRepository() {

        setField(organizationX, "name", "randomNewName");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(organizationX)
                .filter(
                        document("organizations/organization-information/read-only",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        ))
                .when()
                .put("/organization-information/{id}", organizationX.getId())
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void cannotAddOrganization() {

        setField(organizationX, "name", "randomNewName");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(organizationX)
                .when()
                .post("/organization-information")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void organizationDoesNotExist() {

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .filter(
                        document("organizations/organization-information/does-not-exist",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        ))
                .when()
                .get("/organization-information/{id}", Long.MAX_VALUE)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
