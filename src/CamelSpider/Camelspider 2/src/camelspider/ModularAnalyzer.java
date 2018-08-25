package camelspider;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.spark.pag.*;
import soot.jimple.spark.sets.*;
import soot.jimple.toolkits.callgraph.*;
import soot.toolkits.graph.*;

public class ModularAnalyzer {
	private DirectedGraph <Unit> graph;
	private Map <Unit, ComputedSummary> processedUnits;
	private AbstractExecution thisSummary;
	private SootMethod thisMethod;
	private List <SootMethod> callStack;
	
	public ModularAnalyzer (SootMethod method, List <SootMethod> callStack) {
		this.callStack = callStack;
		this.callStack.add(method);
		if (method.retrieveActiveBody() == null)
			System.exit(0);
		try {
		graph = new ExceptionalUnitGraph(method.retrieveActiveBody());
		} catch (Throwable o)
			System.exit(0);
		thisMethod = method;
		processedUnits = new HashMap <Unit, ComputedSummary> ();
		thisSummary = new AbstractExecution ();
		doAnalysis ();
	}
	
	private void processUnit (Unit stmt) {
		ComputedSummary result = null;
		
		if (stmt instanceof InvokeStmt) {
			AbstractExecution partialResult = new AbstractExecution ();
			boolean isThreadSummary = processInvoke((InvokeStmt) stmt, partialResult);
			
			try {
				result = new ComputedSummary (partialResult, isThreadSummary);
			} catch (InappropriateSummaryException e) {
				e.printStackTrace();
			}
		} else if (stmt instanceof AssignStmt) {
			try {
				result = new ComputedSummary (processAssign((AssignStmt) stmt), false);
			} catch (InappropriateSummaryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (stmt instanceof EnterMonitorStmt || stmt instanceof ExitMonitorStmt)
			result = new ComputedSummary (processMonitor((MonitorStmt) stmt));
		else
			result = new ComputedSummary(new AbstractNeutralEvent());
		processedUnits.put(stmt, result);
	}
	
	private AbstractEvent processMonitor(MonitorStmt stmt) {		
		if (stmt instanceof EnterMonitorStmt) {
			EnterMonitorStmt N = (EnterMonitorStmt) stmt;
			
			return createMonitorEvent((Local) N.getOp(), KIND_ENTER_MONITOR);
		} else { // it's an ExitMonitor statement.
			ExitMonitorStmt X = (ExitMonitorStmt) stmt;
			
			return createMonitorEvent((Local) X.getOp(), KIND_EXIT_MONITOR);
		}
	}

	private AbstractExecution processAssign(AssignStmt stmt) {	
		AbstractEvent theEventOfLHS = null;
		Set <AbstractEvent> succs = new HashSet <AbstractEvent> ();
		
		AbstractExecution res = new AbstractExecution ();
		Value left = stmt.getLeftOp();
		PointsToSet pts;

		if (left instanceof InstanceFieldRef) {
			//the assignment has a write on a non-final instance field
			Local theVar = (Local) ((InstanceFieldRef) left).getBase();
			SootField theFd = ((InstanceFieldRef) left).getField();

			//now use theField and theVar to generate a WRITE event
			pts = Scene.v().getPointsToAnalysis().reachingObjects(theVar);
			theEventOfLHS = new AbstractWriteEvent (new ThreadID("", Multiplicities.UNKNOWN), 
					new FieldObjectLoci(pts, theFd.getName()));
		} else if (left instanceof StaticFieldRef && !((StaticFieldRef) left).getField().isFinal())
			theEventOfLHS = new AbstractWriteEvent (new ThreadID("", Multiplicities.UNKNOWN),
					new FieldObjectLoci((StaticFieldRef) left));
		else if (left instanceof ArrayRef) {
			Local arrBase = (Local) ((ArrayRef) left).getBase();
			
			pts = Scene.v().getPointsToAnalysis().reachingObjects(arrBase);
			theEventOfLHS = new AbstractWriteEvent (new ThreadID("", Multiplicities.UNKNOWN),
					new FieldObjectLoci(pts, "array"));
		}
		//examine right-hand-side of the assignment for possible reads
		Iterator<ValueBox> useIt = stmt.getUseBoxes().iterator();

		while (useIt.hasNext()) {
			Value theRef = ((ValueBox) useIt.next()).getValue();

			if (theRef instanceof InstanceFieldRef && !((InstanceFieldRef) theRef).getField().isFinal()) {
				Local theVar = (Local) ((InstanceFieldRef) theRef).getBase();
				SootField theFd = ((InstanceFieldRef) theRef).getField();

				//now use theField and theVar to generate a READ event
				pts = Scene.v().getPointsToAnalysis().reachingObjects(theVar);
				succs.add(new AbstractReadEvent (new ThreadID("", Multiplicities.UNKNOWN),
						new FieldObjectLoci(pts, theFd.getName())));
			} else if (theRef instanceof StaticFieldRef && !((StaticFieldRef) theRef).getField().isFinal())
				succs.add(new AbstractReadEvent(new ThreadID("", Multiplicities.UNKNOWN),
						new FieldObjectLoci((StaticFieldRef) theRef)));
			else if (theRef instanceof ArrayRef) {
				Local arrBase = (Local) ((ArrayRef) theRef).getBase();
				
				pts = (PointsToSet) Scene.v().getPointsToAnalysis().reachingObjects(arrBase);
				succs.add(new AbstractReadEvent(new ThreadID("", Multiplicities.UNKNOWN),
						new FieldObjectLoci(pts, "array")));
			}
		}
		//examine if there exists an invoke expression at the right had side
		if (stmt.containsInvokeExpr()) {
			processInvoke (stmt, res);
			if (theEventOfLHS != null) {
				Set <AbstractEvent> heads = res.getHeads();
				
				res.getHeads().clear();
				res.addNode(theEventOfLHS, null, heads, AbstractExecution.HEAD);
			}
		} else if (theEventOfLHS != null) {
			if (succs.size() > 0) {
				res.addNode(theEventOfLHS, null, succs, AbstractExecution.HEAD);
				if (succs.size() > 1)
					res.buildAnchors();
				else
					res.getTails().add(succs.iterator().next());
			} else {
				res.addNode(theEventOfLHS, null, null, AbstractExecution.HEAD);
				res.getTails().add(theEventOfLHS);
			}
		} else if (succs.size() > 0) { //we have only rhs 
			AbstractEvent hdN = new AbstractNeutralEvent();
			AbstractEvent tlN = new AbstractNeutralEvent();
			Set<AbstractEvent> hd = new HashSet <AbstractEvent>();
			Set<AbstractEvent> tl = new HashSet <AbstractEvent>();
			
			hd.add(hdN);
			tl.add(tlN);
			res.addNode(hdN, null, succs, AbstractExecution.HEAD);			
			for (AbstractEvent s : succs)
				res.addNode(s, hd, tl, AbstractExecution.INTERNAL);				
			res.addNode(tlN, succs, null, AbstractExecution.TAIL);
		} else {
			AbstractEvent hdN = new AbstractNeutralEvent();
			
			res.addNode(hdN, null, null, AbstractExecution.HEAD);
			res.getTails().add(hdN);
		}
		return res;
	}
	
	private static final int KIND_ENTER_MONITOR = 0;
	private static final int KIND_EXIT_MONITOR = 1;
	
	private AbstractEvent createMonitorEvent (Local theVar, int kind) {
		CSP2SetVisitor visitor = new CSP2SetVisitor();
		PointsToSet pts;
		
		pts = Scene.v().getPointsToAnalysis().reachingObjects(theVar);			
		((PointsToSetInternal) pts).forall(visitor);
		if (((PointsToSetInternal) pts).size() > 1 || visitor.hasMultipleNodes())
			pts = EmptyPointsToSet.v();
		switch (kind) {
		case KIND_ENTER_MONITOR:
			return new AbstractEnterMonitorEvent(new ThreadID("", Multiplicities.UNKNOWN), pts.toString());
		case KIND_EXIT_MONITOR:
			return new AbstractExitMonitorEvent(new ThreadID("", Multiplicities.UNKNOWN), pts.toString());
		}
		return null;
	}

	/**
	 * 
	 * @param stmt
	 * @param dst
	 *  return value: should point to a nun-null empty abstract execution instance.
	 * @return
	 *  true if stmt may be a thread-creation site
	 */
	private boolean processInvoke(Unit stmt, AbstractExecution dst) {
		List<AbstractExecution> summaries = new LinkedList<AbstractExecution> ();
		Iterator<Edge> tgtIt = Scene.v().getCallGraph().edgesOutOf(stmt);
		boolean filter = true;

		while (tgtIt.hasNext()) {
			Edge tgtEdge = (Edge) tgtIt.next();
			
			if (tgtEdge.kind() != soot.Kind.CLINIT) {
				SootMethod meth = tgtEdge.getTgt().method();
			
				if (meth.getDeclaringClass().isApplicationClass() 
						&& !meth.isAbstract()
						&& meth.isDeclared()
						&& !callStack.contains(meth)) {
					Multiplicities mult = Multiplicities.UNKNOWN;
					
					if (CallGraphUtilities.v().isInLoop(stmt, graph, thisMethod))
						mult = Multiplicities.MULTIPLE;
					if (meth.getName().equals("run") && stmt.toString().contains("start"))
						filter = false;
					boolean flag = false; //is there any m in callStack that is SCC with "meth?"
					
					for (SootMethod m : callStack)
						if (CallGraphUtilities.v().SCC(m, meth))
							flag = true;
					if (flag) {
						ModularAnalyzer analyzer = new ModularAnalyzer (meth, new LinkedList <SootMethod> (callStack));
						AbstractExecution sum = analyzer.retrieveSummary().cloneAndInstantiate(LabelGenerator.getNext(),
								LabelGenerator.getNext(), mult, filter);
						
						if (meth.isSynchronized()) {
							ValueBox vb = null;
							
							if (stmt instanceof InvokeStmt)
								vb = (ValueBox) ((InvokeStmt) stmt).getInvokeExpr().getUseBoxes().iterator().next();
							else
								vb = (ValueBox) (((AssignStmt) stmt).getInvokeExpr().getUseBoxes().iterator().next());
							AbstractEvent enterMonitorEvent = createMonitorEvent((Local) vb.getValue(), KIND_ENTER_MONITOR);
							AbstractEvent exitMonitorEvent = createMonitorEvent((Local) vb.getValue(), KIND_EXIT_MONITOR);
							
							sum.substituteAnchors(enterMonitorEvent, exitMonitorEvent);
						}
						summaries.add(sum);						
					} else {
						AbstractExecution sum = MethodSummary.v().retrieveSummary(meth, LabelGenerator.getNext(),
								LabelGenerator.getNext(), mult, filter); 
					
						if (meth.isSynchronized()) {
							ValueBox vb = null;
							
							if (stmt instanceof InvokeStmt)
								vb = (ValueBox) ((InvokeStmt) stmt).getInvokeExpr().getUseBoxes().iterator().next();
							else
								vb = (ValueBox) (((AssignStmt) stmt).getInvokeExpr().getUseBoxes().iterator().next());
							AbstractEvent enterMonitorEvent = createMonitorEvent((Local) vb.getValue(), KIND_ENTER_MONITOR);
							AbstractEvent exitMonitorEvent = createMonitorEvent((Local) vb.getValue(), KIND_EXIT_MONITOR);
						
							sum.substituteAnchors(enterMonitorEvent, exitMonitorEvent);
						}
						summaries.add(sum);
					}
				}
			}
		}
		Set <AbstractEvent> heads = new HashSet <AbstractEvent> ();
		Set <AbstractEvent> tails = new HashSet <AbstractEvent> ();
		
		for (AbstractExecution s : summaries) {
			heads.addAll(s.getHeads());
			tails.addAll(s.getTails());
			dst.union(s);
		}
		if (summaries.size() > 0) {
			AbstractEvent hdN = new AbstractNeutralEvent();
			AbstractEvent tlN = new AbstractNeutralEvent();
			Set <AbstractEvent> hdSet = new HashSet <AbstractEvent> ();
			Set <AbstractEvent> tlSet = new HashSet <AbstractEvent> ();
			
			hdSet.add(hdN);
			tlSet.add(tlN);
			dst.addNode(hdN, null, heads, AbstractExecution.HEAD);
			for (AbstractEvent h : heads)
				dst.addNode(h, hdSet, null, AbstractExecution.INTERNAL);
			dst.addNode(tlN, tails, null, AbstractExecution.TAIL);
			for (AbstractEvent t : tails)
				dst.addNode(t, null, tlSet, AbstractExecution.INTERNAL);
		} else {
			AbstractEvent hdN = new AbstractNeutralEvent();
			
			dst.addNode(hdN, null, null, AbstractExecution.HEAD);
			dst.getTails().add(hdN);
		}
		return !filter;
	}
	
	private int getCategory (Unit unit) {
		if (graph.getHeads().contains(unit))
			return AbstractExecution.HEAD;
		else if (graph.getTails().contains(unit))
			return AbstractExecution.TAIL;
		else
			return AbstractExecution.INTERNAL;
	}

	private void doAnalysis () {
		//phase #1:	
		for (Unit u : graph)
			processUnit (u);					
		//phase #2:
		for (Unit unit : processedUnits.keySet()) {
			ComputedSummary cs = processedUnits.get(unit);
			Set<AbstractEvent> preds = new HashSet<AbstractEvent> ();
			Set<AbstractEvent> succs = new HashSet<AbstractEvent> ();
			Map<AbstractEvent, Integer> catMap = new HashMap<AbstractEvent, Integer>();

			for (Unit p : graph.getPredsOf(unit)) {
				AbstractEvent exit = processedUnits.get(p).getExit();

				catMap.put(exit, getCategory(p));
				preds.add(exit);
			}
			for (Unit p : graph.getSuccsOf(unit)) {
				AbstractEvent entry = processedUnits.get(p).getEntry();

				catMap.put(entry, getCategory (p));
				succs.add(entry);
			}
			if (cs.isThreadSummary()) {
				thisSummary.union((AbstractExecution) cs.getStoredValue());
				succs.addAll(((AbstractExecution) cs.getStoredValue()).getSuccsOf(cs.getEntry()));
				thisSummary.addNode(cs.getEntry(), preds, succs, getCategory(unit));
				thisSummary.getTails().addAll(((AbstractExecution) cs.getStoredValue()).getTails());
			} else if (cs.getStoredValue() instanceof AbstractEvent) //abstract event
				thisSummary.addNode((AbstractEvent) cs.getStoredValue(), preds, succs, getCategory(unit));
			else { //method summary
				AbstractExecution v = (AbstractExecution) cs.getStoredValue();
				
				thisSummary.union(v);
				thisSummary.addNode(cs.getEntry(), preds, null, AbstractExecution.INTERNAL);
				thisSummary.addNode(cs.getExit(), null, succs, getCategory(unit));
			}
		}
	}
	
	public AbstractExecution retrieveSummary () {
		return thisSummary.buildAnchors();
	}
}


class CSP2SetVisitor extends P2SetVisitor {
	private boolean mult = false; 

	public void visit(Node n) {
		try {
			if(isInLoop((AllocNode) n))
				mult = true;
		} catch (ClassCastException cce) {
			System.err.println(n.toString());
		}

	}
	
	private boolean isInLoop (AllocNode n) {
		SootMethod meth = n.getMethod();
		
		if (meth == null)
			return false;
		
		if (CallGraphUtilities.v().isRecursive(meth))
			return true;
		DirectedGraph <Unit> ug = new ExceptionalUnitGraph (meth.getActiveBody());
		
		for (Unit unit : ug)
			for (ValueBox vb : unit.getUseBoxes())
				if (vb.getValue().equivTo(n.getNewExpr()) && CallGraphUtilities.v().isInLoop(unit, ug, meth))
						return true;
		return false;
	}
	
	public boolean hasMultipleNodes () {
		return mult;
	}
}