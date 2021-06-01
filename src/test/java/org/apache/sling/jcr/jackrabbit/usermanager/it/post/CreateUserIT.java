/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.usermanager.it.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * Tests for the 'createUser' Sling Post Operation
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CreateUserIT extends UserManagerClientTestSupport {

    /*
        <form action="/system/userManager/user.create.html" method="POST">
           <div>Name: <input type="text" name=":name" value="testUser" /></div>
           <div>Password: <input type="text" name="pwd" value="testUser" /></div>
           <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
           <input type="submit" value="Submit" />
        </form>
     */
    @Test
    public void testCreateUser() throws IOException, JsonException {
        testUserId = "testUser" + getNextInt();
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);
        final List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("marker", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        final Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        // fetch the user profile json to verify the settings
        String getUrl = String.format("%s/system/userManager/user/%s.json", baseServerUri, testUserId);
        final String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, HttpServletResponse.SC_OK);
        assertNotNull(json);
        final JsonObject jsonObj = parseJson(json);
        assertEquals(testUserId, jsonObj.getString("marker"));
        assertFalse(jsonObj.containsKey(":name"));
        assertFalse(jsonObj.containsKey("pwd"));
        assertFalse(jsonObj.containsKey("pwdConfirm"));

        // fetch the session info to verify that the user can log in
        final Credentials newUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
        final String getUrl2 = String.format("%s/system/sling/info.sessionInfo.json", baseServerUri);
        final String json2 = getAuthenticatedContent(newUserCreds, getUrl2, CONTENT_TYPE_JSON, HttpServletResponse.SC_OK);
        assertNotNull(json2);
        final JsonObject jsonObj2 = parseJson(json2);
        assertEquals(testUserId, jsonObj2.getString("userID"));
    }

    @Test
    public void testNotAuthorizedCreateUser() throws IOException, JsonException {
        testUserId2 = createTestUser();

        String testUserId3 = "testUser" + getNextInt();
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);
        final List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId3));
        postParams.add(new BasicNameValuePair("marker", testUserId3));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        final Credentials creds = new UsernamePasswordCredentials(testUserId2, "testPwd");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    @Test
    public void testAuthorizedCreateUser() throws IOException, JsonException {
        testUserId2 = createTestUser();
        grantUserManagementRights(testUserId2);

        testUserId = "testUser" + getNextInt();
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);
        final List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("marker", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        final Credentials creds = new UsernamePasswordCredentials(testUserId2, "testPwd");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        // fetch the user profile json to verify the settings
        final String getUrl = String.format("%s/system/userManager/user/%s.json", baseServerUri, testUserId);
        final String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, HttpServletResponse.SC_OK);
        assertNotNull(json);
        final JsonObject jsonObj = parseJson(json);
        assertEquals(testUserId, jsonObj.getString("marker"));
        assertFalse(jsonObj.containsKey(":name"));
        assertFalse(jsonObj.containsKey("pwd"));
        assertFalse(jsonObj.containsKey("pwdConfirm"));

        // fetch the session info to verify that the user can log in
        final Credentials newUserCreds = new UsernamePasswordCredentials(testUserId, "testPwd");
        final String getUrl2 = String.format("%s/system/sling/info.sessionInfo.json", baseServerUri);
        final String json2 = getAuthenticatedContent(newUserCreds, getUrl2, CONTENT_TYPE_JSON, HttpServletResponse.SC_OK);
        assertNotNull(json2);
        final JsonObject jsonObj2 = parseJson(json2);
        assertEquals(testUserId, jsonObj2.getString("userID"));
    }

    @Test
    public void testCreateUserMissingUserId() throws IOException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        List<NameValuePair> postParams = new ArrayList<>();
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    @Test
    public void testCreateUserMissingPwd() throws IOException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        String userId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", userId));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    @Test
    public void testCreateUserWrongConfirmPwd() throws IOException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        String userId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", userId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd2"));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    @Test
    public void testCreateUserUserAlreadyExists() throws IOException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        testUserId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        //post the same info again, should fail
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }

    /*
    <form action="/system/userManager/user.create.html" method="POST">
       <div>Name: <input type="text" name=":name" value="testUser" /></div>
       <div>Password: <input type="text" name="pwd" value="testUser" /></div>
       <div>Password Confirm: <input type="text" name="pwdConfirm" value="testUser" /></div>
       <div>Extra Property #1: <input type="text" name="displayName" value="My Test User" /></div>
       <div>Extra Property #2: <input type="text" name="url" value="http://www.apache.org" /></div>
       <input type="submit" value="Submit" />
    </form>
    */
    @Test
    public void testCreateUserWithExtraProperties() throws IOException, JsonException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        testUserId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("marker", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        postParams.add(new BasicNameValuePair("displayName", "My Test User"));
        postParams.add(new BasicNameValuePair("url", "http://www.apache.org"));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        assertAuthenticatedPostStatus(creds, postUrl, HttpServletResponse.SC_OK, postParams, null);

        //fetch the user profile json to verify the settings
        String getUrl = String.format("%s/system/userManager/user/%s.json", baseServerUri, testUserId);
        String json = getAuthenticatedContent(creds, getUrl, CONTENT_TYPE_JSON, HttpServletResponse.SC_OK);
        assertNotNull(json);
        JsonObject jsonObj = parseJson(json);
        assertEquals(testUserId, jsonObj.getString("marker"));
        assertEquals("My Test User", jsonObj.getString("displayName"));
        assertEquals("http://www.apache.org", jsonObj.getString("url"));
        assertFalse(jsonObj.containsKey(":name"));
        assertFalse(jsonObj.containsKey("pwd"));
        assertFalse(jsonObj.containsKey("pwdConfirm"));
    }

    /**
     * Test for SLING-1642 to verify that user self-registration by the anonymous
     * user is not allowed by default.
     */
    @Test
    public void testAnonymousSelfRegistrationDisabled() throws IOException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        String userId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", userId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        //user create without logging in as a privileged user should return a 500 error
        assertAuthenticatedPostStatus(null, postUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, postParams, null);
    }


    /**
     * Test for SLING-1677
     */
    @Test
    public void testCreateUserResponseAsJSON() throws IOException, JsonException {
        String postUrl = String.format("%s/system/userManager/user.create.json", baseServerUri);

        testUserId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("marker", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        String json = getAuthenticatedPostContent(creds, postUrl, CONTENT_TYPE_JSON, postParams, HttpServletResponse.SC_OK);

        //make sure the json response can be parsed as a JSON object
        JsonObject jsonObj = parseJson(json);
        assertNotNull(jsonObj);
    }

    /**
     * Test for SLING-7831
     */
    @Test
    public void testCreateUserCustomPostResponse() throws IOException, JsonException {
        String postUrl = String.format("%s/system/userManager/user.create.html", baseServerUri);

        testUserId = "testUser" + getNextInt();
        List<NameValuePair> postParams = new ArrayList<>();
        postParams.add(new BasicNameValuePair(":responseType", "custom"));
        postParams.add(new BasicNameValuePair(":name", testUserId));
        postParams.add(new BasicNameValuePair("pwd", "testPwd"));
        postParams.add(new BasicNameValuePair("pwdConfirm", "testPwd"));
        Credentials creds = new UsernamePasswordCredentials("admin", "admin");
        String content = getAuthenticatedPostContent(creds, postUrl, CONTENT_TYPE_HTML, postParams, HttpServletResponse.SC_OK);
        assertEquals("Thanks!", content); //verify that the content matches the custom response
    }

}
