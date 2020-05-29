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
package com.ixortalk.organization.api.asset;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.ixortalk.organization.api.domain.OrganizationId;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class Properties {

    private OrganizationId organizationId;
    private DeviceId deviceId;
    private Object actions;

    private Map<String, Object> otherProperties = newHashMap();

    private Properties() {
    }

    @JsonUnwrapped
    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    @JsonUnwrapped
    public DeviceId getDeviceId() {
        return deviceId;
    }

    public Object getActions() {
        return actions;
    }

    @JsonAnySetter
    public void setOtherProperty(String key, Object value) {
        otherProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public Object get(String propertyName) {
        return MappedField.INDEXED_PROPERTIES.containsKey(propertyName) ?
                MappedField.INDEXED_PROPERTIES.get(propertyName).get(this) :
                otherProperties.get(propertyName);

    }

    public enum MappedField {

        ORGANIZATION_ID("organizationId", Properties::getOrganizationId, o -> ((OrganizationId)o).longValue()),
        DEVICE_ID("deviceId", Properties::getDeviceId, o -> ((DeviceId)o).stringValue()),
        ACTIONS("actions", Properties::getActions),
        IMAGE("image", properties -> properties.getOtherProperties().get("image"));

        public static final Map<String, MappedField> INDEXED_PROPERTIES = stream(MappedField.values()).collect(toMap(MappedField::getPropertyName, identity()));

        private String propertyName;
        private Function<Properties, Object> mappedGetter;

        private Optional<Function<Object, Object>> unwrap;

        MappedField(String propertyName, Function<Properties, Object> mappedGetter) {
            this(propertyName, mappedGetter, null);
        }

        MappedField(String propertyName, Function<Properties, Object> mappedGetter, Function<Object, Object> unwrap) {
            this.propertyName = propertyName;
            this.mappedGetter = mappedGetter;
            this.unwrap = ofNullable(unwrap);
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object get(Properties properties) {
            return unwrap
                    .map(unwrapFunction -> unwrapFunction.apply(mappedGetter.apply(properties)))
                    .orElseGet(() -> mappedGetter.apply(properties));
        }
    }

}
