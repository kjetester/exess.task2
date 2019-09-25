import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class RestApiTest extends BaseTest {

	private static final String PING_METHOD = "/ping/";
	private static final String AUTH_METHOD = "/authorize/";
	private static final String SAVE_DATA_METHOD = "/api/save_data/";

	@Test
	public void testPing() {
		given().spec(requestSpecification)
		.when()
			.get(PING_METHOD)
		.then()
			.statusCode(HttpStatus.SC_OK);
	}

	@Test (
		dependsOnMethods = "testPing",
		priority = 1)
	public void testSuccessAuth() {
		token = "Bearer " + given().spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.formParam("username", LOGIN)
			.formParam("password", PASSWORD)
		.when()
			.post(AUTH_METHOD)
		.then()
			.statusCode(HttpStatus.SC_OK)
			.extract().path("token");
		tokenExpirationTime = System.currentTimeMillis() + 60_000;
	}

	@Test (
		dependsOnMethods = "testSuccessAuth",
		priority = 2,
		dataProvider = "IncorrectCredentials")
	public void testFailAuth(String login, String password) {
		given().spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.formParam("username", login)
			.formParam("password", password)
		.when()
			.post(AUTH_METHOD)
		.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
	}

	@DataProvider(name = "IncorrectCredentials")
	public static Object[][] incorrectCredentials() {
		return new Object[][] {
			{"testsuper", "passwordsuper"},
			{"supertest", "passwordsuper"},
			{"testsuper", "superpassword"},
			{"", ""}};
	}

	@Test(
		dependsOnMethods = "testSuccessAuth",
		priority = 3,
		dataProvider = "PayloadLength"
	)
	public void testSavingDataJsonWithLength(int length) throws NoSuchAlgorithmException, SQLException {
		payload = tdHelper.genPayload(length);
		given()
			.spec(requestSpecification)
			.contentType(ContentType.JSON)
			.header("Authorization", token)
			.body("{\"payload\": \"" + payload + "\"}")
		.when()
			.post(SAVE_DATA_METHOD)
		.then()
			.statusCode(HttpStatus.SC_OK)
			.extract().path("status").equals("OK");
		Map<String, String> storedValues = dbHelper.getStoredValues();
		Assertions.assertThat(storedValues.get("login")).isEqualToIgnoringCase(LOGIN);
		Assertions.assertThat(storedValues.get("payload")).isEqualToIgnoringCase(tdHelper.getEncryptedPayload(payload));
	}

	@Test(
		dependsOnMethods = "testSuccessAuth",
		priority = 3,
		dataProvider = "PayloadLength"
	)
	public void testSavingDataUrlencodedWithLength(int length) throws NoSuchAlgorithmException, SQLException {
		payload = tdHelper.genPayload(length);
		given()
			.spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.header("Authorization", token)
			.body("payload=" + payload)
		.when()
			.post(SAVE_DATA_METHOD)
		.then()
			.statusCode(HttpStatus.SC_OK)
			.extract().path("status").equals("OK");
		Map<String, String> storedValues = dbHelper.getStoredValues();
		Assertions.assertThat(storedValues.get("login")).isEqualToIgnoringCase(LOGIN);
		Assertions.assertThat(storedValues.get("payload")).isEqualToIgnoringCase(tdHelper.getEncryptedPayload(payload));
	}

	@DataProvider(name = "PayloadLength")
	public static Object[][] payloadLength() {
		return new Object[][] {
			{1},
			{50}};
	}

	@Test(
		dependsOnMethods = "testSavingDataJsonWithLength",
		priority = 4,
		dataProvider = "FailPayloadJSON"
	)
	public void testFailSavingInvalidDataJson(String payload) throws SQLException {
		dbHelper.setDbRowsCount();
		given()
			.spec(requestSpecification)
			.contentType(ContentType.JSON)
			.header("Authorization", token)
			.body(payload)
			.when()
			.post(SAVE_DATA_METHOD)
			.then()
			.statusCode(HttpStatus.SC_BAD_REQUEST);
		Assertions.assertThat(dbHelper.getDbRowsCount()).isEqualTo(dbHelper.getActualRowsCount());
	}

	@Test(
		dependsOnMethods = "testSavingDataUrlencodedWithLength",
		priority = 4,
		dataProvider = "FailPayloadUrlEncoded"
	)
	public void testFailSavingInvalidDataUrlencoded(String payload) throws SQLException {
		dbHelper.setDbRowsCount();
		given()
			.spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.header("Authorization", token)
			.body(payload)
			.when()
			.post(SAVE_DATA_METHOD)
			.then()
			.statusCode(HttpStatus.SC_BAD_REQUEST);
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

	@Test(
		dependsOnMethods = "testSavingDataJsonWithLength",
		priority = 5
	)
	public void testFailSavingDataIfTokenIsExpired() throws SQLException, InterruptedException {
		dbHelper.setDbRowsCount();
		payload = tdHelper.genPayload(5);
		Thread.sleep(tokenExpirationTime - System.currentTimeMillis());
		given()
			.spec(requestSpecification)
			.contentType(ContentType.URLENC)
			.header("Authorization", token)
			.body("payload=" + payload)
			.when()
			.post(SAVE_DATA_METHOD)
			.then()
			.statusCode(HttpStatus.SC_FORBIDDEN);
		Assertions.assertThat(dbHelper.getDbRowsCount()).isEqualTo(dbHelper.getActualRowsCount());
	}
}