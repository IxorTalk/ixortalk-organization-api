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
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.payload.RequestFieldsSnippet;

import javax.inject.Inject;

import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


public class OrganizationRestResource_Add_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

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
                    fieldWithPath("image").type(STRING).optional().description("The URL for the image to use"),
                    fieldWithPath("logo").type(STRING).optional().description("The URL for the logo to use"),
                    fieldWithPath("users").type(ARRAY).optional().description("An (optional) array of users (URI's) in this organization"),
                    fieldWithPath("roles").type(ARRAY).optional().description("An (optional) array of roles (URI's) in this organization")
            );

    private static final String EXPECTED_GENERATED_ROLE_NAME_FOR_MY_TEST_ORGANIZATION = "ROLE_MY_ORGANIZATION_ADMIN";

    private Organization myTestOrganization;

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Before
    public void before() {
        myTestOrganization =
                OrganizationTestBuilder.anOrganization()
                        .withName("My Organization")
                        .withPhoneNumber("+32 15 43 43 67")
                        .withEmailAddress("info@ixortalk.com")
                        .withImage("https://my-organization.com/image")
                        .withLogo("https://my-organization.com/logo.png")
                        .build();

        when(auth0Roles.getAllRoleNames()).thenReturn(newHashSet("ROLE_EXISTING_1", "ROLE_EXISTING_2"));
    }

    @Test
    public void asAdmin() throws JsonProcessingException {
        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(myTestOrganization))
                .when()
                .post("/organizations")
                .then()
                .statusCode(SC_CREATED);

        assertThat(restResourcesTransactionalHelper.getUsers(myTestOrganization.getName())).isEmpty();
    }

    @Test
    public void asUser() throws JsonProcessingException {
        given()
                .auth()
                .preemptive()
                .oauth2(USER_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(myTestOrganization))
                .filter(
                        document("organizations/save/no-admin-token",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .post("/organizations")
                .then()
                .statusCode(SC_CREATED);

        assertThat(organizationRestResource.findByName(myTestOrganization.getName())).isPresent();
        assertThat(restResourcesTransactionalHelper.getUsers(myTestOrganization.getName()).size()).isEqualTo(1);
        assertThat(restResourcesTransactionalHelper.getUsers(myTestOrganization.getName()).get(0).isAdmin()).isTrue();
        assertThat(restResourcesTransactionalHelper.getUsers(myTestOrganization.getName()).get(0).getLogin()).isEqualTo(USER_EMAIL);
    }

    @Test
    public void nameAlreadyInUse() throws JsonProcessingException {

        organizationRestResource.save(OrganizationTestBuilder.anOrganization().withName(myTestOrganization.getName()).build());

        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(myTestOrganization))
                .filter(
                        document("organizations/save/name-already-in-use",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                ORGANIZATION_REQUEST_FIELDS
                        ))
                .when()
                .post("/organizations")
                .then()
                .statusCode(SC_CONFLICT)
                .body(not(containsString("SQL")));

        verify(auth0Roles, never()).addRole(anyString());
    }

}
