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
package com.ixortalk.organization.api.graphql.util;

import com.querydsl.core.types.Predicate;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.inject.Inject;

import static com.querydsl.core.types.dsl.Expressions.asBoolean;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.data.util.ClassTypeInformation.from;
import static org.springframework.data.util.Pair.of;

@Service
public class QuerydslService {

    private static final String FIELD_SEPARATOR = ",";
    private static final String KEY_VALUE_SEPARATOR = ":";

    @Inject
    private QuerydslPredicateBuilder querydslPredicateBuilder;

    @Inject
    private QuerydslBindings querydslBindings;

    public <T> Predicate buildPredicate(Class<T> type, String filter) {
        return !isBlank(filter) ? querydslPredicateBuilder.getPredicate(from(type), toMultiValueMap(filter), querydslBindings) : asBoolean(true).isTrue();
    }

    private MultiValueMap<String, String> toMultiValueMap(String filter) {
        return stream(filter.split(FIELD_SEPARATOR))
                .map(s -> of(substringBefore(s, KEY_VALUE_SEPARATOR), substringAfter(s, KEY_VALUE_SEPARATOR)))
                .collect(groupingBy(Pair::getFirst, LinkedMultiValueMap::new, mapping(Pair::getSecond, toList())));
    }
}
