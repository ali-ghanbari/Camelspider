package camelspider;

import java.util.*;
import java.io.*;

public class PrettyPrinter {
	private List <Warning> wl;
	private PrintStream outLog;
	private Map <String, List <Warning>> mwrObjectRaces;
	private Map <String, List <Warning>> confObjectRaces;
	
	public PrettyPrinter (boolean generateLogFile, String outputFileName) throws FileNotFoundException {
		wl = new LinkedList <Warning> ();
		outLog = generateLogFile ? new PrintStream (outputFileName) : null;
		mwrObjectRaces = new HashMap <String, List <Warning>> ();
		confObjectRaces = new HashMap <String, List <Warning>> ();
	}
	
	public List <Warning> getWarningsList () {
		return wl;
	}
	
	private void groupWarnings () {
		Map <String, List <Warning>> store;
		
		for (Warning w : wl) {
			for (String obj : w.getAffectedObjects()) {
				if (w instanceof MultipleWriteWarning)
					store = mwrObjectRaces;
				else
					store = confObjectRaces;
				if (store.get(obj) == null) {
					List <Warning> l = new LinkedList <Warning> ();
					
					l.add(w);
					store.put(obj, l);
				} else if (!store.get(obj).contains(obj))
					store.get(obj).add(w);
			}
		}
	}
	
	private void generateLog () {
		PrintStream printer = outLog;
		int i = 1;
		
		if (outLog == null)
			printer = System.out;
		printer.println("List of multiple-write warnings:");
		for (Warning w : wl)
			if (w instanceof MultipleWriteWarning)
				printer.println("\t(" + (i ++) + ") " + w);
		i = 1;
		printer.println("");
		printer.println("List of conflicting-pair warnings:");
		for (Warning w : wl)
			if (w instanceof ConflictingPairWarning)
				printer.println("\t(" + (i ++) + ") " + w);
	}
	
	public void printOut () {
		groupWarnings();
		System.out.println("\nThere is " + mwrObjectRaces.size() + " multiple-write, and " + confObjectRaces.size() + " conflicting-pair warning(s)");
		generateLog();
	}
}
