package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import io.klerch.alexa.state.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AWSDynamoStateHandlerTest {
    private AWSDynamoStateHandler handler;
    private Session session;
    private final String tableName = "tableName";
    private final String modelId = "id";

    @Before
    public void setUp() {
        final Application application = new Application("applicationId");
        final User user = User.builder().withUserId("userId").withAccessToken("accessToken").build();
        session = Session.builder().withSessionId("sessionId")
                .withApplication(application).withUser(user).build();

        final AmazonDynamoDBClient awsClient = mock(AmazonDynamoDBClient.class);
        handler = new AWSDynamoStateHandler(session, awsClient, tableName);

        // prepare static read return from DynamoDB without given model-Id

        final String jsonApp = "{\"id\":null,\"sampleApplication\":true}";
        final String jsonUser = "{\"id\":null,\"sampleUser\":\"sampleUser\"}";

        final Map<String, AttributeValue> mapUser = new HashMap<>();
        mapUser.put(handler.getAttributeKeyState(), new AttributeValue(jsonUser));

        final Map<String, AttributeValue> mapApp = new HashMap<>();
        mapApp.put(handler.getAttributeKeyState(), new AttributeValue(jsonApp));

        final GetItemResult resultUser = new GetItemResult().withItem(mapUser);
        final GetItemResult resultApp = new GetItemResult().withItem(mapApp);

        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class)))
                .thenReturn(resultUser);
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class)))
                .thenReturn(resultApp);

        // prepare static read return from DynamoDB with given model-Id

        final String jsonAppId = "{\"id\":\"" + modelId + "\",\"sampleApplication\":true}";
        final String jsonUserId = "{\"id\":\"" + modelId + "\",\"sampleUser\":\"sampleUser\"}";

        final Map<String, AttributeValue> mapUserId = new HashMap<>();
        mapUserId.put(handler.getAttributeKeyState(), new AttributeValue(jsonUserId));

        final Map<String, AttributeValue> mapAppId = new HashMap<>();
        mapAppId.put(handler.getAttributeKeyState(), new AttributeValue(jsonAppId));

        final GetItemResult resultUserId = new GetItemResult().withItem(mapUserId);
        final GetItemResult resultAppId = new GetItemResult().withItem(mapAppId);

        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultUserId);
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultAppId);
    }

    @Test
    public void getAwsClient() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getAwsClient());

        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, awsClient);
        assertEquals(awsClient, handler2.getAwsClient());
    }

    @Test
    public void getTableName() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getTableName());

        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, "tableName");
        assertEquals("tableName", handler2.getTableName());
    }

    @Test
    public void getAwsClientAndTableName() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getTableName());
        assertNotNull(handler.getAwsClient());

        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, awsClient, "tableName");
        assertEquals(awsClient, handler2.getAwsClient());
        assertEquals("tableName", handler2.getTableName());
    }

    @Test
    public void getAttributeKey() throws Exception {
        final Model model = new Model();
        final String expected = model.getClass().getTypeName();
        assertEquals(expected, handler.getAttributeKey(model));
        assertEquals(expected, handler.getAttributeKey(Model.class, ""));
        assertEquals(expected, handler.getAttributeKey(Model.class, null));

        final String id = "id";
        final String expected2 = expected + ":" + id;
        model.setId(id);
        assertEquals(expected2, handler.getAttributeKey(model));
        assertEquals(expected2, handler.getAttributeKey(Model.class, id));
    }

    @Test
    public void createModel() throws Exception {
        final Model model = handler.createModel(Model.class);
        assertNotNull(model);
        assertNull(model.getId());
        assertEquals(handler, model.getHandler());
    }

    @Test
    public void createModelWithId() throws Exception {
        final String id = "id";
        final Model model = handler.createModel(Model.class, id);
        assertNotNull(model);
        assertEquals(handler, model.getHandler());
        assertEquals(id, model.getId());
    }

    @Test
    public void crudModel() throws Exception {
        session.getAttributes().clear();

        final String value = "value";
        final String valueU = "sampleUser";

        final Model model = new Model();
        model.sampleString = value;
        model.sampleApplication = true;
        model.sampleUser = valueU;

        final String key = handler.getAttributeKey(model);

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked dynamoDB
        session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class);
        assertTrue(model2.isPresent());
        assertNull(model2.get().getId());
        assertNull(model2.get().sampleString);
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }

    @Test
    public void crudModelWithId() throws Exception {
        session.getAttributes().clear();

        final String value = "value";
        final String valueU = "sampleUser";

        final Model model = new Model();
        model.setId(modelId);
        model.sampleString = value;
        model.sampleApplication = true;
        model.sampleUser = valueU;

        final String key = handler.getAttributeKey(model);

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked dynamoDB
        session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class, modelId);
        assertTrue(model2.isPresent());
        assertEquals(modelId, model2.get().getId());
        assertNull(model2.get().sampleString);
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }
}