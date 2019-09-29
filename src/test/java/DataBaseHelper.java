import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Working with database.
 */
class DataBaseHelper {

	private String databasePath;
	private Connection dbConnection;
	private int dbRowsCount;

	/**
	 * Setting SQLite DataBase path.
	 * @param dataBasePath path
	 */
	void setDataBasePath(String dataBasePath) {
		this.databasePath = dataBasePath;
	}

	/**
	 * 	Extracting the last row from the database.
	 * @return result set as map
	 * @throws SQLException SQLException
	 */
	Map<String, String> getStoredValues(String id) throws SQLException {
		checkConnection();
		Map<String, String> rsMap = new HashMap<>();
		PreparedStatement ps = dbConnection.prepareStatement("select * from uploads where id = ?");
		ps.setString(1, id);
		ResultSet resultSet = ps.executeQuery();
		if (resultSet.next()) {
			rsMap.put("login", resultSet.getString(2));
			rsMap.put("payload", resultSet.getString(3));
		}
		dbConnection.close();
		return rsMap;
	}

	/**
	 * Establishing the connection to the DataBase.
	 * @throws SQLException SQLException
	 */
	private void getConnection() throws SQLException {
		dbConnection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
	}

	void setDbRowsCount() throws SQLException {
		checkConnection();
		dbRowsCount = dbConnection.createStatement().executeQuery("select count(*) from uploads;").getInt(1);
		dbConnection.close();
	}

	int getDbRowsCount() {
		return dbRowsCount;
	}

	/**
	 * Getting actual count of row in the database.
	 * @return count
	 * @throws SQLException SQLException
	 */
	int getActualRowsCount() throws SQLException {
		checkConnection();
		int res = dbConnection.createStatement().executeQuery("select count(*) from uploads;").getInt(1);
		dbConnection.close();
		return res;
	}

	/**
	 * Erasing working table.
	 * @throws SQLException SQLException
	 */
	void truncateTable() throws SQLException {
		checkConnection();
		dbConnection.createStatement().executeUpdate("delete from uploads;");
		dbConnection.close();
	}

	/**
	 * Checking connection with the database.
	 * @throws SQLException SQLException
	 */
	private void checkConnection() throws SQLException {
		if (dbConnection == null || dbConnection.isClosed()) {
			getConnection();
		}
	}
}
