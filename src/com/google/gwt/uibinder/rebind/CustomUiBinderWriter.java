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

package com.google.gwt.uibinder.rebind;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.uibinder.attributeparsers.AttributeParsers;
import com.google.gwt.uibinder.attributeparsers.BundleAttributeParsers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.rebind.messages.MessagesWriter;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.OwnerClass;
import com.jhickman.web.gwt.customuibinder.rebind.CustomHandlerEvaluator;
import com.jhickman.web.gwt.customuibinder.rebind.Reflector;
import com.jhickman.web.gwt.customuibinder.resourceparsers.ResourceParser;

/**
 * Custom implementation of the {@link UiBinderWriter}.
 * 
 * In this implementation, we extend UiBinderWriter, and by using
 * reflection, modifying the private fields in the superclass.
 * 
 * Hopefully, by not copying the entire contents of UiBinderWriter,
 * we're buffered from future changes to GWT.
 *
 */
public class CustomUiBinderWriter extends UiBinderWriter {
    
    private static final String GWT_UIBINDER_ELEMENTPARSER = "gwt.uibinder.elementparser";
    private static final String GWT_UIBINDER_CUSTOM_HANDLER_EVALUATOR = "gwt.uibinder.customHandlerEvaluator";
	private static final String GWT_UIBINDER_RESOURCEPARSER = "gwt.uibinder.resourceparser";
	
	private static final Pattern GWT_PROPERTY_SPLIT_PATTERN = Pattern.compile("^(.*):()$");
    
    
    private Map<String, String> globalElementParsers;
    private Map<String, String> customElementParsers = new HashMap<String, String>();
	private List<Class<? extends ResourceParser>> customResourceParsers = new ArrayList<Class<? extends ResourceParser>>();
    private final MortalLogger logger;
    
    private MultiHandlerEvaluator multiHandlerEvaluator;
    private final TypeOracle oracle;
    private final Reflector<UiBinderWriter> reflector;

    
    public CustomUiBinderWriter(JClassType baseClass, String implClassName,
            String templatePath, TypeOracle oracle,
            PropertyOracle propertyOracle, MortalLogger logger,
            FieldManager fieldManager, MessagesWriter messagesWriter,
            DesignTimeUtils designTime, UiBinderContext uiBinderCtx, boolean useSafeHtmlTemplates)
            throws UnableToCompleteException {
        super(baseClass, 
                implClassName, 
                templatePath, 
                oracle, 
                logger, 
                fieldManager, 
                messagesWriter, 
                designTime, 
                uiBinderCtx, 
                useSafeHtmlTemplates);
        this.oracle = oracle;
        this.logger = logger;
        this.reflector = new Reflector<UiBinderWriter>(UiBinderWriter.class, this, logger);

        if ( ! isCustomParserCapable()) {
            logger.warn("Was unable to fetch the elementParsers object to register custom ElementParsers. No custom parsers will be run.");
            return;
        }
        
        findCustomParsers(propertyOracle, logger);
        
        findCustomHandlerEvaluators(propertyOracle);
        
        findCustomResourceParser(propertyOracle);
    }

	protected void findCustomHandlerEvaluators(PropertyOracle propertyOracle)
			throws UnableToCompleteException {
		ConfigurationProperty customHandlerEvaluatorConfigurationProperty = getProperty(propertyOracle, GWT_UIBINDER_CUSTOM_HANDLER_EVALUATOR);
        if (customHandlerEvaluatorConfigurationProperty != null) {
            for(String propertyValue : customHandlerEvaluatorConfigurationProperty.getValues()) {
                multiHandlerEvaluator.addCustomHandlerEvaluator(propertyValue);
            }
        }
	}

	protected void findCustomParsers(PropertyOracle propertyOracle,
			MortalLogger logger) throws UnableToCompleteException {
		ConfigurationProperty customParsersConfigurationProperty = getProperty(propertyOracle, GWT_UIBINDER_ELEMENTPARSER);
        if (customParsersConfigurationProperty != null) {
            List<String> properties = customParsersConfigurationProperty.getValues();
            for (String propertyValue : properties) {
                
                String[] parts = propertyValue.split(":");
                if (parts.length != 2) {
                    logger.die("Registed custom element parser is improperly formatted. Format required: widgetClass:parserClass.  Value found: %s", propertyValue);
                }
        
                customElementParsers.put(parts[0], parts[1]);
            }
        }
	}
	
