package camelspider;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.spark.pag.*;
import soot.jimple.spark.sets.*;

public class FieldObjectLoci implements Cloneable {
	private final List<String> loci = new LinkedList<String> ();
	private String fn;
	
	private FieldObjectLoci () {
	}
	
	public FieldObjectLoci (PointsToSet pts, String field) {
		P2SetVisitor visitor = new P2SetVisitor () {
			public void visit (Node n) {
				loci.add(n.toString());
			}
		};
		
		((PointsToSetInternal) pts).forall (visitor);
		fn = field;
	}
	
	public FieldObjectLoci(StaticFieldRef sfr) {
		loci.add(sfr.toString());
		fn = "static";
	}
	
	public List <String> getLoci () {
		return loci;
	}
	
	public String getFieldName () {
		return fn;
	}
	
	public boolean hasNonEmptyIntersection (FieldObjectLoci l) {
		if (!fn.equals(l.fn))
			return false;
		for (String s : loci)
			if (l.loci.contains(s))
				return true;
		return false;
	}
	
	public String toString () {
		return "(" + loci.toString() + ", " + fn + ")";  
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof FieldObjectLoci))
			return false;
		FieldObjectLoci obj = (FieldObjectLoci) o;
		
		return loci.equals(obj.loci) && fn.equals(obj.fn);
	}
	
	public FieldObjectLoci clone () {
		FieldObjectLoci n = new FieldObjectLoci();
		
		n.fn = fn;
		for (String s : loci)
			n.loci.add(s);
		return n;
	}
	
	public int hashCode () {
		int first = 1;
		
		for (String s : loci)
			first += s.hashCode();
		return HashCodeUtilities.codePair(first, 1 + fn.hashCode());
	}
}
