import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Map;

import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;

public class RestApiTest extends BaseTest {

	private static final String PING_METHOD = "/ping/";
	private static final String AUTH_METHOD = "/authorize/";
	private static final String SAVE_DATA_METHOD = "/api/save_data/";

	private Response response;

	@Test
	public void testPing() {
		step("Executing 'GET " + PING_METHOD + "'");
		response = given().spec(requestSpecification).when().get(PING_METHOD);
		step("Checking status code");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
	}

	@Test (dependsOnMethods = "testPing", priority = 1)
	public void testSuccessAuth() {
		step("Executing 'POST " + AUTH_METHOD + "'");
		response = given().spec(requestSpecification).contentType(ContentType.URLENC)
			.formParam("username", LOGIN).formParam("password", PASSWORD).post(AUTH_METHOD);
		step("Checking status code");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
		step("Grabbing bearer token");
		token = "Bearer " + response.jsonPath().getString("token");
		tokenExpirationTime = System.currentTimeMillis() + 60_000;
	}

	@Test (dependsOnMethods = "testSuccessAuth", priority = 2, dataProvider = "IncorrectCredentials")
	public void testFailAuth(String login, String password) {
		step("Executing 'POST " + AUTH_METHOD
			+ "' with invalid credentials '" + login + "' and password '" + password + "'");
		response = given().spec(requestSpecification).contentType(ContentType.URLENC)
			.formParam("username", login).formParam("password", password)
			.when().post(AUTH_METHOD);
		step("Verifying that status code is '" + HttpStatus.SC_FORBIDDEN + "'");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
	}

	@DataProvider(name = "IncorrectCredentials")
	public static Object[][] incorrectCredentials() {
		return new Object[][] {
			{"testsuper", "passwordsuper"},
			{"supertest", "passwordsuper"},
			{"testsuper", "superpassword"},
			{"", ""}};
	}

	@Test(dependsOnMethods = "testSuccessAuth", priority = 3, dataProvider = "Payload")
	public void testSavingDataJsonWithLength(String payload) throws NoSuchAlgorithmException, SQLException {
		step("Executing 'POST " + SAVE_DATA_METHOD + "' with JSON payload '" + payload + "'");
		response = given()
			.spec(requestSpecification)
			.contentType(ContentType.JSON)
			.header("Authorization", token)
			.body("{\"payload\": \"" + payload + "\"}")
			.post(SAVE_DATA_METHOD);
		step("Verifying that status code is '" + HttpStatus.SC_OK + "'");
		response.then().statusCode(HttpStatus.SC_OK);
		if (response.then().extract().path("status").equals("OK")) {
			String id = Integer.toString(response.then().extract().path("id"));
			Map<String, String> storedValues = dbHelper.getStoredValues(id);
			step("Verifying that payload md5 hash was stored in the database with id '" + id + "'");
			Assertions.assertThat(storedValues.get("login")).isEqualToIgnoringCase(LOGIN);
			Assertions.assertThat(storedValues.get("payload")).isEqualToIgnoringCase(tdHelper.getEncryptedPayload(payload));
		} else {
			Assertions.fail("Service says: '" + response.then().extract().path("error") + "'");
		}
	}

	@Test(dependsOnMethods = "testSuccessAuth", priority = 3, dataProvider = "Payload")
	public void testSavingDataUrlencodedWithLength(String payload) throws NoSuchAlgorithmException, SQLException {
		step("Executing 'POST " + SAVE_DATA_METHOD + "' with URL encoded payload '" + payload + "'");
		response = given()
			.spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.header("Authorization", token)
			.body("payload=" + payload)
			.post(SAVE_DATA_METHOD);
		step("Verifying that status code is '" + HttpStatus.SC_OK + "'");
		response.then().statusCode(HttpStatus.SC_OK);
		if (response.then().extract().path("status").equals("OK")) {
			String id = Integer.toString(response.then().extract().path("id"));
			Map<String, String> storedValues = dbHelper.getStoredValues(id);
			step("Verifying that payload md5 hash was stored in the database with id '" + id + "'");
			Assertions.assertThat(storedValues.get("login")).isEqualToIgnoringCase(LOGIN);
			Assertions.assertThat(storedValues.get("payload")).isEqualToIgnoringCase(tdHelper.getEncryptedPayload(payload));
		} else {
			Assertions.fail("Service says: '" + response.then().extract().path("error") + "'");
		}
	}

