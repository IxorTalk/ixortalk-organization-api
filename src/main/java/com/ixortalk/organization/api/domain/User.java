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
package com.ixortalk.organization.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ixortalk.organization.api.domain.validation.LanguageISO639;
import org.springframework.data.annotation.ReadOnlyProperty;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.ixortalk.organization.api.domain.Status.*;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.EnumType.STRING;

@Entity
@Table(name = "org_user")
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String login;

    @LanguageISO639
    private String inviteLanguage;

    @ManyToMany(cascade = ALL)
    @JoinTable(name = "org_role_in_user",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<Role> roles = newArrayList();

    @ReadOnlyProperty
    @Enumerated(STRING)
    private Status status = CREATED;

    @Embedded
    private AcceptKey acceptKey;

    private boolean isAdmin = false;

    @Column(name = "organization_id", updatable = false)
    private Long organizationId;

    private User() {
    }

    public static User initialOrganizationAdminUser(String login) {
        User newUser = new User().accepted();
        newUser.login = login;
        newUser.setAdmin(true);
        return newUser;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getInviteLanguage() {
        return inviteLanguage;
    }

    public Status getStatus() {
        return status;
    }

    public List<Role> getRoles() {
        return roles;
    }

    @JsonIgnore
    public AcceptKey getAcceptKey() {
        return acceptKey;
    }

    public User accepted() {
        this.status = ACCEPTED;
        this.acceptKey = null;
        return this;
    }

    public User invited(Instant now) {
        this.status = INVITED;
        this.acceptKey = AcceptKey.generateAcceptKey(now);
        return this;
    }

    @JsonIgnore
    public boolean isAccepted() {
        return this.status == ACCEPTED;
    }

    @JsonIgnore
    public boolean isInvited() {
        return this.status == INVITED;
    }

    @JsonIgnore
    public boolean isCreated() {
        return this.status == CREATED;
    }

    @JsonProperty(value="isAdmin")
    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void loginToLowercase() {
        this.login = this.login.toLowerCase();
    }
}
