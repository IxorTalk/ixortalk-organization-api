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
package com.ixortalk.organization.api.graphql;

import com.ixortalk.organization.api.AbstractSpringIntegrationTest;
import com.ixortalk.organization.api.config.TestConstants;
import com.ixortalk.organization.api.util.GraphQLUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class GraphQL_Organization_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Test
    public void asAdmin() {
        String query =
                "{" +
                        "  organizationsPage(page:0, size:2, sort:\"name\", direction:\"asc\", filter:\"name:org\") {" +
                        "content {" +
                    "          id" +
                    "          name" +
                    "          phoneNumber" +
                    "          emailAddress" +
                    "          image" +
                    "          logo" +
                    "          roles {" +
                    "              id" +
                    "              name" +
                    "          }" +
                    "          users {" +
                    "              id" +
                    "              login" +
                    "          }" +
                    "          address {" +
                    "              streetAndNumber" +
                    "              postalCode" +
                    "              city" +
                    "              country" +
                    "          }" +
                    "      }" +
                        GraphQLUtil.PAGE_FIELDS +
                        "  }" +
                        "}";

        JsonPath jsonPath = given()
                .config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs("application/graphql", ContentType.TEXT)))
                .auth()
                .preemptive()
                .oauth2(TestConstants.ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(GraphQLUtil.graphqlToJson(query))
                .filter(
                        document("graphql/organizations/as-admin/success",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()
                        )))
                .when()
                .post("/graphql")
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath();

        assertThat(jsonPath.getList("data.organizationsPage.content").size()).isEqualTo(2);
        assertThat(jsonPath.getInt("data.organizationsPage.page.totalElements")).isEqualTo(3);
    }

    @Test
    public void asNonAdminICanNotSearchOrganizations() {
        String query =
                "{" +
                        "  organizationsPage(page:0, size:10, sort:\"name\", direction:\"asc\", filter:\"name:group1\") {" +
                        "content {" +
                        "          id" +
                        "          name" +
                        "          address {" +
                        "              streetAndNumber" +
                        "              postalCode" +
                        "              city" +
                        "              country" +
                        "          }" +
                        "      }" +
                        GraphQLUtil.PAGE_FIELDS +
                        "  }" +
                        "}";

        JsonPath jsonPath = given()
                .config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs("application/graphql", ContentType.TEXT)))
                .auth()
                .preemptive()
                .oauth2(TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                .contentType(JSON)
                .body(GraphQLUtil.graphqlToJson(query))
                .filter(
                        document("graphql/organizations/not-as-admin/access-denied",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()
                                )))
                .when()
                .post("/graphql")
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath();

                assertThat(jsonPath).matches(GraphQLUtil::isGraphQLAccessDeniedError, "is GraphQL Access Denied error");

    }
}
