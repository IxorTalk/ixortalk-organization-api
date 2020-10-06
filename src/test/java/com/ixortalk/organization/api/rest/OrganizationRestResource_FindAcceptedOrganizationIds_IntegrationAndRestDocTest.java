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
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.organization.api.config.TestConstants.USER_EMAIL;
import static com.ixortalk.organization.api.domain.OrganizationTestBuilder.anOrganization;
import static com.ixortalk.organization.api.domain.Status.ACCEPTED;
import static com.ixortalk.organization.api.domain.Status.INVITED;
import static com.ixortalk.organization.api.domain.UserTestBuilder.aUser;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;


public class OrganizationRestResource_FindAcceptedOrganizationIds_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private Organization acceptedOrganization;
    private Organization anotherAcceptedOrganization;

    @Before
    public void before() {
        acceptedOrganization =
                anOrganization()
                        .withUsers(
                                aUser().withLogin(USER_EMAIL.toLowerCase()).withStatus(ACCEPTED).build(),
                                aUser().withStatus(INVITED).build()
                        )
                        .build();

        anotherAcceptedOrganization =
                anOrganization()
                        .withUsers(
                                aUser().withLogin(USER_EMAIL.toLowerCase()).withStatus(ACCEPTED).build(),
                                aUser().withStatus(ACCEPTED).build()
                        )
                        .build();

        Organization invitedOrganization =
                anOrganization()
                        .withUsers(
                                aUser().withLogin(USER_EMAIL.toLowerCase()).withStatus(INVITED).build(),
                                aUser().withStatus(ACCEPTED).build()
                        )
                        .build();

        Organization notInOrganization =
                anOrganization()
                        .withUsers(
                                aUser().withStatus(ACCEPTED).build()
                        )
                        .build();

        Organization organizationWithoutUsers =
                anOrganization().build();

        organizationRestResource.saveAll(newArrayList(
                acceptedOrganization,
                anotherAcceptedOrganization,
                invitedOrganization,
                notInOrganization,
                organizationWithoutUsers
        ));
    }

    @Test
    public void ok() {

        List<Long> ids =
                given()
                        .auth().preemptive().oauth2(TestConstants.USER_JWT_TOKEN)
                        .filter(
                                document("organizations/find-accepted-organization-ids/ok",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader())
                                ))
                        .contentType(JSON)
                        .when()
                        .get("/organizations/search/findAcceptedOrganizationIds")
                        .then()
                        .statusCode(SC_OK)
                        .extract().as(List.class)
                        .stream()
                        .mapToLong(o -> ((Integer) o).longValue())
                        .boxed()
                        .collect(toList());

        assertThat(ids).hasSize(2).containsOnly(acceptedOrganization.getId(), anotherAcceptedOrganization.getId());
    }

    @Test
    public void noAcceptedOrganizations() {

        List<?> ids =
                given()
                        .auth().preemptive().oauth2(TestConstants.OTHER_USER_JWT_TOKEN)
                        .contentType(JSON)
                        .when()
                        .get("/organizations/search/findAcceptedOrganizationIds")
                        .then()
                        .statusCode(SC_OK)
                        .extract().as(List.class);

        assertThat(ids).isEmpty();
    }

    @Test
    public void asAdminForOtherUserId() {

        List<Long> ids =
                given()
                        .auth().preemptive().oauth2(TestConstants.ADMIN_JWT_TOKEN)
                        .contentType(JSON)
                        .when()
                        .params("userId", USER_EMAIL)
                        .get("/organizations/search/findAcceptedOrganizationIds")
                        .then()
                        .statusCode(SC_OK)
                        .extract().as(List.class)
                        .stream()
                        .mapToLong(o -> ((Integer) o).longValue())
                        .boxed()
                        .collect(toList());

        assertThat(ids).hasSize(2).containsOnly(acceptedOrganization.getId(), anotherAcceptedOrganization.getId());
    }

    @Test
    public void asNonAdminForOwnUserId() {

        given()
                .auth().preemptive().oauth2(TestConstants.USER_JWT_TOKEN)
                .contentType(JSON)
                .when()
                .params("userId", USER_EMAIL)
                .get("/organizations/search/findAcceptedOrganizationIds")
                .then()
                .statusCode(SC_OK)
                .extract().as(List.class)
                .stream()
                .mapToLong(o -> ((Integer) o).longValue())
                .boxed()
                .collect(toList());
    }

    @Test
    public void asNonAdminForOtherUserId() {

        given()
                .auth().preemptive().oauth2(TestConstants.OTHER_USER_JWT_TOKEN)
                .contentType(JSON)
                .when()
                .parameters("userId", USER_EMAIL)
                .get("/organizations/search/findAcceptedOrganizationIds")
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
