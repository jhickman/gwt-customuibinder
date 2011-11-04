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
package com.google.gwt.uibinder.rebind;

import java.util.List;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.jhickman.web.gwt.customuibinder.rebind.Reflector;
import com.jhickman.web.gwt.customuibinder.resourceparsers.ResourceParser;

/**
 * @author hickman
 *
 */
public class CustomUiBinderParser extends UiBinderParser {
	
	private static final String TAG = "UiBinder";	
	private final Reflector<UiBinderParser> reflector;
	private final UiBinderWriter writer;
	private final FieldManager fieldManager;
	private final List<Class<? extends ResourceParser>> customResourceParsers;
	private final String binderUri;

	/**
	 * @param writer
	 * @param messagesWriter
	 * @param fieldManager
	 * @param oracle
	 * @param bundleClass
	 * @param customResourceParsers2 
	 */
	public CustomUiBinderParser(UiBinderWriter writer,
			MessagesWriter messagesWriter, FieldManager fieldManager,
			TypeOracle oracle, ImplicitClientBundle bundleClass, String binderUri, List<Class<? extends ResourceParser>> customResourceParsers) {
		
		super(writer, messagesWriter, fieldManager, oracle, bundleClass, binderUri);
		this.writer = writer;
		this.fieldManager = fieldManager;
		this.binderUri = binderUri;
		this.customResourceParsers = customResourceParsers;
		
		this.reflector = new Reflector<UiBinderParser>(UiBinderParser.class, this, writer.getLogger());
	}
	
	
	@Override
	public String parse(XMLElement elem) throws UnableToCompleteException {
		if (!writer.isBinderElement(elem)) {
			writer.die(elem, "Bad prefix on <%s:%s>? The root element must be in "
					+ "xml namespace \"%s\" (usually with prefix \"ui:\"), "
					+ "but this has prefix \"%s\"", elem.getPrefix(),
					elem.getLocalName(), binderUri, elem.getPrefix());
		}
		
		if (!TAG.equals(elem.getLocalName())) {
			writer.die(elem, "Root element must be %s:%", elem.getPrefix(), TAG);
		}
		
		reflector.callMethod("findResources", new Class[]{XMLElement.class}, elem);
		
		findCustomResources(elem);
		
		MessagesWriter messagesWriter = reflector.getField("messagesWriter");
		messagesWriter .findMessagesConfig(elem);
		XMLElement uiRoot = elem.consumeSingleChildElement();
		return writer.parseElementToField(uiRoot);
	}


	/**
	 * @param elem
	 */
	private void findCustomResources(XMLElement binderElement) throws UnableToCompleteException {
		binderElement.consumeChildElements(new XMLElement.Interpreter<Boolean>() {
			public Boolean interpretElement(XMLElement elem) throws UnableToCompleteException {
				
				for (Class<? extends ResourceParser> parserClass : customResourceParsers) {
					ResourceParser parser;
					try {
						parser = parserClass.newInstance();
					} catch (Exception e) {
						writer.die("Unable to create instance of ResourceParser: %s", parserClass);
						return false; // won't be called, but need to satisfy compilation
					}
					
					if (parser.supports(elem)) {
						parser.parse(elem, fieldManager, writer);
						return true;
					}
				}
				return false;
			}
		});
	}

}
