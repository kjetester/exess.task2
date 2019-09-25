import org.apache.commons.lang3.RandomStringUtils;

import javax.xml.bind.*;
import java.security.*;

public class TestDataHelper {

	/**
	 * Generating random payload string.
	 */
	String genPayload(int length) {
		return RandomStringUtils.random(length, true, true);
	}

	/**
	 * Encrypting the payload.
	 * @return encrypted payload
	 * @throws NoSuchAlgorithmException NoSuchAlgorithmException
	 */
	String getEncryptedPayload(String payload) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(payload.getBytes());
		return DatatypeConverter.printHexBinary(md5.digest());
	}
}
