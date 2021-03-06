/**
 * 
 */
package org.padirbtuves.lock;

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
		this.valid = validTill != null && new Date().before(validTill);
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
