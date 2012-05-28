package ca.phon.ipa.phone.phonex;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation required for phonex plug-ins.  
 * Identifies the name used for matcher
 * part of the phonex expression.
 * 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PhonexPlugin {
	
	/**
	 * <p>The name used in the phonex expression to identify
	 * the matcher.</p>
	 * 
	 * <p>
	 * For example, if the a class FooMatcher implements {@link PluginMatcher} and
	 * has the annotation @PhonexPlugin("foo") then FooMatcher will
	 * be used to parse phonex matchers identified with the "foo" string.
	 * <pre>
	 * {}:foo(&lt;expression&gt;)
	 * </pre>
	 * 
	 * The value of expression will be passed into the {@link PluginMatcher#checkInput(String)}
	 * and {@link PluginMatcher#createMatcher(String)} methods.
	 * </p>
	 */
	public String value();
	
}
