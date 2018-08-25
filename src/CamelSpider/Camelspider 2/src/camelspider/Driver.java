package camelspider;

import java.util.*;

import soot.*;

public class Driver {
	private static Driver instance = null;
	
	public static Driver v () {
		if (instance == null) instance = new Driver ();
		return instance;
	}
	
	private void orderedAnalyze () {
		Set <SootMethod> M = new HashSet <SootMethod> ();
		Set <SootMethod> N = new HashSet <SootMethod> (CallGraphUtilities.Methods);
		
		do {
			for (SootMethod m : N) {
				if (M.containsAll(CallGraphUtilities.v().mayCall(m))) {
					System.out.print ("Analyzing the method " + m);
					ModularAnalyzer analyzer = new ModularAnalyzer(m, new LinkedList <SootMethod> ());
					
					MethodSummary.v().introduceMethodSummary(m, analyzer.retrieveSummary());
					M.add(m);
					System.out.print(" [OK]\n");
					System.out.flush();
				}
			}
			Set <List <SootMethod>> R = CallGraphUtilities.v().RecGroups(N);
			
			for (List <SootMethod> r : R)
				for (SootMethod m : r) {
					Set<SootMethod> mci = new HashSet<SootMethod>(M);
					
					mci.addAll(r);
					if (mci.containsAll(CallGraphUtilities.v().mayCallIndirectly(m))) {
						System.out.print ("Analyzing the method " + m);
						ModularAnalyzer analyzer = new ModularAnalyzer(m, new LinkedList <SootMethod> ());
						
						MethodSummary.v().introduceMethodSummary(m,  analyzer.retrieveSummary());
						M.add(m);
						System.out.print(" [OK]\n");
						System.out.flush();
					}
				}
			N.removeAll(M);
		} while (!N.isEmpty());
		
	}
	
	public void start () {
		orderedAnalyze();
	}
}
