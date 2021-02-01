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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@ConfigurationProperties(prefix = "ixortalk")
public class IxorTalkConfigProperties {

    private LoadbalancerConfig loadbalancer = new LoadbalancerConfig();

    private Map<String, Microservice> server = newHashMap();

    private Organization organization = new Organization();

    public Map<String, Microservice> getServer() {
        return server;
    }

    public Microservice getMicroservice(String name) {
        return getServer().get(name);
    }

    public Organization getOrganization() {
        return organization;
    }

    public LoadbalancerConfig getLoadbalancer() {
        return loadbalancer;
    }

    public void setLoadbalancer(LoadbalancerConfig loadbalancer) {
        this.loadbalancer = loadbalancer;
    }

    public static class Organization {

        private Api api = new Api();

        private Assetmgmt assetmgmt = new Assetmgmt();

        public Api getApi() {
            return api;
        }

        public Assetmgmt getAssetmgmt() {
            return assetmgmt;
        }

        public static class Api {

            private Mail mail = new Mail();
            private List<String> deviceInfoFields = newArrayList();
            private int acceptKeyMaxAgeInHours = 24;
            private String verifyEmailLandingPagePath = "/";

            public Mail getMail() {
                return mail;
            }

            public List<String> getDeviceInfoFields() {
                return deviceInfoFields;
            }

            public int getAcceptKeyMaxAgeInHours() {
                return acceptKeyMaxAgeInHours;
            }

            public void setAcceptKeyMaxAgeInHours(int acceptKeyMaxAgeInHours) {
                this.acceptKeyMaxAgeInHours = acceptKeyMaxAgeInHours;
            }

            public String getVerifyEmailLandingPagePath() {
                return verifyEmailLandingPagePath;
            }

            public void setVerifyEmailLandingPagePath(String verifyEmailLandingPagePath) {
                this.verifyEmailLandingPagePath = verifyEmailLandingPagePath;
            }

            public static class Mail {

                private String inviteMailSubjectKey = "invite";
                private String inviteMailTemplate = "invite";
                private String verifyMailSubjectKey = "verify";
                private String verifyMailTemplate = "verify";
                private String defaultMailLanguageTag = "en";

                public String getInviteMailSubjectKey() {
                    return inviteMailSubjectKey;
                }

                public void setInviteMailSubjectKey(String inviteMailSubjectKey) {
                    this.inviteMailSubjectKey = inviteMailSubjectKey;
                }

                public String getInviteMailTemplate() {
                    return inviteMailTemplate;
                }

                public void setInviteMailTemplate(String inviteMailTemplate) {
                    this.inviteMailTemplate = inviteMailTemplate;
                }

                public String getVerifyMailSubjectKey() {
                    return verifyMailSubjectKey;
                }

                public void setVerifyMailSubjectKey(String verifyMailSubjectKey) {
                    this.verifyMailSubjectKey = verifyMailSubjectKey;
                }

                public String getVerifyMailTemplate() {
                    return verifyMailTemplate;
                }

                public void setVerifyMailTemplate(String verifyMailTemplate) {
                    this.verifyMailTemplate = verifyMailTemplate;
                }

                public String getDefaultMailLanguageTag() {
                    return defaultMailLanguageTag;
                }

                public void setDefaultMailLanguageTag(String defaultMailLanguageTag) {
                    this.defaultMailLanguageTag = defaultMailLanguageTag;
                }
            }
        }

        public static class Assetmgmt {

            private Map<String, List<String>> allowedSaveCalls = newHashMap();

            public Map<String, List<String>> getAllowedSaveCalls() {
                return allowedSaveCalls;
            }
        }
    }

    public static class LoadbalancerConfig {

        private Loadbalancer internal;
        private Loadbalancer external;

        public Loadbalancer getInternal() {
            return internal;
        }

        public void setInternal(Loadbalancer internal) {
            this.internal = internal;
        }

        public Loadbalancer getExternal() {
            return external;
        }

        public void setExternal(Loadbalancer external) {
            this.external = external;
        }
    }

    public static class Loadbalancer {

        private String url;
        private String protocol;
        private String host;
        private int port;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUrlWithoutStandardPorts() {
            String urlWithoutStandardPorts = this.protocol + "://" + this.host;
            if (!isDefaultPort(this.port)) {
                urlWithoutStandardPorts += ":" + this.port;
            }
            return urlWithoutStandardPorts;
        }

        private boolean isDefaultPort(int port) {
            return port==80 || port==443;
        }
    }

    public static class Microservice {

        private int port;
        private String contextPath;
        private String downloadPath;
        private String url;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }
    }
}
