import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import java.sql.SQLException;

public class BaseTest {

	protected static final String LOGIN = "supertest";
	protected static final String PASSWORD = "superpassword";

	protected DataBaseHelper dbHelper = new DataBaseHelper();
	protected TestDataHelper tdHelper = new TestDataHelper();

	protected static String token = "";
	protected static String payload;
	protected long tokenExpirationTime;

	protected RequestSpecification requestSpecification;

	@Parameters ("dbpath")
	@BeforeSuite
	public void setUp(String sqlitePath) {
		dbHelper.setDataBasePath(sqlitePath);
		RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
		requestSpecBuilder.setBaseUri ("http://localhost:5000");
		requestSpecification = requestSpecBuilder.build();
	}

	@AfterSuite
	public void tearDown() throws SQLException {
		dbHelper.truncateTable();
	}
}
