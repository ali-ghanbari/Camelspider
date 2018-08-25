package camelspider;

import java.util.*;

public class ConflictingPairWarning implements Warning {
	private AbstractEvent e1;
	private AbstractEvent e2;
	
	public ConflictingPairWarning (AbstractEvent contributingEvent1, AbstractEvent contributingEvent2) {
		e1 = contributingEvent1;
		e2 = contributingEvent2;
	}
	
	public String toString () {
		return "CONFLICTING ACCESSES DUE TO " + e1 + " AND " + e2;
	}
	
	public boolean equals (Object other) {
		if (!(other instanceof ConflictingPairWarning))
			return false;
		return e1.equals(((ConflictingPairWarning) other).e1) && e2.equals(((ConflictingPairWarning) other).e2);
	}

	public Collection<String> getAffectedObjects() {
		Collection<String> res = new LinkedList<String> (((FieldObjectLoci) e1.getEventLoci()).getLoci());
		
		res.addAll(((FieldObjectLoci) e2.getEventLoci()).getLoci());
		return res;
	}
}
