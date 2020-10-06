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

import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfo;
import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.OrganizationTestBuilder;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.domain.UserTestBuilder;
import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.retrievedAdminTokenAuthorizationHeader;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Optional.of;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


public class OrganizationRestResource_AddUserInOrganization_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final String LOGIN_TO_ADD = nextString("user") + "@ixortalk.com";
    private static final String EXISTING_LOGIN = nextString("existing-user");

    @Before
    public void before() {

        when(auth0Users.userExists(LOGIN_TO_ADD)).thenReturn(true);
        when(auth0Users.getUserInfo(LOGIN_TO_ADD)).thenReturn(of(new UserInfo(LOGIN_TO_ADD, "user", "already", "exists")));

        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(aResponse().withStatus(SC_OK)));
    }

    @Test
    public void asAdmin() {

        String createdUserURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers + 1)
                .extracting(User::getLogin)
                .containsOnly(LOGIN_TO_ADD, TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);
    }


    @Test
    public void alreadyAddedUserId() {

        String createdUserURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).hasSize(organizationXInitialNumberOfUsers);
    }

    @Test
    public void userCreatedTwice() {

        String firstCreatedUserURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");
        String secondCreatedUserURI =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(firstCreatedUserURI + "\n" + secondCreatedUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);


        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).hasSize(organizationXInitialNumberOfUsers + 1);
    }

    @Test
    public void asUserInOrganizationXAdminRole() {

        String createdUserURI =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/add-user/add/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestFields(
                                                fieldWithPath("login").type(STRING).description("The `login` (email address) for the user to add")
                                        )
                                )
                        )
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-user/link/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers + 1)
                .extracting(User::getLogin)
                .containsOnly(LOGIN_TO_ADD, TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);
    }

    @Test
    public void addedUserDoesNotExistsYet() {

        when(auth0Users.userExists(LOGIN_TO_ADD)).thenReturn(false);

        mailingServiceWireMockRule.stubFor(post(urlEqualTo("/mailing/send"))
                .andMatching(retrievedAdminTokenAuthorizationHeader())
                .willReturn(aResponse().withStatus(SC_OK)));

        String createdUserURI =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/add-user/add/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestFields(
                                                fieldWithPath("login").type(STRING).description("The `login` (email address) for the user to add"),
                                                fieldWithPath("inviteLanguage").type(STRING).description("Optional language that will be used for the invite mail (ISO 639-1 2-letter code)").optional()
                                        )
                                )
                        )
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\", \"inviteLanguage\": \"en\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-user/link/ok",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);


        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId()))
                .hasSize(organizationXInitialNumberOfUsers + 1)
                .extracting(User::getLogin)
                .containsOnly(LOGIN_TO_ADD, TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_AND_Y_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_EMAIL, USER_IN_ORGANIZATION_X_CREATED_EMAIL);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void asUserInNotOrganizationXAdminRole() {

        String createdUserURI =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                        .filter(
                                document("organizations/add-user/add/no-access",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()),
                                        requestFields(
                                                fieldWithPath("login").type(STRING).description("The `login` (email address) for the user to add")
                                        )
                                )
                        )
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath()
                        .getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_JWT_TOKEN)
                .filter(
                        document("organizations/add-user/link/no-access",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_FORBIDDEN);

        assertThat(restResourcesTransactionalHelper.getUsers(organizationX.getId())).hasSize(organizationXInitialNumberOfUsers).extracting(User::getLogin).doesNotContain(LOGIN_TO_ADD);
    }

    @Test
    public void savingUserFailsWhenAlreadyLinkedToAnOrganizationWhereTheUserHasNoAdminRole() {

        Organization otherOrganization =
                organizationRestResource.save(
                        OrganizationTestBuilder.anOrganization()
                                .withUsers(UserTestBuilder.aUser().withLogin(EXISTING_LOGIN).build())
                                .build()
                );

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .body("{ \"login\": \"someEvilUsersLogin\" }")
                .put("/users/{id}", restResourcesTransactionalHelper.getUsers(otherOrganization.getId()).get(0).getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void gettingUserFailsWhenAlreadyLinkedToAnOrganizationWhereTheUserHasNoAdminRole() {

        Organization otherOrganization =
                organizationRestResource.save(
                        OrganizationTestBuilder.anOrganization()
                                .withUsers(UserTestBuilder.aUser().withLogin(EXISTING_LOGIN).build())
                                .build()
                );

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .get("/users/{id}", restResourcesTransactionalHelper.getUsers(otherOrganization.getId()).get(0).getId())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void sendInviteMail() {

        when(auth0Users.userExists(LOGIN_TO_ADD)).thenReturn(false);

        JsonPath jsonpath =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + LOGIN_TO_ADD + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath();

        String createdUserURI = jsonpath.getString("_links.self.href");

        given()
                .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(TEXT_URI_LIST_VALUE)
                .body(createdUserURI)
                .post("/organizations/{id}/users", organizationX.getId())
                .then()
                .statusCode(HTTP_NO_CONTENT);

        mailingServiceWireMockRule.verify(0, postRequestedFor(urlEqualTo("/mailing/send")));
    }

    @Test
    public void loginIsSavedInLowercase() {

        String loginContainingSomeUppercaseLetters = "theUserId";

        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .contentType(JSON)
                        .body("{ \"login\": \"" + loginContainingSomeUppercaseLetters + "\" }")
                        .post("/users")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("login")).isEqualTo(loginContainingSomeUppercaseLetters.toLowerCase());
        assertThat(userRestResource.findById(jsonPath.getLong("id"))).get().extracting(User::getLogin).isEqualTo(loginContainingSomeUppercaseLetters.toLowerCase());
    }
}
