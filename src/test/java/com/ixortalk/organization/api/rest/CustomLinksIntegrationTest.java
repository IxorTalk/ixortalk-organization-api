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

import java.net.MalformedURLException;

import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.domain.EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomLinksIntegrationTest extends AbstractSpringIntegrationTest {

    @Test
    public void enhancedUsersLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();


        assertThat(jsonPath.getString("_links.enhancedUsers.href"))
                .isEqualTo(constructFullUri("/organizations/" + organizationX.getId() + "/users?projection=" + ENHANCED_USER_PROJECTION_NAME));
    }

    @Test
    public void adminUsersLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();


        assertThat(jsonPath.getString("_links.adminUsers.href"))
                .isEqualTo(constructFullUri("/organizations/" + organizationX.getId() + "/adminUsers"));
    }

    @Test
    public void findUsersByOrganization() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.findUsersByOrganization.href"))
                .isEqualTo(constructFullUri("/users/search/findByOrganizationId?organizationId=" + organizationX.getId() + "&projection=" + ENHANCED_USER_PROJECTION_NAME));
    }

    @Test
    public void findRolesByOrganization() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.findRolesByOrganization.href"))
                .isEqualTo(constructFullUri("/roles/search/findByOrganizationId?organizationId=" + organizationX.getId()));
    }

    @Test
    public void resendInviteLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/users/{id}", userInOrganizationXInvited.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.resend-invite.href"))
                .isEqualTo(constructFullUri("/users/" + userInOrganizationXInvited.getId() + "/resend-invite"));
    }

    @Test
    public void userEnhancedProjectionLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/users/{id}", userInOrganizationXInvited.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links." + ENHANCED_USER_PROJECTION_NAME + ".href"))
                .isEqualTo(constructFullUri("/users/" + userInOrganizationXInvited.getId() + "?projection=" + ENHANCED_USER_PROJECTION_NAME));
    }

    @Test
    public void enhancedUserResendInviteLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .param("projection", ENHANCED_USER_PROJECTION_NAME)
                        .get("/users/{id}", userInOrganizationXInvited.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.resend-invite.href"))
                .isEqualTo(constructFullUri("/users/" + userInOrganizationXInvited.getId() + "/resend-invite"));
    }

    @Test
    public void promoteToAdminRoleLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/users/{id}", userInOrganizationXInvited.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.promoteToAdmin.href"))
                .isEqualTo(constructFullUri("/" + organizationX.getId() + "/" + userInOrganizationXInvited.getId() + "/promote-to-admin"));

    }

    @Test
    public void removeAdminRightsLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/users/{id}", userInOrganizationXInvited.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.removeAdminRights.href"))
                .isEqualTo(constructFullUri("/" + organizationX.getId() + "/" + userInOrganizationXInvited.getId() + "/remove-admin-rights"));

    }

    @Test
    public void findAssetsLink() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.devices.href"))
                .isEqualTo(constructFullUri("/organizations/" + organizationX.getId() + "/devices"));

    }

    @Test
    public void getOrganizationInformation() throws MalformedURLException {
        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.organizationInformation.href"))
                .isEqualTo(constructFullUri("/organization-information/" + organizationX.getId()));
    }

    @Test
    public void getOrganizationImage() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.image.href")).isEqualTo(IMAGE_DOWNLOAD_URL_PREFIX + organizationX.getImage());
    }

    @Test
    public void getOrganizationLogo() {

        JsonPath jsonPath =
                given()
                        .auth().preemptive().oauth2(USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getString("_links.logo.href")).isEqualTo(IMAGE_DOWNLOAD_URL_PREFIX + organizationX.getLogo());
    }
}
