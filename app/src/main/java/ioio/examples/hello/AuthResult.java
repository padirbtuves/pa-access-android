/**
 * 
 */
package ioio.examples.hello;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author ibm
 *
 */
public class AuthResult {

	public final boolean valid;
	
	public final Date validTill;
	
	public final String tagId;
	
	public AuthResult(String tagId, Date validTill) {
		this.tagId = tagId;
		this.validTill = validTill;
		if (validTill != null) {
			this.valid = new Date().before(validTill);
		} else {
			this.valid = false;
		}
	}
	
	@Override
	public String toString() {
		if (this.valid) {
			DateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
			return "valid till : " + displayFormat.format(this.validTill);
		} else {
			return "unknown tag";
		}
	}
}
