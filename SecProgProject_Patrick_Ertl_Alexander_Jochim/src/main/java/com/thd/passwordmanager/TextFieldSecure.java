package com.thd.passwordmanager;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.textfield.TextField;

public class TextFieldSecure extends TextField {

	private String forbiddenInputs = "'+:,;<>*%(){}[]|`¬¦!£$%^&*#~+=,/\\" + '"';

	/**
	 * @return String
	 */
	@Override
	public String getValue() {
		char[] value = super.getValue().toCharArray();
		Set<String> validationSet = getValidationSet();
		if (value.length < 9 || value.length > 30) {
			DBController.setErrorMessage("length");
			return null;
		}
		for (int i = 0; i < value.length; i++) {
			if (validationSet.contains(String.valueOf(value[i])) == true) {
				DBController.setErrorMessage("invalidInput");
				return null;
			}
		}
		return String.valueOf(value);
	}

	/**
	 * @return String
	 */
	public String getUniqueUsername() {
		char[] value = super.getValue().toCharArray();
		if (DBController.checkDuplicate(String.valueOf(value))) {
			DBController.setErrorMessage("duplicateUsername");
			return null;
		}
		Set<String> validationSet = getValidationSet();
		if (value.length < 9 || value.length > 30) {
			DBController.setErrorMessage("length");
			return null;
		}
		for (int i = 0; i < value.length; i++) {
			if (validationSet.contains(String.valueOf(value[i])) == true) {
				DBController.setErrorMessage("invalidInput");
				return null;
			}
		}
		return String.valueOf(value);
	}

	/**
	 * @return Set of Strings
	 */
	public Set<String> getValidationSet() {
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < forbiddenInputs.length(); i++) {
			set.add(String.valueOf(forbiddenInputs.charAt(i)));
		}
		return set;
	}
}
