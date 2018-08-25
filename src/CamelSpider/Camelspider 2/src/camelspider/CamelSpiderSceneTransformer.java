package camelspider;

import java.io.*;
import java.util.*;

import soot.*;

public class CamelSpiderSceneTransformer extends SceneTransformer {
	private boolean logFile;
	
	public CamelSpiderSceneTransformer (boolean generateLogFile) {
		logFile = generateLogFile;
	}
	
	@SuppressWarnings("rawtypes")
	protected void internalTransform(String phaseName, Map options) {
		SootMethod mainMeth = Scene.v().getMainMethod();
		PrettyPrinter printer = null;
		
		try {
			printer = new PrettyPrinter(logFile, "/home/ali/csLog.log");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ThreadEscapeAnalysis.v().start();
		Driver.v().start();
		try {
			ThreadEscapeAnalysis.v().join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		AbstractExecution mainSummary = MethodSummary.v().retrieveSummary(mainMeth, "", "main",
				Multiplicities.UNIQUE, false);
		
		System.out.println("All methods have been analyzed.");
		System.out.println("Relaxing memory space...");
		MethodSummary.v().relaxMemory();
		System.out.println("Please wait while checking results...");
		System.out.flush();
		if (mainSummary.isSafe(printer.getWarningsList()))
			System.out.println ("\nThe program is safe!\n");
		else 
			printer.printOut();
		
		try {
			int i = 1;
			Map <AbstractEvent, String> mm = new HashMap <AbstractEvent, String> ();
			@SuppressWarnings("resource")
			PrintStream pw = new PrintStream ("/home/ali/graph.gv");
			Set <String> succ = new HashSet <String> ();

			AbstractExecution sum = mainSummary;

			for (AbstractEvent e : sum.getNodes())
				mm.put(e, "N" + (i ++));
			pw.println("digraph G {");
			for (AbstractEvent e : sum.getNodes()) {
				succ.clear();
				for (AbstractEvent s : sum.getSuccsOf(e))
					succ.add(mm.get(s));
				for (String s : succ)
					pw.println(mm.get(e) + "->" + s + ";");
			}
			for (AbstractEvent e : sum.getNodes())
				pw.println(mm.get(e) + "[label=\"{" + e + "}\"];");
			pw.println("}");
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
