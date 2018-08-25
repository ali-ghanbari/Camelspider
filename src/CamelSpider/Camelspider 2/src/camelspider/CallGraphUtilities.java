package camelspider;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.purity.*;
import soot.jimple.toolkits.callgraph.*;
import soot.toolkits.graph.*;

public class CallGraphUtilities {
	public static final Set <SootMethod> Methods;
	private static CallGraphUtilities instance = null;
	public static DirectedCallGraph directedCallGraph = null;
	
	static {
		Methods = new HashSet <SootMethod> ();
		
		for (SootClass cls : Scene.v().getApplicationClasses())
			for (SootMethod m : cls.getMethods())
				if (m.isDeclared() && !m.isAbstract() && (!m.isEntryMethod() || m.isMain()))
					Methods.add(m);
		SootMethodFilter myMethFilter = new SootMethodFilter() {
			public boolean want(SootMethod method) {
				return isWantedMethod(method);
			}
		};
		List<SootMethod> heads = new LinkedList<SootMethod>();
		
		heads.add(Scene.v().getMainMethod());
		directedCallGraph = new DirectedCallGraph(Scene.v().getCallGraph(), myMethFilter, heads.iterator(), false);
	}
	
	public static CallGraphUtilities v () {
		if (instance == null)
			instance = new CallGraphUtilities();
		return instance;
	}
	
	private static boolean isWantedMethod (SootMethod method) {
		if (method.getDeclaringClass().isApplicationClass() && !method.isAbstract())
			if (!method.isEntryMethod() || method.isMain())
				return true;
		return false;
	}
	
	public Collection <SootMethod> mayCall (SootMethod method) {
		Iterator <Edge> oe = Scene.v().getCallGraph().edgesOutOf(method);
		List <SootMethod> result = new LinkedList <SootMethod> ();
		
		while (oe.hasNext()) {
			SootMethod m = oe.next().getTgt().method();
			
			if (isWantedMethod(m))
					result.add(m);
		}
		return result;
	}
	
	private Collection <SootMethod> mayCallIndirectlyWorkHorse (SootMethod method, List <SootMethod> result) {
		Iterator <Edge> oe = Scene.v().getCallGraph().edgesOutOf(method);
		
		while (oe.hasNext()) {
			SootMethod meth = oe.next().getTgt().method();
			
			if (!result.contains(meth))
				if (isWantedMethod(meth)) {
						result.add(meth);
						mayCallIndirectlyWorkHorse (meth, result);
					}
		}
		return result;
	}
	
	public Collection <SootMethod> mayCallIndirectly (SootMethod method) {
		return mayCallIndirectlyWorkHorse(method, new LinkedList <SootMethod> ());
	}
	
	public boolean isRecursive (SootMethod method) {
		return mayCallIndirectly (method).contains(method);
	}
	
	public Collection <SootMethod> callees (InvokeStmt callSite) {
		Iterator <Edge> oe = Scene.v().getCallGraph().edgesOutOf(callSite);
		List <SootMethod> result = new LinkedList <SootMethod> ();
		
		while (oe.hasNext()) {
			SootMethod m = oe.next().getTgt().method();
			
			if (isWantedMethod(m))
					result.add(m);
		}
		return result;
	}
	
	public boolean SCC (SootMethod m1, SootMethod m2) {
		return mayCallIndirectly(m1).contains(m2) && mayCallIndirectly(m2).contains(m1);
	}
	
	public boolean isInLoop (final Unit unit, DirectedGraph <Unit> surroundingUG, SootMethod ownerMethod) {
		if (isRecursive (ownerMethod))
			return true;
		UnitVisitor unitVisitor = new UnitVisitor() {
			public boolean visit(Unit u) {
				if (u.equals(unit))
					return true;
				return false;
			}
		};
		
		return doDepthFirstSearch(surroundingUG, unit, unitVisitor);
	}
	
	private List <SootMethod> recGroup (SootMethod meth) {
		List <SootMethod> res = new LinkedList <SootMethod> ();
		
		for (SootMethod m : Methods)
			if (SCC (meth, m) || SCC (m, meth))
				res.add(m);
		return res;
	}
	
	public Set <List <SootMethod>> RecGroups (Set <SootMethod> M) {
		Set <List <SootMethod>> res = new HashSet <List <SootMethod>> ();
		
		for (SootMethod m : M)
			res.add(recGroup (m));
		return res;
	}
	
	private UnitVisitor visitor;
	private Set <Unit> visited;
	private DirectedGraph <Unit> graph;
	
	private boolean doDepthFirstSearch (List <Unit> queue) {
		for (Unit unit : queue)
			if (!visited.contains(unit)) {				
				visited.add(unit);
				if (visitor.visit(unit))
					return true;
				if (doDepthFirstSearch(graph.getSuccsOf (unit)))
					return true;
			}
		return false;
	}
	
	private boolean doDepthFirstSearch (DirectedGraph <Unit> graph, Unit origin, UnitVisitor visitor) {
		this.graph = graph;
		this.visitor = visitor;
		if (visited == null)
			visited = new HashSet <Unit> ();
		else
			visited.clear();
		return doDepthFirstSearch(graph.getSuccsOf (origin));
	}
}

interface UnitVisitor {
	/**
	 * return "true" is you wish to stop searching
	 * @param unit
	 * @return
	 */
	public boolean visit (Unit unit);
}
