# DEPRECATED #

Please note that with the newer releases of GWT, this project is now deprecated as many of the techniques used to add custom element parsers to UiBinder are now not possible.


---


# Introduction #

As of GWT 2.1.0, UiBinder does not support the concept of registering custom ElementParser classes.  This project aims at providing a mechanism to register custom ElementParsers as well as custom Handler Evaluators.

Unlike some solutions for custom ElementParser registration, this one does not require ClassLoader tricks.  This module overrides the UiBinderGenerator.  The only requirement is for this module to be inherited after all other modules.  This will allow the CustomUiBinderGenerator to be used rather than the GWT UiBinderGenerator.

# News #
**2011-11-04**
  * Release of gwt-customuibinder 1.2.0
    * support for GWT 2.4.0 ([Issue 3](https://code.google.com/p/gwt-customuibinder/issues/detail?id=3))

  * Release of gwt-customuibinder 1.1.0
    * Support for GWT 2.3.0 ([Issue 1](https://code.google.com/p/gwt-customuibinder/issues/detail?id=1))
  * Released to Sonatype and am expecting activate of Central Sync

# Registering Custom ElementParsers #

Registering custom ElementParsers is done by extending a GWT configuration property.  This means that there are no code changes needed to support new ElementParser classes.

Here is an example:
```
<module>
  <extend-configuration-property name="gwt.uibinder.elementparser"
            value="com.foo.client.ui.SomeWidget:com.foo.uibinder.elementparsers.SomeWidgetParser" />
</module>
```

As you can see from this code snippet, we "extend-configuration-property" called "gwt.uibinder.elementparser".  The value of the property must be a colon separated widget:parser combination.

**Note:** If you use "set-configuration-property", it will override the previous definitions.


# Register Custom HandlerEvaluators #
gwt-customuibinder supports custom HandlerEvaluator instances as well.  If the widget library you're attempting to make UiBinder friendly doesn't support the same GWT Event model, then this will be something you would want to implement.

## module XML change ##
You can add a new HandlerEvaluator by using the "gwt.uibinder.customHandlerEvaluator" configuration property.

For example:
```
<module>
  <extend-configuration-property name="gwt.uibinder.customHandlerEvaluator"
            value="com.foo.rebind.SomeCustomHandlerEvaluator" />
</module>
```

## Implement CustomHandlerEvaluator interface ##
The value in the above XML change should point to a class that implements the "com.jhickman.web.gwt.customuibinder.rebind.CustomHandlerEvaluator" interface.

# Example Project #
Example usage can be found in the project [gxt-uibinder](https://code.google.com/p/gxt-uibinder/)


# News #

**2011-03-25**
  * Release 0.2
    * Updated dependency of GWT to 2.2.0
    * Changing Class.forName to use the current Thread ClassLoader
