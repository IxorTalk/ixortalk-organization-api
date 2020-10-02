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

import static org.springframework.test.util.ReflectionTestUtils.setField;

public class AssetPropertiesTestBuilder extends ReflectionInstanceTestBuilder<AssetProperties> {

    private PropertiesTestBuilder propertiesTestBuilder = PropertiesTestBuilder.aProperties();

    private AssetPropertiesTestBuilder() {}

    public static AssetPropertiesTestBuilder anAssetProperties() {
        return new AssetPropertiesTestBuilder();
    }

    @Override
    public void setFields(AssetProperties instance) {
        setField(instance, "properties", propertiesTestBuilder.build());
    }

    public AssetPropertiesTestBuilder withOrganizationId(OrganizationId organizationId) {
        this.propertiesTestBuilder.withOrganizationId(organizationId);
        return this;
    }

    public AssetPropertiesTestBuilder withDeviceId(DeviceId deviceId) {
        this.propertiesTestBuilder.withDeviceId(deviceId);
        return this;
    }

    public AssetPropertiesTestBuilder withActions(Object actions) {
        this.propertiesTestBuilder.withActions(actions);
        return this;
    }

    public AssetPropertiesTestBuilder withOtherProperty(String key, Object value) {
        this.propertiesTestBuilder.withOtherProperty(key, value);
        return this;
    }
}
