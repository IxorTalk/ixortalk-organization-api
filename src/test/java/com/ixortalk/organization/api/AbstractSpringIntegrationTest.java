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
package com.ixortalk.organization.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration;
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Roles;
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.Auth0Users;
import com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfo;
import com.ixortalk.organization.api.asset.DeviceId;
import com.ixortalk.organization.api.config.IxorTalkConfigProperties;
import com.ixortalk.organization.api.domain.*;
import com.ixortalk.organization.api.rest.ImageController_UploadImage_IntegrationAndRestDocTest;
import com.ixortalk.organization.api.rest.OrganizationRestResource;
import com.ixortalk.organization.api.rest.RoleRestResource;
import com.ixortalk.organization.api.rest.UserRestResource;
import com.ixortalk.organization.api.rest.docs.RestDocDescriptors;
import com.ixortalk.organization.api.util.RestResourcesTransactionalHelper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.ixortalk.autoconfigure.oauth2.OAuth2TestConfiguration.buildJwtToken;
import static com.ixortalk.autoconfigure.oauth2.TokenServerWireMockHelper.stubAdminToken;
import static com.ixortalk.autoconfigure.oauth2.auth0.mgmt.api.UserInfoTestBuilder.aUserInfo;
import static com.ixortalk.organization.api.TestConstants.CUSTOM_CLAIMS_NAMESPACE;
import static com.ixortalk.organization.api.TestConstants.IMAGE_SERVICE_CONTEXT_PATH;
import static com.ixortalk.organization.api.TestConstants.IMAGE_SERVICE_DOWNLOAD_PATH;
import static com.ixortalk.organization.api.TestConstants.LOADBALANCER_EXTERNAL_URL;
import static com.ixortalk.organization.api.asset.DeviceId.deviceId;
import static com.ixortalk.organization.api.config.TestConstants.ADMIN_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.OTHER_USER_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.OTHER_USER_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.Role.ORGANIZATION_X_ADMIN;
import static com.ixortalk.organization.api.config.TestConstants.Role.ORGANIZATION_Y_ADMIN;
import static com.ixortalk.organization.api.config.TestConstants.USER_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_ID;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_ID;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_ID;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_ID;
import static com.ixortalk.organization.api.config.TestConstants.USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_JWT_TOKEN;
import static com.ixortalk.organization.api.config.TestConstants.USER_WITHOUT_ROLES_EMAIL;
import static com.ixortalk.organization.api.config.TestConstants.USER_WITHOUT_ROLES_ID;
import static com.ixortalk.organization.api.config.TestConstants.USER_WITHOUT_ROLES_JWT_TOKEN;
import static com.ixortalk.organization.api.domain.RoleTestBuilder.aRole;
import static com.ixortalk.organization.api.domain.UserTestBuilder.aUser;
import static com.ixortalk.test.util.Randomizer.nextString;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory.HTTPS_SCHEME;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static wiremock.com.google.common.net.HttpHeaders.X_FORWARDED_HOST;
import static wiremock.com.google.common.net.HttpHeaders.X_FORWARDED_PORT;
import static wiremock.com.google.common.net.HttpHeaders.X_FORWARDED_PROTO;

