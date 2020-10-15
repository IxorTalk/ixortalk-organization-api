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
package com.ixortalk.organization.api.config;

import com.ixortalk.organization.api.graphql.resolvers.OrganizationGraphQLQueryResolver;
import com.ixortalk.organization.api.graphql.resolvers.RoleGraphQLQueryResolver;
import com.ixortalk.organization.api.graphql.resolvers.UserGraphQLQueryResolver;
import graphql.execution.ExecutionStrategy;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class GraphQLConfig {

    @Bean
    public GraphQLScalarType jsonType() {
        return ExtendedScalars.Json;
    }

    @Bean
    public GraphQLScalarType dateType() {
        return ExtendedScalars.Date;
    }

    @Bean
    public OrganizationGraphQLQueryResolver organizationGraphQLQueryResolver() {
        return new OrganizationGraphQLQueryResolver();
    }

    @Bean
    public UserGraphQLQueryResolver userGraphQLQueryResolver() {
        return new UserGraphQLQueryResolver();
    }

    @Bean
    public RoleGraphQLQueryResolver roleGraphQLQueryResolver() {
        return new RoleGraphQLQueryResolver();
    }

    @Bean
    public Map<String, ExecutionStrategy> executionStrategies() {
        //execution strategy, needed to avoid lazy init exceptions
        Map<String, ExecutionStrategy> executionStrategyMap = new HashMap<>();
        executionStrategyMap.put("queryExecutionStrategy", new AsyncTransactionalExecutionStrategy());
        return executionStrategyMap;
    }
}
