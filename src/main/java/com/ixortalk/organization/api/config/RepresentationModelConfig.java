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

import com.ixortalk.organization.api.domain.EnhancedUserProjection;
import com.ixortalk.organization.api.domain.Organization;
import com.ixortalk.organization.api.domain.Role;
import com.ixortalk.organization.api.domain.User;
import com.ixortalk.organization.api.rest.*;
import com.ixortalk.organization.api.service.ImageMethodsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.ixortalk.organization.api.config.RepresentationModelConfig.VarsBuilder.linkVars;
import static java.util.Optional.ofNullable;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Configuration
public class RepresentationModelConfig {

    @Inject
    private RepositoryEntityLinks repositoryEntityLinks;

    @Inject
    private OrganizationRestResource organizationRestResource;

    @Inject
    private UserRestResource userRestResource;

    @Inject
    private ImageMethodsService imageMethodsService;

    @Bean
    public RepresentationModelProcessor<EntityModel<Organization>> organizationRepresentationModel() {
        // noinspection Convert2Lambda
        return new RepresentationModelProcessor<EntityModel<Organization>>() {
            @Override
            public EntityModel<Organization> process(EntityModel<Organization> resource) {
                resource.add(
                        repositoryEntityLinks.linkForItemResource(Organization.class, resource.getContent().getId())
                                .slash("users?projection=" + EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME)
                                .withRel("enhancedUsers"));
                resource.add(
                        WebMvcLinkBuilder.linkTo(
                                methodOn(OrganizationRestController.class).getAdminUsers(resource.getContent().getId()))
                                .withRel("adminUsers"));
                resource.add(
                        linkTo(
                                methodOn(OrganizationRestController.class)
                                        .getDevices(resource.getContent().getId()))
                                .withRel("devices"));
                resource.add(
                        repositoryEntityLinks.linksToSearchResources(User.class)
                                .getLink("findByOrganizationId")
                                .get()
                                .expand(
                                        linkVars()
                                                .with("organizationId", resource.getContent().getId())
                                                .with("projection", EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME)
                                                .build())
                                .withRel("findUsersByOrganization"));
                resource.add(
                        repositoryEntityLinks.linksToSearchResources(Role.class)
                                .getLink("findByOrganizationId")
                                .get()
                                .expand(
                                        linkVars()
                                                .with("organizationId", resource.getContent().getId())
                                                .build())
                                .withRel("findRolesByOrganization"));
                resource.add(
                        WebMvcLinkBuilder.linkTo(
                                methodOn(OrganizationInformationController.class)
                                        .findOne(resource.getContent().getId(), null))
                                .withRel("organizationInformation"));

                ofNullable(resource.getContent().getImage())
                        .ifPresent(image -> resource.add(new Link(imageMethodsService.constructImageLink(image), "image")));
                ofNullable(resource.getContent().getLogo())
                        .ifPresent(logo -> resource.add(new Link(imageMethodsService.constructImageLink(logo), "logo")));
                return resource;
            }
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<User>> userRepresentationModel() {
        // noinspection Convert2Lambda
        return new RepresentationModelProcessor<EntityModel<User>>() {

            @Override
            public EntityModel<User> process(EntityModel<User> entityModel) {
                addUserLinks(entityModel, entityModel.getContent());
                entityModel.add(entityModel.getLink("user").get().expand(EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME).withRel(EnhancedUserProjection.ENHANCED_USER_PROJECTION_NAME));
                return entityModel;
            }
        };
    }

    @Bean
    public RepresentationModelProcessor<EntityModel<EnhancedUserProjection>> enhancedUserProjectionRepresentationModel() {
        // noinspection Convert2Lambda
        return new RepresentationModelProcessor<EntityModel<EnhancedUserProjection>>() {
            @Override
            public EntityModel<EnhancedUserProjection> process(EntityModel<EnhancedUserProjection> resource) {
                addUserLinks(resource, userRestResource.findById(resource.getContent().getId()).get());
                return resource;
            }
        };
    }

    private void addUserLinks(EntityModel<?> resource, User user) {
        resource.add(
                WebMvcLinkBuilder.linkTo(
                        methodOn(UserRestController.class)
                                .resendInvite(user.getId()))
                        .withRel("resend-invite"));
        resource.add(
                linkTo(
                        methodOn(AssignRoleController.class)
                                .assignAdminRole(
                                        organizationRestResource.findByUsers(user).map(Organization::getId).orElse(null),
                                        user.getId()))
                        .withRel("assignAdminRole"));
        resource.add(
                linkTo(
                        methodOn(AssignRoleController.class)
                                .removeAdminRole(
                                        organizationRestResource.findByUsers(user).map(Organization::getId).orElse(null),
                                        user.getId()))
                        .withRel("removeAdminRole"));
    }

    static class VarsBuilder {

        private Map<String, Object> vars = newHashMap();

        private VarsBuilder() {}

        public static VarsBuilder linkVars() {
            return new VarsBuilder();
        }

        public VarsBuilder with(String key, Object value) {
            this.vars.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            return vars;
        }
    }
}
