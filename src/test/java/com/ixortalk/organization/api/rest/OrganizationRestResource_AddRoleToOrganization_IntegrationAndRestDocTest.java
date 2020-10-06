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
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.OrganizationTestBuilder;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.RoleTestBuilder;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.*;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationRestResource_AddRoleToOrganization_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final String NEW_ROLE = nextString("newRole");

    @Before
    public void before() {
        when(auth0Roles.getAllRoleNames()).thenReturn(newHashSet("ROLE_EXISTING_1", "ROLE_EXISTING_2"));
    }

    @Test
    public void asAdmin() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId()))
                .hasSize(3)
                .extracting(Role::getName)
                .containsOnly(NEW_ROLE, FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void nameAlreadyInUse_SameOrganization() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + FIRST_ROLE_IN_ORGANIZATION_X + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_CONFLICT)
                .body(not(containsString("SQL")));
    }

    @Test
    public void nameAlreadyInUse_DifferentOrganization() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + ROLE_IN_ORGANIZATION_Y + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId()))
                .hasSize(3)
                .extracting(Role::getName)
                .containsOnly(ROLE_IN_ORGANIZATION_Y, FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void roleCreatedInAuth0() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        String roleId = substringAfterLast(createdRoleURI, "/");

        verify(auth0Roles).addRole("ROLE_ORGANIZATION_X_" + roleId);
    }

    @Test
    public void generatedRoleNameAlreadyExists() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        String roleId = substringAfterLast(createdRoleURI, "/");
        organizationRestResource.save(OrganizationTestBuilder.anOrganization().withRoles(RoleTestBuilder.aRole().withRole("ROLE_ORGANIZATION_X_" + roleId).build()).build());

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-role/link/conflict",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_CONFLICT)
                .body(not(containsString("SQL")));
    }

    @Test
    public void generatedRoleNameAlreadyExistsInAuth0() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        String roleId = substringAfterLast(createdRoleURI, "/");
        when(auth0Roles.getAllRoleNames()).thenReturn(newHashSet("ROLE_EXISTING_1", "ROLE_EXISTING_2", "ROLE_ORGANIZATION_X_" + roleId));

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_CONFLICT)
                .body(not(containsString("SQL")));
    }

    @Test
    public void asUserInOrganizationXAdminRole() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/add-role/add/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestFields(
                                                fieldWithPath("name").type(STRING).description("The name for the role to add, this is a pure functional name, not the technical role name (which will be generated behind the scenes).")
                                        )
                                )
                        )
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-role/link/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId()))
                .hasSize(3)
                .extracting(Role::getName)
                .containsOnly(NEW_ROLE, FIRST_ROLE_IN_ORGANIZATION_X, SECOND_ROLE_IN_ORGANIZATION_X);
    }

    @Test
    public void asUserInNotOrganizationXAdminRole() {

        String createdRoleURI =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/add-role/add/no-access-to-organization",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestFields(
                                                fieldWithPath("name").type(STRING).description("The name for the role to add, this is a pure functional name, not the technical role name (which will be generated behind the scenes).")
                                        )
                                )
                        )
                        .when()
                        .contentType(JSON)
                        .body("{ \"name\": \"" + NEW_ROLE + "\" }")
                        .post("/roles")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-role/link/no-access-to-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdRoleURI)
                .post("/organizations/{id}/roles", organizationX.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getRoles(organizationX.getId())).hasSize(2).extracting(Role::getName).doesNotContain(NEW_ROLE);
    }

    @Test
    public void savingRoleFailsWhenAlreadyLinkedToAnOrganizationWhereTheUserHasNoAdminRole() {

        Organization otherOrganization =
                organizationRestResource.save(
                        OrganizationTestBuilder.anOrganization()
                                .withRoles(RoleTestBuilder.aRole().withName("existingRole").build())
                                .build()
                );

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .body("{ \"name\": \"someEvilRoleName\" }")
                .put("/roles/{id}", restResourcesTransactionalHelper.getRoles(otherOrganization.getId()).get(0).getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void gettingRoleFailsWhenAlreadyLinkedToAnOrganizationWhereTheUserHasNoAdminRole() {

        Organization otherOrganization =
                organizationRestResource.save(
                        OrganizationTestBuilder.anOrganization()
                                .withRoles(RoleTestBuilder.aRole().withName("existingRole").build())
                                .build()
                );

        given()
                .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .body("{ \"name\": \"someEvilRoleName\" }")
                .get("/roles/{id}", restResourcesTransactionalHelper.getRoles(otherOrganization.getId()).get(0).getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
