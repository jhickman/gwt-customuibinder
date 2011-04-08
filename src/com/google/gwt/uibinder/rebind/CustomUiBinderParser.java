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
			TypeOracle oracle, ImplicitClientBundle bundleClass, List<Class<? extends ResourceParser>> customResourceParsers) {
		
		
		super(writer, messagesWriter, fieldManager, oracle, bundleClass);
		this.writer = writer;
		this.fieldManager = fieldManager;
		this.customResourceParsers = customResourceParsers;
		
		this.reflector = new Reflector<UiBinderParser>(UiBinderParser.class, this, writer.getLogger());
	}
	
	
	@Override
	public String parse(XMLElement elem) throws UnableToCompleteException {
		if (!writer.isBinderElement(elem)) {
			writer.die(elem, "Bad prefix on <%s:%s>? The root element must be in "
					+ "xml namespace \"%s\" (usually with prefix \"ui:\"), "
					+ "but this has prefix \"%s\"", elem.getPrefix(),
					elem.getLocalName(), UiBinderGenerator.BINDER_URI, elem.getPrefix());
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
