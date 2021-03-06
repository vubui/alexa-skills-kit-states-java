/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import io.klerch.alexa.state.handler.AlexaSessionStateHandler;
import io.klerch.alexa.state.handler.AlexaStateHandler;
import io.klerch.alexa.state.model.dummies.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AlexaStateModelTest {
    private Session session;
    private final String attributeKey = "key";

    @Before
    public void createSession() {
        final Application application = new Application("applicationId");
        final User user = User.builder().withUserId("userId").withAccessToken("accessToken").build();
        session = Session.builder().withSessionId("sessionId")
                .withApplication(application).withUser(user).build();
        session.setAttribute(attributeKey, "value");
    }

    @Test
    public void getSetValidId() throws Exception {
        final String id = "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789";
        final Model model = new Model();
        model.setId(id);
        assertEquals(id, model.getId());
    }

    public void getSetNullId() throws Exception {
        final Model model = new Model();
        model.setId(null);
        Assert.assertNull(model.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSetInvalidId() {
        final String id = "abcd/f";
        final Model model = new Model();
        model.setId(id);
    }

    @Test
    public void getSetHandler() throws Exception {
        final AlexaStateHandler handler = new AlexaSessionStateHandler(session);
        final Model model = new Model();
        model.setHandler(handler);
        Assert.assertEquals(handler, model.getHandler());
    }

    @Test
    public void withHandler() throws Exception {
        final AlexaStateHandler handler = new AlexaSessionStateHandler(session);
        final Model model1 = new Model();
        final AlexaStateModel model2 = model1.withHandler(handler);
        assertEquals(model1, model2);
        Assert.assertEquals(handler, model1.getHandler());
    }

    @Test (expected = NullPointerException.class)
    public void saveStateWithoutHandler() throws Exception {
        new Model().saveState();
    }

    @Test
    public void saveAndRemoveStateWithHandler() throws Exception {
        final Model model = new Model();
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        model.withHandler(handler).saveState();
        Assert.assertNotNull(session.getAttribute(model.getAttributeKey()));
        model.removeState();
        Assert.assertNull(session.getAttribute(model.getAttributeKey()));
    }

    @Test (expected = NullPointerException.class)
    public void removeStateWithoutHandler() throws Exception {
        new Model().removeState();
    }

    @Test
    public void createWithHandler() throws Exception {
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        Model model = AlexaStateModel.create(Model.class).withHandler(handler).build();
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getHandler(), handler);
    }

    @Test
    public void createWithHandlerAndId() throws Exception {
        final String id = "id";
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        Model model = AlexaStateModel.create(Model.class).withHandler(handler).withId(id).build();
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getHandler(), handler);
        assertEquals(model.getId(), id);
    }

    @Test (expected = NullPointerException.class)
    public void createWithoutHandler() throws Exception {
        AlexaStateModel.create(Model.class).build();
    }

    @Test
    public void getExisting() throws Exception {
        final Model model = new Model();
        final String value = "value";
        model.sampleString = value;
        final Field field = model.getClass().getField("sampleString");
        assertEquals(model.get(field), value);
    }

    @Test
    public void getSetPrivate() throws Exception {
        final Model model = new Model();
        final String value = "value";
        final Field field = model.getClass().getDeclaredField("privateField");
        model.set(field, value);
        assertEquals(model.get(field), value);
    }

    @Test
    public void setExisting() throws Exception {
        final Model model = new Model();
        final String value = "value";
        final Field field = model.getClass().getField("sampleString");
        model.set(field, value);
        assertEquals(model.sampleString, value);
    }

    @Test
    public void fromJSON() throws Exception {
        final String value = "value";
        final String json = "{\"sampleString\":\"" + value + "\"}";
        final Model model = new Model();
        model.fromJSON(json);
        assertEquals(model.sampleString, value);
    }

    @Test
    public void fromJSONInScope() throws Exception {
        final String value = "value";
        final String json = "{\"sampleString\":\"" + value + "\", \"users\": [{\"field\" : \"test1\"}, {\"field\" : \"test1\"}]}";
        final Model model = new Model();
        model.fromJSON(json, AlexaScope.SESSION);
        assertEquals(model.sampleString, value);
        assertTrue(model.users.stream().allMatch(modelUser -> modelUser.getField().equals("test1")));
    }

    @Test
    public void fromJSONWithOutOfScope() throws Exception {
        final String value = "value";
        final String json = "{\"sampleString\":\"" + value + "\"}";
        final Model model = new Model();
        model.fromJSON(json, AlexaScope.APPLICATION);
        assertNull(model.sampleString);
    }

    @Test
    public void toJSONInScope() throws Exception {
        final String value = "value";
        final String json = "{\"id\":null,\"sampleString\":\"" + value + "\",\"sampleUser\":null,\"sampleApplication\":false,\"sampleSession\":[],\"users\":[]}";
        final Model model = new Model();
        model.sampleString = value;
        assertEquals(model.toJSON(AlexaScope.SESSION), json);
    }

    @Test
    public void toJSONOutOfScope() throws Exception {
        final String json = "{\"id\":null,\"sampleApplication\":false}";
        final Model model = new Model();
        assertEquals(model.toJSON(AlexaScope.APPLICATION), json);
    }

    @Test
    public void toMapInScope() throws Exception {
        final String value = "value";
        final Model model = new Model();
        model.sampleString = value;
        final Map<String, Object> map = model.toMap(AlexaScope.SESSION);
        assertTrue(map.containsKey("sampleString"));
        assertFalse(map.containsKey("sampleIgnore"));
        assertEquals(value, map.get("sampleString"));
    }

    @Test
    public void toMapOutOfScope() throws Exception {
        final String value = "value";
        final Model model = new Model();
        model.sampleString = value;
        final Map<String, Object> map = model.toMap(AlexaScope.APPLICATION);
        assertFalse(map.containsKey("sampleString"));
        assertFalse(map.containsKey("sampleIgnore"));
    }

    @Test
    public void hasScopedFieldsTrue() throws Exception {
        assertTrue(new Model().hasSessionScopedField());
        assertTrue(new Model().hasApplicationScopedField());
        assertTrue(new Model().hasUserScopedField());
        assertTrue(new ModelUser().hasSessionScopedField());
        assertTrue(new ModelUser().hasUserScopedField());
    }

    @Test
    public void hasScopedFieldsFalse() throws Exception {
        assertFalse(new EmptyModel().hasSessionScopedField());
        assertFalse(new EmptyModel().hasApplicationScopedField());
        assertFalse(new EmptyModel().hasUserScopedField());
        assertFalse(new ModelUser().hasApplicationScopedField());
    }

    @Test
    public void getSaveStateFieldsWithSessionModel() throws Exception {
        List<Field> fields = new SessionModel().getSaveStateFields();
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());

        fields = new SessionModel().getSaveStateFields(AlexaScope.SESSION);
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());

        fields = new SessionModel().getSaveStateFields(AlexaScope.APPLICATION);
        assertTrue(fields.isEmpty());

        fields = new SessionModel().getSaveStateFields(AlexaScope.USER);
        assertTrue(fields.isEmpty());
    }

    @Test
    public void getSaveStateFieldsWithUserModel() throws Exception {
        List<Field> fields = new UserModel().getSaveStateFields();
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());

        fields = new UserModel().getSaveStateFields(AlexaScope.SESSION);
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());

        fields = new UserModel().getSaveStateFields(AlexaScope.APPLICATION);
        assertTrue(fields.isEmpty());

        fields = new UserModel().getSaveStateFields(AlexaScope.USER);
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
    }

    @Test
    public void getSaveStateFieldsWithApplicationModel() throws Exception {
        List<Field> fields = new ApplicationModel().getSaveStateFields();
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());

        fields = new ApplicationModel().getSaveStateFields(AlexaScope.SESSION);
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());

        fields = new ApplicationModel().getSaveStateFields(AlexaScope.USER);
        assertTrue(fields.isEmpty());

        fields = new ApplicationModel().getSaveStateFields(AlexaScope.APPLICATION);
        // should contain
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleString")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreSession")).findAny().isPresent());
        assertTrue(fields.stream().filter(x -> x.getName().equals("sampleIgnoreUser")).findAny().isPresent());
        // should not contain
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnoreApplication")).findAny().isPresent());
        assertFalse(fields.stream().filter(x -> x.getName().equals("sampleIgnore")).findAny().isPresent());
    }
}