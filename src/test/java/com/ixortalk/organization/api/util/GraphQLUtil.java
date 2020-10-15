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
package com.ixortalk.organization.api.util;

import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLSchema;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.join;

public class GraphQLUtil {

    public static String graphqlToJson(String payload)  {
        try {
            JSONObject json = new JSONObject();
            json.put("query", payload);
            return  json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String PAGE_FIELDS =
            "      page {" +
            "          totalPages" +
            "          totalElements" +
            "          number" +
            "          first" +
            "          last" +
            "          hasNext" +
            "          hasPrevious" +
            "      }";

    public static boolean isGraphQLAccessDeniedError(JsonPath jsonPath) {
        return jsonPath.getObject("data", Object.class) == null
                && jsonPath.getString("errors[0].message").equals("Access is denied")
                && jsonPath.getString("errors[0].extensions.classification").equals("AccessDenied");
    }

    public static RequestSpecification withGraphQLQuery(String query, String token) {
        return given()
                .auth()
                .preemptive()
                .oauth2(token)
                .contentType(JSON)
                .body(GraphQLUtil.graphqlToJson(query));
    }

    public static List<String> buildGraphQlFieldNames(GraphQLSchema graphQLSchema, String typeName, Map<String, String> nestedFieldNames) {
       return getGraphQLTypeFieldNames(graphQLSchema, typeName, nestedFieldNames);
    }

    private static List<String> getGraphQLTypeFieldNames(GraphQLSchema graphQLSchema, String typeName,  Map<String, String> nestedFieldNames) {
        return graphQLSchema
                .getType(typeName)
                .getChildren()
                .stream()
                .map(graphQLSchemaElement -> ((GraphQLNamedSchemaElement)graphQLSchemaElement).getName())
                .map(fieldName -> nestedFieldNames.containsKey(fieldName) ?
                        (fieldName + " { " + join(",", getGraphQLTypeFieldNames(graphQLSchema, nestedFieldNames.get(fieldName), nestedFieldNames)) + " } ")
                        : fieldName)
                .collect(Collectors.toList());
    }
}
