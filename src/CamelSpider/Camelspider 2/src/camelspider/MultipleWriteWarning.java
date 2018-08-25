package camelspider;

import java.util.*;

public class MultipleWriteWarning implements Warning {
	private AbstractWriteEvent e;
	
	public MultipleWriteWarning (AbstractWriteEvent contributingEvent) {
		e = contributingEvent;
	}
	
	public String toString () {
		return "MULTIPLE WRITE DUE TO " + e;
	}
	
	public boolean equals (Object other) {
		if (!(other instanceof MultipleWriteWarning))
			return false;
		return e.equals(((MultipleWriteWarning) other).e);
	}

	public Collection<String> getAffectedObjects() {
		return ((FieldObjectLoci) e.getEventLoci()).getLoci();
	}
}
