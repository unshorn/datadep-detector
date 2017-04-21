package de.unisaarland.instrumentation;

/**
 * This interface is dynamically added to objects to enable dynamic tainting
 * 
 * @author gambi
 *
 */
public interface DependencyInstrumented {
	/**
	 * The tag of the object
	 * 
	 * @return
	 */
	public DependencyInfo getDEPENDENCY_INFO();
}
