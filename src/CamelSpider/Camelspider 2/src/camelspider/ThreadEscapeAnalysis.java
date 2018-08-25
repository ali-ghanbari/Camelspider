package camelspider;

import java.io.*;
import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.spark.pag.*;
import soot.jimple.spark.sets.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.*;

public class ThreadEscapeAnalysis extends Thread {
	private static ThreadEscapeAnalysis instance = null;
	private Set<Integer> escaped;

	private ThreadEscapeAnalysis () {
		escaped = new HashSet<Integer>();
	}
	
	public static ThreadEscapeAnalysis v () {
		if (instance == null)
			instance = new ThreadEscapeAnalysis();
		return instance;
	}
	
	private boolean isWantedMethod (SootMethod method) {
		if (method.getDeclaringClass().isApplicationClass() && !method.isAbstract())
			if (!method.isEntryMethod() || method.isMain())
				return true;
		return false;
	}
	
	private void cleanUpDisk () {
		String[] fn = {"pointsto.tuples", "reachablefromstatics.tuples", "trivialescape.tuples"};
		
		for (int i = 0; i < fn.length; i ++)
			(new File (fn [i])).delete();
	}
	
	public void run () {
		PrintStream pointsTo = null;
		
		cleanUpDisk();
		
		try {
			pointsTo = new PrintStream ("pointsto.tuples");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		pointsTo.println ("##"); //print out header
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		Iterator <Object> nit = pag.allocSourcesIterator();
		
		while (nit.hasNext()) {
			AllocNode an = (AllocNode) nit.next();
			SootMethod creator = an.getMethod();
			
			if (creator != null && creator.getDeclaringClass().isApplicationClass()) {
				int src = PAGNodeManager.v().mappedWhere(an.toString());
				
				if (src < 0)
					src = PAGNodeManager.v().put(an.toString());
				@SuppressWarnings("rawtypes")
				Iterator fit = an.getAllFieldRefs().iterator();
				
				while (fit.hasNext()) {
					AllocDotField adf = (AllocDotField) fit.next();
					
					if (adf.getField() instanceof SootField) {
						SootField theField = (SootField) adf.getField();
						
						if (theField.getDeclaringClass().isApplicationClass()) {
							PointsToSetInternal ptsContainingAN = new HashPointsToSet (an.getType(), pag);
							
							ptsContainingAN.add(an);
							PointsToSetInternal ptsi = (PointsToSetInternal)
									pag.reachingObjects(ptsContainingAN, theField);
							
							ptsi.forall(new FirstP2SetVisitor(src, pointsTo));
						}
					}
				}
			}
		}
		
		pointsTo.close();
		PrintStream reachableFromStatics = null;
		Set<String> reachableFromStaticsSet = new HashSet<String> ();
		
		try {
			reachableFromStatics = new PrintStream ("reachablefromstatics.tuples");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		reachableFromStatics.println("##"); //header
		for (SootClass cls : Scene.v().getApplicationClasses())
			for (SootField field : cls.getFields())
				if (field.isStatic()) {
					PointsToSetInternal ptsi = (PointsToSetInternal) 
							Scene.v().getPointsToAnalysis().reachingObjects(field);
					
					ptsi.forall(new SecondP2SetVisitor(reachableFromStaticsSet));
				}
		
		for(String s : reachableFromStaticsSet) {
			int no = PAGNodeManager.v().mappedWhere(s);
			
			if (no < 0)
				no = PAGNodeManager.v().put(s);
			reachableFromStatics.println (no);
		}
		
		reachableFromStatics.close();
		Set<String> triviallyEscaped = new HashSet<String> ();
		
		for (SootClass cls : Scene.v().getApplicationClasses())
			for (SootMethod meth : cls.getMethods())
				if (isWantedMethod(meth)) {
					DirectedGraph<Unit> dg = new ExceptionalUnitGraph (meth.retrieveActiveBody());
					
					for (Unit unit : dg)
						if (unit instanceof InvokeStmt)
							processInvokeStmt (unit, triviallyEscaped);
				}
		
		PrintStream trivialPS = null;
		
		try {
			trivialPS = new PrintStream ("trivialescape.tuples");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		trivialPS.println("##");
		for(String s : triviallyEscaped) {
			int no = PAGNodeManager.v().mappedWhere(s);
			
			if (no < 0)
				no = PAGNodeManager.v().put(s);
			trivialPS.println (no);
		}
		trivialPS.close();
		/*calling the solver*/
		String[] solverArgs = {"thea.dlog"};
		try {
			net.sf.bddbddb.Solver.main(solverArgs);
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		/*loading the results*/
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try {
			is = new FileInputStream("escaped.tuples");
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = null;
		
		try {
			br.readLine(); //skip the header
			while ((line = br.readLine()) != null)
				escaped.add(new Integer (line.trim()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void processInvokeStmt (Unit stmt, Set<String> triviallyEscaped) {
		Iterator<Edge> tgtIt = Scene.v().getCallGraph().edgesOutOf(stmt);

		while (tgtIt.hasNext()) {
			Edge tgtEdge = (Edge) tgtIt.next();
			
			if (tgtEdge.kind() != soot.Kind.CLINIT) {
				SootMethod meth = tgtEdge.getTgt().method();
			
				if (isWantedMethod(meth))
					if (meth.getName().equals("run") && stmt.toString().contains("start")) {
						ValueBox vb = (ValueBox) ((InvokeStmt) stmt).getInvokeExpr().getUseBoxes().iterator().next();						
						PointsToSetInternal ptsi = (PointsToSetInternal)
								Scene.v().getPointsToAnalysis().reachingObjects((Local) vb.getValue());
						
						ptsi.forall(new SecondP2SetVisitor(triviallyEscaped));
					}
			}
		}
	}
	
	public boolean threadEscaped (String node) {
		return escaped.contains(PAGNodeManager.v().mappedWhere(node));
	}
	
	public boolean allCaptured (Collection<String> pts) {
		for (String s : pts)
			if (threadEscaped(s))
				return false;
		return true;
	}
}

class PAGNodeManager {
	private static PAGNodeManager instance = null;
	private Map<String, Integer> pagMap;
	private int pagIDCounter;

	private PAGNodeManager () {
		pagMap = new HashMap<String, Integer>();
		pagIDCounter = 0;
	}
	
	public static PAGNodeManager v () {
		if (instance == null)
			instance = new PAGNodeManager();
		return instance;
	}
	
	public int mappedWhere (String pagNode) {
		Integer r = pagMap.get(pagNode);
		
		return (r == null) ? -1 : r;
	}
	
	public int put (String pagNode) {
		pagMap.put(pagNode, pagIDCounter);
		return pagIDCounter ++;
	}
}

class FirstP2SetVisitor extends P2SetVisitor {
	private PrintStream ps;
	private int src;
	
	public FirstP2SetVisitor (int src, PrintStream ps) {
		this.src = src;
		this.ps = ps;
	}
	
	public void visit(Node node) {
		AllocNode an = null;
		int uniqueMaker = 0;
		
		if (node instanceof AllocNode)
			an = (AllocNode) node;
		if (an != null) {
			SootMethod creator = an.getMethod();
			
			if (creator != null && creator.getDeclaringClass().isApplicationClass()) {
				int dst = PAGNodeManager.v().mappedWhere(an.toString());
				
				if (dst < 0)
					dst = PAGNodeManager.v().put(an.toString());
				ps.println(src + " " + (uniqueMaker ++) + " " + dst); //uniqueMaker avoids repetitions; we will ignore it
			}
		}
	}
}

class SecondP2SetVisitor extends P2SetVisitor {
	private Set <String> theSet;
	
	public SecondP2SetVisitor (Set<String> theSet) {
		this.theSet = theSet;
	}

	public void visit(Node node) {
		AllocNode an = null;
		
		if (node instanceof AllocNode)
			an = (AllocNode) node;
		if (an != null) {
			SootMethod creator = an.getMethod();
			
			if (creator != null && creator.getDeclaringClass().isApplicationClass())
				theSet.add(an.toString());
		}
	}
}