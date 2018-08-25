package camelspider;

import java.util.*;

import soot.*;

public class MethodSummary {
	private static Map <SootMethod, AbstractExecution> ms;
	private static MethodSummary instance = null;
	
	private MethodSummary () {
		ms = new HashMap <SootMethod, AbstractExecution> ();
	}
	
	public static MethodSummary v () {
		if (instance == null)
			instance = new MethodSummary ();
		return instance;
	}
	
	/**
	 * 
	 * @param meth
	 * @param eidPrefix
	 * @param tidPrefix
	 * @param mult
	 * @param filter
	 *  if the summary of "meth" is to be used in a call site, set this parameter to "true,"
	 *  otherwise pass a "false." 
	 * @return
	 */
	public AbstractExecution retrieveSummary (SootMethod meth,
			String eidPrefix,
			String tidPrefix,
			Multiplicities mult,
			boolean filter) {
		if (meth == null)
			return null;
		AbstractExecution ae = ms.get(meth);
		
		if (ae == null)
			return null;		
		return ae.cloneAndInstantiate(eidPrefix, tidPrefix, mult, filter);
	}
	
	public AbstractExecution introduceMethodSummary (SootMethod meth, AbstractExecution summary) {
		return ms.put(meth, summary);
	}
	
	/**
	 * Warning: call this after all methods have been analyzed. Otherwise, all computation results will be lost!
	 * @param toBeKept
	 */
	public void relaxMemory () {
		ms.clear();
		System.gc ();
	}
}
