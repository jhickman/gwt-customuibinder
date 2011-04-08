/**
 * Copyright 2010 Justin Hickman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package com.jhickman.web.gwt.customuibinder.rebind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;

/**
 * @author hickman
 *
 */
public class Reflector<T> {
	
	private final Class<T> theClass;
	private final T instance;
	private final MortalLogger logger;
	
	public Reflector(Class<T> theClass, T instance, MortalLogger logger) {
		this.theClass = theClass;
		this.instance = instance;
		this.logger = logger;
	}
	
	
	
	public <R> R getField(String fieldName) throws UnableToCompleteException {
		try {
			Field declaredField = theClass.getDeclaredField(fieldName);
			declaredField.setAccessible(true);
			return (R) declaredField.get(instance);
		} catch (Exception e) {
			handleException(e);
		}
		return null;
	}
	
	public void setField(String fieldName, Object value) throws UnableToCompleteException {
		try {
			Field declaredField = theClass.getDeclaredField(fieldName);
			declaredField.setAccessible(true);
			declaredField.set(instance, value);
		} catch (Exception e) {
			handleException(e);
		}
	}

	/**
	 * @param string
	 * @param classes
	 * @param niceWriter
	 * @param rootField
	 */
	public <R> R callMethod(String string, Class<?>[] classes, Object... params) throws UnableToCompleteException {
		try {
			Method declaredMethod = theClass.getDeclaredMethod(string, classes);
			declaredMethod.setAccessible(true);
			return (R) declaredMethod.invoke(instance, params);
		} catch (Exception e) {
			handleException(e);
		}
		return null;
	}


	/**
	 * @param e
	 */
	private void handleException(Exception e) throws UnableToCompleteException {
		logger.warn("Problem using reflection: %s", e);
		throw new UnableToCompleteException();
	}

}
