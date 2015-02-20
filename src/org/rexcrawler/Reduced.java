package org.rexcrawler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * All collection fields marked with this annotation
 * will be merged at the joining phase of the search.
 * 
 * If a field is not marked with "@Reduced", the master
 * parser will only collect the data from its parsing.
 * 
 * @author shake0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface Reduced {

}