	protected void findCustomResourceParser(PropertyOracle propertyOracle) throws UnableToCompleteException {
		ConfigurationProperty customResourceParserConfigurationProperty = getProperty(propertyOracle, GWT_UIBINDER_RESOURCEPARSER);
		if (customResourceParserConfigurationProperty != null) {
			for(String propertyValue : customResourceParserConfigurationProperty.getValues()) {
				try {
					Class<? extends ResourceParser> parser = Class.forName(propertyValue, true, Thread.currentThread().getContextClassLoader()).asSubclass(ResourceParser.class);
					customResourceParsers.add(parser);
				} catch (Exception e) {
					logger.die("Registered custom resource parser cannot be found or not implementing ResourceParser: found %s", propertyValue);
				}
			}
		}
	}


    
    @Override
    public String parseElementToField(XMLElement elem) throws UnableToCompleteException {
        if (globalElementParsers != null && globalElementParsers.isEmpty()) {
            registerParsers();
        }

        return super.parseElementToField(elem);
    }
    
    @SuppressWarnings("unchecked")
    private boolean isCustomParserCapable() {
        try {
            Field elementParsersField = UiBinderWriter.class.getDeclaredField("elementParsers");
            elementParsersField.setAccessible(true);
            globalElementParsers = (Map<String, String>) elementParsersField.get(this);
            
            Field handlerEvaluatorField = UiBinderWriter.class.getDeclaredField("handlerEvaluator");
            handlerEvaluatorField.setAccessible(true);
            HandlerEvaluator handlerEvaluator = (HandlerEvaluator) handlerEvaluatorField.get(this);
            multiHandlerEvaluator = new MultiHandlerEvaluator(handlerEvaluator, logger, oracle);
            handlerEvaluatorField.set(this, multiHandlerEvaluator);
            
            return true;
        } catch (Exception e) {
            logger.warn("Problem using reflection: %s", e);
            return false;
        }
    }
    
    private void registerParsers() {
        try {
            // first we must invoke the superclass registerParsers
            Method method = UiBinderWriter.class.getDeclaredMethod("registerParsers");
            method.setAccessible(true);
            method.invoke(this);
            // now we can add our own
            globalElementParsers.putAll(customElementParsers);
        } catch (Exception e) {
            logger.warn("Unable to register custom elementParsers.");
        }
    }

    private ConfigurationProperty getProperty(PropertyOracle propertyOracle, String propertyName) {
        try {
            return propertyOracle.getConfigurationProperty(propertyName);
        } catch (BadPropertyValueException e) {
            return null;
        }
    }
    
    /*
     * overridden to provide custom resource/field parsing
     */
    @Override
    void parseDocument(Document doc, PrintWriter printWriter) throws UnableToCompleteException {
    	JClassType baseClass = reflector.getField("baseClass");
        JClassType uiBinderClass = getOracle().findType(UiBinder.class.getName());
        if (!baseClass.isAssignableTo(uiBinderClass)) {
          die(baseClass.getName() + " must implement UiBinder");
        }

        Element documentElement = doc.getDocumentElement();
        
        reflector.setField("gwtPrefix", documentElement.lookupPrefix(UiBinderGenerator.BINDER_URI));

        AttributeParsers attributeParsers = reflector.getField("attributeParsers");
		BundleAttributeParsers bundleParsers = reflector.getField("bundleParsers");
		DesignTimeUtils designTime = reflector.getField("designTime");
		
		XMLElement elem = new XMLElementProviderImpl(attributeParsers,
            bundleParsers, oracle, logger, designTime).get(documentElement);
		
		
        Tokenator tokenator = reflector.getField("tokenator");
        String rendered = tokenator.detokenate(parseDocumentElement(elem));
		reflector.setField("rendered", rendered);
        printWriter.print(rendered);
    }
    
