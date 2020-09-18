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
import com.ixortalk.organization.api.domain.EnhancedUserProjection;
import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class OrganizationRestResource_GetUsersInOrganization_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final ResponseFieldsSnippet RESPONSE_FIELDS_SNIPPET =
            responseFields(
                    fieldWithPath("_embedded.users[].id").type(NUMBER).description("The primary key for the user in this organization"),
                    fieldWithPath("_embedded.users[].login").type(STRING).description("The `login` for the user in the organization"),
                    fieldWithPath("_embedded.users[].userInfo.firstName").type(STRING).description("The user's first name"),
                    fieldWithPath("_embedded.users[].userInfo.lastName").type(STRING).description("The user's last name"),
                    fieldWithPath("_embedded.users[].userInfo.profilePictureUrl").type(STRING).description("The user's profile picture URL"),
                    fieldWithPath("_embedded.users[].userInfo.email").type(STRING).description("The user's email address"),
                    fieldWithPath("_embedded.users[].userInfo").optional().description("The optional object containing the user info"),
                    fieldWithPath("_embedded.users[].admin").type(BOOLEAN).description("The Admin flag, true or false whether the user is admin in the organization."),
                    fieldWithPath("_embedded.users[].status").type(STRING).description("The status of the user denotes whether a user has accepted the invitation or not yet."),
                    subsectionWithPath("_embedded.users[]._links").ignored(),
                    subsectionWithPath("_links").ignored()
            );

    @Before
    public void before() {
        when(auth0Roles.getUsersInRole(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME)).thenReturn(newHashSet(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, "completely-different-user@ixortalk.com"));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(newHashSet(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME, FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, ROLE_IN_ORGANIZATION_Y_ROLE_NAME, "ROLE_NOT_EVEN_USED_IN_ORGANIZATION_DOMAIN"));
        userInOrganizationXAcceptedHavingARole.setAdmin(true);
        userRestResource.save(userInOrganizationXAcceptedHavingARole);
    }

    @Test
    public void asAdmin() {

        JsonPath result =
                given()
                        .auth()
                        .preemptive()
                        .oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}/users?projection=" + EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME, organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(result.getList("_embedded.users.login.flatten()"))
                .containsOnly(
                        USER_IN_ORGANIZATION_X_INVITED_EMAIL,
                        USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL,
                        USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL,
                        USER_IN_ORGANIZATION_X_AND_Y_EMAIL,
                        USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL + "') }.userInfo.firstName")).containsExactly(USER_IN_ORGANIZATION_X_ACCEPTED_FIRST_NAME);
        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL + "') }.userInfo.lastName")).containsExactly(USER_IN_ORGANIZATION_X_ACCEPTED_LAST_NAME);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL + "') }.admin")).containsExactly(true);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_AND_Y_EMAIL + "') }.userInfo").get(0)).isNull();
        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_AND_Y_EMAIL + "') }.admin")).containsExactly(false);
    }

    @Test
    public void asUserInOrganizationXAdminRole() {

        JsonPath result =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)
                        .filter(
                                document("organizations/users/get-users-in-org/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        RESPONSE_FIELDS_SNIPPET)
                        )
                        .when()
                        .get("/organizations/{id}/users?projection=" + EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME, organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(result.getList("_embedded.users.login.flatten()"))
                .containsOnly(
                        USER_IN_ORGANIZATION_X_INVITED_EMAIL,
                        USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL,
                        USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL,
                        USER_IN_ORGANIZATION_X_AND_Y_EMAIL,
                        USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL + "') }.userInfo.firstName")).containsExactly(USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME);
        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL + "') }.userInfo.lastName")).containsExactly(USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL + "') }.admin")).containsExactly(true);

        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_AND_Y_EMAIL + "') }.userInfo").get(0)).isNull();
        assertThat(result.getList("_embedded.users.findAll { user -> user.login.equals('" + USER_IN_ORGANIZATION_X_AND_Y_EMAIL + "') }.admin")).containsExactly(false);
    }

    @Test
    public void asUserNotInOrganizationXAdminRole() {

        given()
                .auth()
                .preemptive()
                .oauth2(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)
                .filter(
                        document("organizations/users/get-users-in-org/no-access-to-organization",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()))
                )
                .when()
                .get("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void loginAndStatusProjection() {
        given()
                .auth()
                .preemptive()
                .oauth2(ADMIN_JWT_TOKEN)
                .when()
                .param("projection", "loginAndStatus")
                .get("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(SC_OK)
                .body("_embedded.users[0]", hasKey("login"))
                .body("_embedded.users[0]", hasKey("status"))
                .body("_embedded.users[0]", not(hasKey("id")))
                .body("_embedded.users[0]._links", not(hasKey("assignAdminRole")));
    }
}
