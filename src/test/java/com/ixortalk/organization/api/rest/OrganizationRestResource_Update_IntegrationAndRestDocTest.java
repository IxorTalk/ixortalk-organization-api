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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.OrganizationTestBuilder;
import org.junit.Test;
import org.springframework.restdocs.payload.RequestFieldsSnippet;

import static com.ixortalk.organization.api.config.TestConstants.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.test.util.ReflectionTestUtils.setField;


public class OrganizationRestResource_Update_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final RequestFieldsSnippet ORGANIZATION_REQUEST_FIELDS =
            requestFields(
                    fieldWithPath("id").ignored(),
                    fieldWithPath("name").type(STRING).description("The organization's name"),
                    fieldWithPath("address.streetAndNumber").type(STRING).description("The street for the address (required)"),
                    fieldWithPath("address.postalCode").type(STRING).description("The postcal code for the address (required)"),
                    fieldWithPath("address.city").type(STRING).description("The city for the address (required)"),
                    fieldWithPath("address.country").type(STRING).description("The country for the address (required)"),
                    fieldWithPath("phoneNumber").type(STRING).optional().description("An (optional) phone number"),
                    fieldWithPath("emailAddress").type(STRING).optional().description("An (optional) email address"),
                    fieldWithPath("users").type(ARRAY).optional().description("An (optional) array of users (URI's) in this organization"),
                    fieldWithPath("roles").type(ARRAY).optional().description("An (optional) array of roles (URI's) in this organization"),
                    fieldWithPath("image").type(STRING).optional().description("An (optional) URL for the image to use"),
                    fieldWithPath("logo").type(STRING).optional().description("An (optional) URL for the logo to use")
            );

    private static final String MY_UPDATED_TEST_ORGANIZATION_NAME = "My updated test organization";
    private static final String NEW_EMAIL_ADDRESS = "new@ixortalk.com";

    @Test
    public void asAdmin() throws JsonProcessingException {

        setField(organizationX, "name", MY_UPDATED_TEST_ORGANIZATION_NAME);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .filter(
                        document("organizations/update/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        assertThat(organizationRestResource.findById(organizationX.getId())).get().extracting(Organization::getName).isEqualTo(MY_UPDATED_TEST_ORGANIZATION_NAME);
    }

    @Test
    public void asUserInOrganizationRole() throws JsonProcessingException {

        setField(organizationX, "name", MY_UPDATED_TEST_ORGANIZATION_NAME);

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        assertThat(organizationRestResource.findById(organizationX.getId())).get().extracting(Organization::getName).isEqualTo(MY_UPDATED_TEST_ORGANIZATION_NAME);
    }

    @Test
    public void asUserNotInOrganizationRole() throws JsonProcessingException {

        setField(organizationX, "name", MY_UPDATED_TEST_ORGANIZATION_NAME);

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .filter(
                        document("organizations/update/incorrect-role",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void nameAlreadyInUse() throws JsonProcessingException {

        organizationRestResource.save(OrganizationTestBuilder.anOrganization().withName(MY_UPDATED_TEST_ORGANIZATION_NAME).build());
        setField(organizationX, "name", MY_UPDATED_TEST_ORGANIZATION_NAME);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .filter(
                        document("organizations/update/name-already-in-use",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_CONFLICT)
                .body(not(containsString("SQL")));
    }

    @Test
    public void nameNotChanged() throws JsonProcessingException {

        setField(organizationX, "emailAddress", NEW_EMAIL_ADDRESS);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_OK);

        assertThat(organizationRestResource.findById(organizationX.getId())).get().extracting(Organization::getEmailAddress).isEqualTo(NEW_EMAIL_ADDRESS);
    }

    @Test
    public void noAccess() throws JsonProcessingException {

        setField(organizationX, "name", MY_UPDATED_TEST_ORGANIZATION_NAME);

        given()
                .auth().preemptive().oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(organizationX))
                .filter(
                        document("organizations/update/no-admin-token",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .put("/organizations/{id}", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
