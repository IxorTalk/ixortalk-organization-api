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
package com.ixortalk.organization.api.graphql.querydsl;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.NumberPath;

import java.util.function.Function;

public class ValidateFilteredOnOrganizationIdVisitor implements Visitor<Boolean, Function<Long, Boolean>> {
    private NumberPath<Long> organizationPath;

    public ValidateFilteredOnOrganizationIdVisitor( NumberPath<Long> organizationPath) {
        this.organizationPath = organizationPath;
    }

    @Override
    public Boolean visit(Constant<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return expr.getType().equals(Long.class) && hasOrganizationAccessFunction.apply((Long) expr.getConstant());
    }

    @Override
    public Boolean visit(Operation<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        if (expr.getOperator() == Ops.AND) {
            return expr.getArgs().stream().anyMatch(expression -> expression.accept(this, hasOrganizationAccessFunction));
        }
        if (expr.getOperator() == Ops.EQ) {
            return expr.getArgs().stream().allMatch(expression -> expression.accept(this, hasOrganizationAccessFunction));
        }
        return false;
    }

    @Override
    public Boolean visit(Path<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return expr.equals(organizationPath);
    }

    @Override
    public Boolean visit(FactoryExpression<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return false;
    }

    @Override
    public Boolean visit(ParamExpression<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return false;
    }

    @Override
    public Boolean visit(SubQueryExpression<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return false;
    }

    @Override
    public Boolean visit(TemplateExpression<?> expr, Function<Long, Boolean> hasOrganizationAccessFunction) {
        return false;
    }
}
