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

import static com.ixortalk.organization.api.asset.AssetId.assetId;
import static com.ixortalk.test.util.Randomizer.nextString;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class AssetTestBuilder extends ReflectionInstanceTestBuilder<Asset> {

    private AssetId assetId = assetId(nextString("testAssetId"));

    private AssetPropertiesTestBuilder assetPropertiesTestBuilder = AssetPropertiesTestBuilder.anAssetProperties();

    private AssetTestBuilder() {}

    public static AssetTestBuilder anAsset() {
        return new AssetTestBuilder();
    }

    @Override
    public void setFields(Asset instance) {
        setField(instance, "assetId", assetId);
        setField(instance, "assetProperties", assetPropertiesTestBuilder.build());
    }

    public AssetTestBuilder withAssetId(AssetId assetId) {
        this.assetId = assetId;
        return this;
    }

    public AssetTestBuilder withOrganizationId(OrganizationId organizationId) {
        this.assetPropertiesTestBuilder.withOrganizationId(organizationId);
        return this;
    }

    public AssetTestBuilder withDeviceId(DeviceId deviceId) {
        this.assetPropertiesTestBuilder.withDeviceId(deviceId);
        return this;
    }

    public AssetTestBuilder withActions(Object actions) {
        this.assetPropertiesTestBuilder.withActions(actions);
        return this;
    }

    public AssetTestBuilder withOtherProperty(String key, Object value) {
        this.assetPropertiesTestBuilder.withOtherProperty(key, value);
        return this;
    }
}