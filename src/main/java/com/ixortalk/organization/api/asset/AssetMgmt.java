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

import com.ixortalk.autoconfigure.oauth2.feign.ServiceToServiceFeignConfiguration;
import com.ixortalk.organization.api.domain.OrganizationId;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "assetMgmt",url = "${ixortalk.server.assetmgmt.url}", configuration = ServiceToServiceFeignConfiguration.class, decode404 = true)
public interface AssetMgmt {

    @PostMapping(value = "/assets/search/property", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    List<Asset> getAssets(@RequestBody OrganizationId organizationId);

    @PostMapping(value = "/assets/find/property", consumes = APPLICATION_JSON_VALUE)
    Asset getAssetByDeviceId(@RequestBody DeviceId deviceId);

    @PatchMapping(value = "/assets/{assetId}", consumes = APPLICATION_JSON_VALUE)
    void  saveAsset(@PathVariable("assetId") String assetId, @RequestBody Object assetRoles);

    @PutMapping(value = "/assets/{assetId}/properties", consumes = APPLICATION_JSON_VALUE)
    void saveProperties(@PathVariable("assetId") String assetId, @RequestBody Object properties);
}
