package de.unisaarland.instrumentation.guard;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;
import de.unisaarland.instrumentation.Instrumenter;
import de.unisaarland.instrumentation.disl.context.FieldStaticContext;

public class StoreGuard {
	@GuardMethod
	public static boolean isApplicable(MethodStaticContext msc, FieldStaticContext fsc) {
		return !Instrumenter.isIgnoredClass(msc.thisClassName());
	}
}
