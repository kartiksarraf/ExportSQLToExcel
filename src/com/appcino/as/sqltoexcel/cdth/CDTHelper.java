package com.appcino.as.sqltoexcel.cdth;

import java.sql.Date;

import com.appiancorp.suiteapi.type.exceptions.InvalidTypeException;

public class CDTHelper extends AbstractCDTHelper {
	public CDTHelper(Long typeOf, String[] names, Long[] types) {
		super(typeOf, names, types);
	}

	// Main Types (String, Long, Double, Date, CDTHelper)  setters
	public void setValue(String name, String value) {
		super.setValue(name, value);
	}

	public void setValue(String name, Long value) {
		super.setValue(name, value);
	}

	public void setValue(String name, Double value) {
		super.setValue(name, value);
	}

	public void setValue(String name, Date value) {
		super.setValue(name, value);
	}

	public void setValue(String name, CDTHelper value) {
		super.setValue(name, value);
	}

	// adders
	public void addValue(String name, String value) throws InvalidTypeException {
		super.addValue(name, value);
	}

	public void addValue(String name, Long value) throws InvalidTypeException {
		super.addValue(name, value);
	}

	public void addValue(String name, Double value) throws InvalidTypeException {
		super.addValue(name, value);
	}

	public void addValue(String name, Date value) throws InvalidTypeException {
		super.addValue(name, value);
	}

	public void addValue(String name, CDTHelper value) throws InvalidTypeException {
		super.addValue(name, value);
	}

	// Primitive Types (int) setters
	public void setValue(String name, int value) {
		setValue(name, new Long(value));
	}

	// adders
	public void addValue(String name, int value) throws InvalidTypeException {
		addValue(name, new Long(value));
	}
}
