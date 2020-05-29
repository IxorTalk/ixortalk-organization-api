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
package com.ixortalk.organization.api.rest;

import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.image.ImageService;
import feign.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.function.BiFunction;

import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.notFound;

@RestController
public class ImageController {

    @Inject
    private ImageService imageService;

    @Inject
    private OrganizationRestResource organizationRestResource;

    @PostMapping(path = "/organizations/{organizationId}/image")
    public ResponseEntity<?> uploadImage(@PathVariable("organizationId") Long organizationId, @RequestPart("file") MultipartFile multipartFile) {
        return uploadToImageService(organizationId, multipartFile, Organization::setImage, "image");
    }

    @PostMapping(path = "/organizations/{organizationId}/logo")
    public ResponseEntity<?> uploadLogo(@PathVariable("organizationId") Long organizationId, @RequestPart("file") MultipartFile multipartFile) {
        return uploadToImageService(organizationId, multipartFile, Organization::setLogo, "logo");
    }

    private ResponseEntity<?> uploadToImageService(Long organizationId, MultipartFile multipartFile, BiFunction<Organization, String, Organization> function, String keyName) {
        return organizationRestResource.findById(organizationId)
                .map(organization -> {
                            Response response = imageService.uploadImage(multipartFile, "organizations/" + organizationId + "/" + keyName);
                            String location = new ArrayList<>(response.headers().get(HttpHeaders.LOCATION)).get(0);
                            organizationRestResource.save(function.apply(organization, location));
                            try {
                                return created(new URI(location)).build();
                            } catch (URISyntaxException e) {
                                throw new IllegalArgumentException("Could not save image: " + e.getMessage());
                            }
                        }
                )
                .orElse(notFound().build());
    }
}
