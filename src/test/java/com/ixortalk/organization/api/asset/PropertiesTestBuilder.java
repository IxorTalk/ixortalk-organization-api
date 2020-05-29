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

import com.ixortalk.organization.api.domain.OrganizationId;
import com.ixortalk.test.builder.ReflectionInstanceTestBuilder;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.domain.OrganizationId.organizationId;
import static com.ixortalk.test.util.Randomizer.nextString;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class PropertiesTestBuilder extends ReflectionInstanceTestBuilder<Properties> {

    private OrganizationId organizationId = organizationId(nextLong());
    private DeviceId deviceId = deviceId(nextString("deviceId"));
    private Object actions = nextString("testBuilderActions");
    private Map<String, Object> otherProperties = newHashMap();

    private PropertiesTestBuilder() {}

    public static PropertiesTestBuilder aProperties() {
        return new PropertiesTestBuilder();
    }

    @Override
    public void setFields(Properties instance) {
        setField(instance, "organizationId", organizationId);
        setField(instance, "deviceId", deviceId);
        setField(instance, "actions", actions);

        setField(instance, "otherProperties", otherProperties);
    }

    public PropertiesTestBuilder withDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public PropertiesTestBuilder withActions(Object actions) {
        this.actions = actions;
        return this;
    }

    public PropertiesTestBuilder withOrganizationId(OrganizationId organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public PropertiesTestBuilder withOtherProperty(String key, Object value) {
        otherProperties.put(key, value);
        return this;
    }
}
