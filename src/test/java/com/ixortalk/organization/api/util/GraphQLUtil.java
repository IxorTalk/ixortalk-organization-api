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

import io.restassured.path.json.JsonPath;
import org.json.JSONException;
import org.json.JSONObject;

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
}
