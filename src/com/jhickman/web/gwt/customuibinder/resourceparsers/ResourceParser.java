/**
 * 
 */
package com.jhickman.web.gwt.customuibinder.resourceparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * @author hickman
 *
 */
public interface ResourceParser {
	public boolean supports(XMLElement elem);
	public void parse(XMLElement elem, FieldManager fieldManager, UiBinderWriter writer) throws UnableToCompleteException;
}