    /**
     * Parse the document element and return the source of the Java class that
     * will implement its UiBinder.
     */
    private String parseDocumentElement(XMLElement elem) throws UnableToCompleteException {
    	FieldManager fieldManager = reflector.getField("fieldManager");
		ImplicitClientBundle bundleClass = reflector.getField("bundleClass");
		
		
		fieldManager .registerFieldOfGeneratedType(
    			oracle.findType(ClientBundle.class.getName()),
    			bundleClass .getPackageName(), bundleClass.getClassName(),
    			bundleClass.getFieldName());
    	// Allow GWT.create() to init the field, the default behavior
    	
    	MessagesWriter messages = reflector.getField("messages");
		String rootField = new CustomUiBinderParser(this, messages, fieldManager, oracle, bundleClass, customResourceParsers).parse(elem);
    	
    	fieldManager.validate();
    	
    	StringWriter stringWriter = new StringWriter();
    	IndentedWriter niceWriter = new IndentedWriter(
    			new PrintWriter(stringWriter));
    	
    	reflector.callMethod("writeBinder", new Class[]{IndentedWriter.class, String.class}, niceWriter, rootField);
    	
    	reflector.callMethod("ensureAttachmentCleanedUp", new Class[0]);
    	return stringWriter.toString();
    }
    
    
    
    private static final class MultiHandlerEvaluator extends HandlerEvaluator {
        
        private List<CustomHandlerEvaluator> customHandlerEvaluators = new ArrayList<CustomHandlerEvaluator>();
        private final OwnerClass ownerClass;
        private final MortalLogger logger;
        private final TypeOracle oracle;

        public MultiHandlerEvaluator(final HandlerEvaluator existingHandlerEvaluator, MortalLogger logger, TypeOracle oracle) throws Exception {
            super(null, null, oracle);
            
            this.logger = logger;
            this.oracle = oracle;
            
            customHandlerEvaluators.add(new CustomHandlerEvaluator() {
                public void run(IndentedWriter writer, FieldManager fieldManager, String uiOwner, OwnerClass ownerClass, MortalLogger logger, TypeOracle oracle) throws UnableToCompleteException {
                    existingHandlerEvaluator.run(writer, fieldManager, uiOwner);
                }
            });
            
            ownerClass = extractValue(existingHandlerEvaluator, "ownerClass");
        }

        @Override
        public void run(IndentedWriter writer, FieldManager fieldManager, String uiOwner) throws UnableToCompleteException {
            for (CustomHandlerEvaluator evaluator : customHandlerEvaluators) {
                evaluator.run(writer, fieldManager, uiOwner, ownerClass, logger, oracle);
            }
        }
        
        public void addCustomHandlerEvaluator(String propertyValue) throws UnableToCompleteException {
            try {
                Class<? extends CustomHandlerEvaluator> clazz = Class.forName(propertyValue, true, Thread.currentThread().getContextClassLoader()).asSubclass(CustomHandlerEvaluator.class);
                addCustomHandlerEvaluator(clazz);
            } catch (Exception e) {
                logger.die("Invalid CustomHanderEvaluator cannot find or wrong type: %s", propertyValue);
            }
        }
        
        public void addCustomHandlerEvaluator(Class<? extends CustomHandlerEvaluator> customHandlerEvaluatorClass) throws UnableToCompleteException {

            try {
                CustomHandlerEvaluator instance = customHandlerEvaluatorClass.newInstance();
                customHandlerEvaluators.add(instance);
            } catch (Exception e) {
                logger.die("CustomHandlerEvaluator classes should have an accessible no-arg constructor.  Failed on %s", customHandlerEvaluatorClass.getName());
            }
        }

        private OwnerClass extractValue(HandlerEvaluator existingHandlerEvaluator, String string) throws Exception {
            Field ownerClassField = HandlerEvaluator.class.getDeclaredField("ownerClass");
            ownerClassField.setAccessible(true);
            return (OwnerClass) ownerClassField.get(existingHandlerEvaluator);
        }

    }
}