	@DataProvider(name = "Payload")
	public Object[][] payload() {
		return new Object[][] {
			{tdHelper.genPayload(1)},
			{tdHelper.genPayload(50)}
		};
	}

	@Test(dependsOnMethods = "testSavingDataJsonWithLength", priority = 4, dataProvider = "FailPayloadJSON")
	public void testFailSavingInvalidDataJson(String payload) throws SQLException {
		dbHelper.setDbRowsCount();
		step("Getting database rows count: " + dbHelper.getDbRowsCount());
		step("Executing 'POST " + SAVE_DATA_METHOD + "' with invalid JSON payload");
		response = given().spec(requestSpecification).contentType(ContentType.JSON)
			.header("Authorization", token).body(payload).when().post(SAVE_DATA_METHOD);
		step("Verifying that response code is '" + HttpStatus.SC_BAD_REQUEST + "'");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
		step("Verifying that invalid payload wasn't stored");
		Assertions.assertThat(dbHelper.getDbRowsCount()).isEqualTo(dbHelper.getActualRowsCount());
	}

	@Test(dependsOnMethods = "testSavingDataUrlencodedWithLength", priority = 4, dataProvider = "FailPayloadUrlEncoded")
	public void testFailSavingInvalidDataUrlencoded(String payload) throws SQLException {
		dbHelper.setDbRowsCount();
		step("Getting database rows count: " + dbHelper.getDbRowsCount());
		step("Executing 'POST " + SAVE_DATA_METHOD + "' with invalid URL encoded payload");
		response = given().spec(requestSpecification).contentType(ContentType.URLENC)
			.header("Authorization", token).body(payload).when().post(SAVE_DATA_METHOD);
		step("Verifying that response code is '" + HttpStatus.SC_BAD_REQUEST + "'");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
		step("Verifying that invalid payload wasn't stored");
		Assertions.assertThat(dbHelper.getDbRowsCount()).isEqualTo(dbHelper.getActualRowsCount());
	}

	@DataProvider(name = "FailPayloadJSON")
	public static Object[][] failPayloadJson() {
		return new Object[][]{
			{""},
			{"{\"payload\": \"\"}"}};
	}

	@DataProvider(name = "FailPayloadUrlEncoded")
	public static Object[][] failPayloadUrlEnc() {
		return new Object[][]{
			{""},
			{"payload="}};
	}

	@Test(dependsOnMethods = "testSuccessAuth", priority = 5)
	public void testFailSavingDataIfTokenIsExpired() throws SQLException, InterruptedException {
		dbHelper.setDbRowsCount();
		step("Getting database rows count: " + dbHelper.getDbRowsCount());
		long resumeTime = tokenExpirationTime - System.currentTimeMillis();
		step("Waiting for " + DateFormat.getDateInstance().format(resumeTime) + " to resume test");
		Thread.sleep(resumeTime);
		step("Executing 'POST " + SAVE_DATA_METHOD + "' with valid payload and expired access token");
		response = given().spec(requestSpecification).contentType(ContentType.URLENC)
			.header("Authorization", token).body("payload=" + tdHelper.genPayload(5)).when()
			.post(SAVE_DATA_METHOD);
		step("Verifying that response code is '" + HttpStatus.SC_FORBIDDEN + "'");
		Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
		step("Verifying that invalid payload wasn't stored");
		Assertions.assertThat(dbHelper.getDbRowsCount()).isEqualTo(dbHelper.getActualRowsCount());
	}
}