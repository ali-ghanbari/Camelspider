package camelspider;

public class AbstractNeutralEvent extends AbstractEvent {
	public AbstractNeutralEvent() {
		super(null);
	}

	public Object getEventLoci() {
		return null;
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof AbstractNeutralEvent))
			return false;
		AbstractNeutralEvent obj = (AbstractNeutralEvent) o;
		
		return eventID.equals(obj.eventID);
	}
	
	public AbstractEvent clone () {
		AbstractNeutralEvent n = new AbstractNeutralEvent();
		
		n.eventID = eventID;
		return n;
	}

	public int hasCode() {
		return HashCodeUtilities.codePair(1 + eventID.hashCode(), HashCodeUtilities.NEUTRAL_HASH);
	}
}
