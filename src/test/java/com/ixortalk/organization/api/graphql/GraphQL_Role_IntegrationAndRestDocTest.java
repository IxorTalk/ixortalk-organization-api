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
import graphql.schema.GraphQLSchema;
import io.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import java.util.HashMap;

import static com.ixortalk.organization.api.util.GraphQLUtil.PAGE_FIELDS;
import static com.ixortalk.organization.api.util.GraphQLUtil.withGraphQLQuery;
import static java.lang.String.join;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class GraphQL_Role_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Inject
    private GraphQLSchema graphQLSchema;

    private String graphQlQueryFieldNames;

    @Before
    public void initFieldNames() {
        graphQlQueryFieldNames = "{ content {"
                + join(",", GraphQLUtil.buildGraphQlFieldNames(graphQLSchema, "Role", new HashMap<String, String>() {
            {
                put("users", "User");
            }
        }))
                + "}" + PAGE_FIELDS + "}}";
    }

    @Test
    public void asAdmin() {
        String query =
                "{ rolesPage(page:0, size:10, sort:\"name\", direction:\"asc\") "
                + graphQlQueryFieldNames;


        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.ADMIN_JWT_TOKEN)
                .filter(
                        document("graphql/roles/as-admin/success",
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
        assertThat(jsonPath.getList("data.rolesPage.content")).hasSize(3);

    }

    @Test
    public void pagingAndSorting() {
        String query = "{ rolesPage( size:2) "
                        + graphQlQueryFieldNames;
        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.ADMIN_JWT_TOKEN)
                        .post("/graphql")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("data.rolesPage.content")).hasSize(2);
        assertThat(jsonPath.getInt("data.rolesPage.page.totalElements")).isEqualTo(3);
    }

    @Test
    public void asUser() {
        String query = "{ rolesPage( size:2 )  "
                + graphQlQueryFieldNames;

        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .post("/graphql")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath).matches(GraphQLUtil::isGraphQLAccessDeniedError, "is GraphQL Access Denied error");
    }

    @Test
    public void asOrganizationAdmin_notFilteringOnOrganizationId() {
        String query = "{ rolesPage( size:2 )  " + graphQlQueryFieldNames;
        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("graphql/roles/as-organization-admin/not-filtering-on-organization-id",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()
                                        )))
                        .post("/graphql")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath).matches(GraphQLUtil::isGraphQLAccessDeniedError, "is GraphQL Access Denied error");
    }

    @Test
    public void asOrganizationAdmin_filteringOnOrganizationId() {
        String query =
                "{ rolesPage(page:0, size:2, sort:\"name\", direction:\"asc\", filter:\"organizationId:" + organizationX.getId() + "\")  "
                + graphQlQueryFieldNames;
        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("graphql/roles/as-organization-admin/success",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()
                                        )))
                        .post("/graphql")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("data.rolesPage.content")).hasSize(2);
        assertThat(jsonPath.getInt("data.rolesPage.page.totalElements")).isEqualTo(2);
    }

    @Test
    public void asOrganizationAdmin_combinedFilter() {
        String query =
                "{ rolesPage(page:0, size:2, sort:\"name\", direction:\"asc\", filter:\"organizationId:" + organizationX.getId() + ",name:First role\")  "
                + graphQlQueryFieldNames;
        JsonPath jsonPath = withGraphQLQuery(query, TestConstants.USER_IN_ORGANIZATION_X_ADMIN_JWT_TOKEN)
                        .filter(
                                document("graphql/roles/as-organization-admin/combined-filter",
                                        preprocessRequest(staticUris(), prettyPrint()),
                                        preprocessResponse(prettyPrint()),
                                        requestHeaders(describeAuthorizationTokenHeader()
                                        )))
                        .post("/graphql")
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath();

        assertThat(jsonPath.getList("data.rolesPage.content")).hasSize(1);
        assertThat(jsonPath.getInt("data.rolesPage.page.totalElements")).isEqualTo(1);
    }
}