@SpringBootTest(classes = {OrganizationApiApplication.class, OAuth2TestConfiguration.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@RunWith(SpringRunner.class)
@AutoConfigureRestDocs
public abstract class AbstractSpringIntegrationTest  {

    protected static final String HOST_IXORTALK_COM = "www.ixortalk.com";

    protected static final String IMAGE_DOWNLOAD_URL_PREFIX = LOADBALANCER_EXTERNAL_URL.configValue() + IMAGE_SERVICE_CONTEXT_PATH.configValue() + IMAGE_SERVICE_DOWNLOAD_PATH.configValue() + "/";

    protected static final String ORGANIZATION_X = "Organization X";
    protected static final String ORGANIZATION_Y = "Organization Y";

    protected static final String USER_IN_ORGANIZATION_X_CREATED_EMAIL = nextString("user-created@organiation-x.com");
    protected static final String USER_IN_ORGANIZATION_X_AND_Y_EMAIL = nextString("user@organization-x-and-y.com");

    protected static final String USER_IN_ORGANIZATION_Y_EMAIL = nextString("user@organization-y.com");

    protected static final String USER_IN_ORGANIZATION_X_ACCEPTED_FIRST_NAME = "First name Accepted User in Organization X";
    protected static final String USER_IN_ORGANIZATION_X_ACCEPTED_LAST_NAME = "Last name Accepted User in Organization X";

    protected static final String FIRST_ROLE_IN_ORGANIZATION_X = "First role in Organization X";
    protected static final String FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME = "FIRST_ROLE_IN_ORG_X_ROLE_NAME";

    protected static final String SECOND_ROLE_IN_ORGANIZATION_X = "Second role in Organization X";
    protected static final String SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME = "SECOND_ROLE_IN_ORG_X_ROLE_NAME";

    protected static final String ROLE_IN_ORGANIZATION_Y = "roleInOrganizationY";
    protected static final String ROLE_IN_ORGANIZATION_Y_ROLE_NAME = "ROLE_IN_ORG_Y_ROLE_NAME";

    protected static final String ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME = ORGANIZATION_X_ADMIN.roleName();
    protected static final String ADMIN_ROLE_IN_ORGANIZATION_Y_ROLE_NAME = ORGANIZATION_Y_ADMIN.roleName();

    protected static final PathParametersSnippet DEVICE_IN_ORGANIZATION_PATH_PARAMETERS = pathParameters(
            parameterWithName("id").description("The id of the organization."),
            parameterWithName("deviceId").description("The known `deviceId` for the device where actions will be saved to.")
    );

    protected static final String ROLE_ONLY_IN_AUTH0 = "someRoleInAuth0";

    protected static final String USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME = "userInOrganizationXAdminRoleFirstName";
    protected static final String USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME = "userInOrganizationXAdminRoleLastName";

    protected static final String USER_IN_ORGANIZATION_Y_ADMIN_ROLE_FIRST_NAME = "userInOrganizationYAdminRoleFirstName";
    protected static final String USER_IN_ORGANIZATION_Y_ADMIN_ROLE_LAST_NAME = "userInOrganizationYAdminRoleLastName";

    protected static final ParameterDescriptor SORT_REQUEST_PARAM_DESCRIPTION = parameterWithName("sort").description("Indicates the field to use for sorting, optionally append `,asc`,`,desc`.");
    protected static final ParameterDescriptor PAGE_SIZE_REQUEST_PARAM_DESCRIPTION = parameterWithName("size").description("Optional parameter to overrule default page size.");
    public static final String USER_IN_ORGANIZATION_X_ACCEPTED_PROFILE_PICTURE_URL = "https://user-in-organization-x-accepted-profile-picture";
    protected static final DeviceId TEST_DEVICE = deviceId(nextString("testDevice"));

    public static final int TOKEN_SERVER_PORT = 65300;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Rule
    public WireMockRule tokenServerWireMockRule = new WireMockRule(TOKEN_SERVER_PORT);

    @Rule
    public WireMockRule organizationCallbackApiWireMockRule = new WireMockRule(65301);

    @Rule
    public WireMockRule assetMgmtWireMockRule = new WireMockRule(65303);

    @Rule
    public WireMockRule mailingServiceWireMockRule = new WireMockRule(65304);

    @Rule
    public WireMockRule imageServiceWireMockRule = new WireMockRule(65305);

    @LocalServerPort
    protected int port;

    @Value("${server.servlet.context-path}")
    protected String contextPath;

    @MockBean
    protected Auth0Users auth0Users;

    @MockBean
    protected Auth0Roles auth0Roles;

    @MockBean
    protected JwtDecoder jwtDecoder;

    @Inject
    protected ObjectMapper objectMapper;

    protected ObjectMapper feignObjectMapper;

    @Inject
    protected IxorTalkConfigProperties ixorTalkConfigProperties;

    @Inject
    protected OrganizationRestResource organizationRestResource;

    @Inject
    protected UserRestResource userRestResource;

    @Inject
    protected RoleRestResource roleRestResource;

    @Inject
    protected RestResourcesTransactionalHelper restResourcesTransactionalHelper;

    @Inject
    private CrudRepository<?, ?>[] crudRepositories;

    @Inject
    private FeignContext feignContext;

    @Inject
    protected Clock clock;

    protected Organization organizationX, organizationY;

    protected User userInOrganizationXCreated, userInOrganizationXInvited, userInOrganizationXAcceptedHavingARole, adminInOrganizationX, adminInOrganizationY, userInOrganizationY;

    protected Role firstRoleInOrganizationX, secondRoleInOrganizationX, roleInOrganizationY;

    protected int organizationXInitialNumberOfUsers, organizationYInitialNumberOfUsers;

    public byte[] originalImageBytes;

    protected static UriModifyingOperationPreprocessor staticUris() {
        return modifyUris().scheme(HTTPS_SCHEME).host(HOST_IXORTALK_COM).removePort();
    }

    @Deprecated
    /**
     * @deprecated Use a {@link RestDocDescriptors.TokenHeaderDescriptors} instead
     */
    public static HeaderDescriptor describeAuthorizationTokenHeader() {
        return headerWithName("Authorization").description("The bearer token needed to authorize this request.");
    }

    @Before
    public void setupTokenStub() {
        stubAdminToken(tokenServerWireMockRule);
    }

    @Before
    public void setupRestAssuredAndRestDocSpec() {

        RestAssured.port = port;
        RestAssured.basePath = contextPath;
        RestAssured.config =
                config()
                        .objectMapperConfig(objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
                        .redirect(redirectConfig().followRedirects(false));
        RestAssured.requestSpecification =
                new RequestSpecBuilder()
                        .addFilter(documentationConfiguration(this.restDocumentation))
                        .addHeader(X_FORWARDED_PROTO, HTTPS_SCHEME)
                        .addHeader(X_FORWARDED_HOST, HOST_IXORTALK_COM)
                        .addHeader(X_FORWARDED_PORT, "")
                        .build();
    }

    @Before
    public void jwtSetUp() {
        when(jwtDecoder.decode(ADMIN_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(ADMIN_JWT_TOKEN, empty(), "admin", "ROLE_ADMIN"));
        when(jwtDecoder.decode(USER_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_JWT_TOKEN, of(USER_EMAIL), "user", "ROLE_USER"));
        when(jwtDecoder.decode(OTHER_USER_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(OTHER_USER_JWT_TOKEN, of(OTHER_USER_EMAIL), "user", "ROLE_USER"));
        when(jwtDecoder.decode(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_IN_ORGANIZATION_X_ADMIN_ROLE_JWT_TOKEN, of(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL), USER_IN_ORGANIZATION_X_ADMIN_ROLE_ID, ORGANIZATION_X_ADMIN.roleName()));
        when(jwtDecoder.decode(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_JWT_TOKEN, of(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL), USER_IN_ORGANIZATION_Y_ADMIN_ROLE_ID, ORGANIZATION_Y_ADMIN.roleName()));
        when(jwtDecoder.decode(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_IN_ORGANIZATION_X_INVITED_JWT_TOKEN, of(USER_IN_ORGANIZATION_X_INVITED_EMAIL), USER_IN_ORGANIZATION_X_INVITED_ID));
        when(jwtDecoder.decode(USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_IN_ORGANIZATION_X_ACCEPTED_JWT_TOKEN, of(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL), USER_IN_ORGANIZATION_X_ACCEPTED_ID));
        when(jwtDecoder.decode(USER_WITHOUT_ROLES_JWT_TOKEN)).thenReturn(buildJwtTokenWithEmailCustomClaim(USER_WITHOUT_ROLES_JWT_TOKEN, of(USER_WITHOUT_ROLES_EMAIL), USER_WITHOUT_ROLES_ID));
    }

    public static Jwt buildJwtTokenWithEmailCustomClaim(String jwtToken, Optional<String> email, String subject, String... roles) {
        return buildJwtToken(
                jwtToken,
                subject,
                email.map(value -> singletonMap(CUSTOM_CLAIMS_NAMESPACE.configValue() + "email", value)).orElse(newHashMap()),
                roles);
    }

    @After
    public void resetRestAssuredDefaultRequestSpecification() {
        RestAssured.requestSpecification = null;
    }

    @Before
    public void feign() {
        feignObjectMapper =
                feignContext.getInstance(
                        feignContext.getContextNames().stream().findAny().orElseThrow(() -> new IllegalStateException("Expected at least one feign context")),
                        ObjectMapper.class);
    }

    @Before
    public void setupOrganizationXAndY() {

        userInOrganizationXCreated = aUser().withLogin(USER_IN_ORGANIZATION_X_CREATED_EMAIL).withStatus(Status.CREATED).build();
        userInOrganizationXInvited = aUser().withLogin(USER_IN_ORGANIZATION_X_INVITED_EMAIL).withStatus(Status.INVITED).build();
        userInOrganizationXAcceptedHavingARole = aUser().withLogin(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL).withStatus(Status.ACCEPTED).build();
        adminInOrganizationX = aUser().withLogin(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL).withStatus(Status.ACCEPTED).withIsAdmin(true).build();

        firstRoleInOrganizationX = aRole().withName(FIRST_ROLE_IN_ORGANIZATION_X).withRole(FIRST_ROLE_IN_ORGANIZATION_X_ROLE_NAME).build();
        secondRoleInOrganizationX = aRole().withName(SECOND_ROLE_IN_ORGANIZATION_X).withRole(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME).build();

        organizationX =
                OrganizationTestBuilder.anOrganization()
                        .withName(ORGANIZATION_X)
                        .withRole(ORGANIZATION_X_ADMIN.roleName())
                        .withPhoneNumber("+32 15 43 43 67")
                        .withEmailAddress("info@organization_x.com")
                        .withImage("organizations/1000/image/abcde")
                        .withLogo("organizations/1000/logo/abcde")
                        .withUsers(
                                adminInOrganizationX,
                                userInOrganizationXCreated,
                                userInOrganizationXInvited,
                                userInOrganizationXAcceptedHavingARole,
                                aUser().withLogin(USER_IN_ORGANIZATION_X_AND_Y_EMAIL).withStatus(Status.INVITED).build())
                        .withRoles(
                                firstRoleInOrganizationX,
                                secondRoleInOrganizationX
                        )
                        .build();

        organizationXInitialNumberOfUsers = organizationX.getUsers().size();

        adminInOrganizationY = aUser().withLogin(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL).withStatus(Status.ACCEPTED).withIsAdmin(true).build();
        userInOrganizationY = aUser().withLogin(USER_IN_ORGANIZATION_Y_EMAIL).build();

        roleInOrganizationY = aRole().withName(ROLE_IN_ORGANIZATION_Y).withRole(ROLE_IN_ORGANIZATION_Y_ROLE_NAME).build();

        organizationY =
                OrganizationTestBuilder.anOrganization()
                        .withName(ORGANIZATION_Y)
                        .withRole(ORGANIZATION_Y_ADMIN.roleName())
                        .withPhoneNumber("+32 15 43 43 67")
                        .withEmailAddress("info@organization_y.com")
                        .withImage("organizations/1000/image/abcde")
                        .withLogo("organizations/1000/logo/abcde")
                        .withUsers(
                                aUser().withLogin(USER_IN_ORGANIZATION_X_AND_Y_EMAIL).withStatus(Status.ACCEPTED).build(),
                                adminInOrganizationY,
                                userInOrganizationY
                        )
                        .withRoles(
                                roleInOrganizationY
                        )
                        .build();

        organizationYInitialNumberOfUsers = organizationY.getUsers().size();

        organizationRestResource.saveAll(newArrayList(organizationX, organizationY));
    }

    @Before
    public void setupUserRoles() {
        setField(userInOrganizationXAcceptedHavingARole, "roles", newArrayList(secondRoleInOrganizationX));
        userRestResource.save(userInOrganizationXAcceptedHavingARole);
    }

    @Before
    public void auth0MockedCalls() {
        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(of(
                aUserInfo()
                        .withEmail(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)
                        .withFirstName(USER_IN_ORGANIZATION_X_ADMIN_ROLE_FIRST_NAME)
                        .withLastName(USER_IN_ORGANIZATION_X_ADMIN_ROLE_LAST_NAME)
                        .withProfilePictureUrl("https://user-in-organization-x-admin-role-profile-picture")
                        .build()));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL)).thenReturn(newHashSet(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_CREATED_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_CREATED_EMAIL)).thenReturn(of(
                aUserInfo()
                        .withEmail(USER_IN_ORGANIZATION_X_CREATED_EMAIL)
                        .withFirstName(nextString("userInOrganizationXCreatedFirstName"))
                        .withLastName(nextString("userInOrganizationXCreatedLastName"))
                        .withProfilePictureUrl("https://user-in-organization-x-created-profile-picture")
                        .build()));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_CREATED_EMAIL)).thenReturn(newHashSet());

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(of(
                aUserInfo()
                        .withEmail(USER_IN_ORGANIZATION_X_INVITED_EMAIL)
                        .withFirstName(nextString("userInOrganizationXInvitedFirstName"))
                        .withLastName(nextString("userInOrganizationXInvitedLastName"))
                        .withProfilePictureUrl("https://user-in-organization-x-invited-profile-picture")
                        .build()));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_INVITED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0));

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(of(
                aUserInfo()
                        .withEmail(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)
                        .withFirstName(USER_IN_ORGANIZATION_X_ACCEPTED_FIRST_NAME)
                        .withLastName(USER_IN_ORGANIZATION_X_ACCEPTED_LAST_NAME)
                        .withProfilePictureUrl(USER_IN_ORGANIZATION_X_ACCEPTED_PROFILE_PICTURE_URL)
                        .build()));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL)).thenReturn(newHashSet(ROLE_ONLY_IN_AUTH0, SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME));

        when(auth0Users.userExists(USER_IN_ORGANIZATION_X_AND_Y_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_X_AND_Y_EMAIL)).thenReturn(of(new UserInfo(USER_IN_ORGANIZATION_X_AND_Y_EMAIL)));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_X_AND_Y_EMAIL)).thenReturn(newHashSet());

        when(auth0Users.userExists(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL)).thenReturn(of(
                aUserInfo()
                        .withEmail(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL)
                        .withFirstName(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_FIRST_NAME)
                        .withLastName(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_LAST_NAME)
                        .withProfilePictureUrl("https://user-in-organization-y-admin-role-profile-picture")
                        .build()));
        when(auth0Roles.getUsersRoles(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL)).thenReturn(newHashSet(ADMIN_ROLE_IN_ORGANIZATION_Y_ROLE_NAME));

        when(auth0Users.userExists(USER_EMAIL)).thenReturn(true);
        when(auth0Users.getUserInfo(USER_EMAIL)).thenReturn(of(new UserInfo(USER_EMAIL)));
        when(auth0Roles.getUsersRoles(USER_EMAIL)).thenReturn(newHashSet());

        when(auth0Roles.getUsersInRole(ADMIN_ROLE_IN_ORGANIZATION_X_ROLE_NAME)).thenReturn(newHashSet(USER_IN_ORGANIZATION_X_ADMIN_ROLE_EMAIL));
        when(auth0Roles.getUsersInRole(ROLE_ONLY_IN_AUTH0)).thenReturn(newHashSet(USER_IN_ORGANIZATION_X_INVITED_EMAIL, USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL));
        when(auth0Roles.getUsersInRole(SECOND_ROLE_IN_ORGANIZATION_X_ROLE_NAME)).thenReturn(newHashSet(USER_IN_ORGANIZATION_X_ACCEPTED_EMAIL));
        when(auth0Roles.getUsersInRole(ADMIN_ROLE_IN_ORGANIZATION_Y_ROLE_NAME)).thenReturn(newHashSet(USER_IN_ORGANIZATION_Y_ADMIN_ROLE_EMAIL));
    }

    @Before
    public void setupImages() throws IOException {
        originalImageBytes = toByteArray(getClass().getClassLoader().getResourceAsStream("test-images/" + ImageController_UploadImage_IntegrationAndRestDocTest.ORIGINAL_IMAGE_FILE_NAME));
    }

    @After
    public void cleanCrudRepositories() {
        userRestResource.deleteAll();
        stream(crudRepositories).forEach(CrudRepository::deleteAll);
    }

    protected String constructFullUri(String uri) throws MalformedURLException {
        return constructFullUri(uri, this.contextPath);
    }

    protected String constructFullUri(String uri, String contextPath) throws MalformedURLException {
        return new URL(HTTPS_SCHEME, HOST_IXORTALK_COM, contextPath + uri).toString();
    }

    protected User convertToHowItShouldBeSentToMailingService(User user) {
        ReflectionTestUtils.setField(user, "status", Status.INVITED);
        setField(user, "roles", null);
        return user;
    }
    
    protected OperationPreprocessor removeBinaryContent() {
        return new ContentModifyingOperationPreprocessor((originalContent, contentType) -> "<theBinaryContent>".getBytes());
    }

    protected static Map<String, Object> assetMgmtFieldsToBeCleared() {
        Map<String, Object> fields = newHashMap();
        fields.put("organizationId", null);
        fields.put("deviceName", "");
        fields.put("deviceInformation", "");
        fields.put("actions", newArrayList());
        fields.put("image", "");
        return fields;
    }
}

