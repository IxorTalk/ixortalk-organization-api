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
import com.ixortalk.organization.api.domain.Role;
import io.restassured.path.json.JsonPath;
import org.junit.Test;

import javax.inject.Inject;

import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class RoleRestResource_ExposeRole_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private final String NEW_ROLE = "aNewRoleToTest";

    @Inject
    private RoleRestResource roleRestResource;

    @Test
    public void roleExposed() {

        JsonPath result =
                given()
                        .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                        .when()
                        .get("/organizations/{id}/roles", organizationX.getId())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(result.getList("_embedded.roles.role.flatten()")).hasSize(2).contains(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME);
    }

    @Test
    public void roleNotEditableWhenUpdated() throws JsonProcessingException {

        setField(firstRoleInOrganizationX, "role", NEW_ROLE);

        given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(objectMapper.writeValueAsString(firstRoleInOrganizationX))
                .when()
                .put("/roles/{id}", firstRoleInOrganizationX.getId())
                .then()
                .statusCode(HTTP_OK);

        assertThat(roleRestResource.findById(firstRoleInOrganizationX.getId())).get().extracting(Role::getRole).isEqualTo(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME);
    }

    @Test
    public void roleNotEditableWhenAdded() {

        Long roleId = given()
                .auth().preemptive().oauth2(ADMIN_JWT_TOKEN)
                .when()
                .contentType(JSON)
                .body("{ \"name\": \"" + NEW_ROLE + "\", \"role\": \""+ NEW_ROLE+"\" }")
                .post("/roles")
                .then()
                .statusCode(SC_CREATED)
                .extract().jsonPath()
                .getLong("id");

        assertThat(roleRestResource.findById(roleId)).get().extracting(Role::getRole).isNull();
    }
}
