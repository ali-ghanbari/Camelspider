package camelspider;

public class AbstractReadEvent extends AbstractEvent {
	private FieldObjectLoci eventLoci;

	public AbstractReadEvent(ThreadID threadID, FieldObjectLoci eventLoci) {
		super(threadID);
		this.eventLoci = eventLoci;
	}

	public Object getEventLoci() {
		return eventLoci;
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof AbstractReadEvent))
			return false;
		AbstractReadEvent obj = (AbstractReadEvent) o;
		
		return eventID.equals(obj.eventID) && threadID.equals(obj.threadID) && eventLoci.equals(obj.eventLoci);
	}
	
	public AbstractEvent clone () {
		AbstractReadEvent n = new AbstractReadEvent(threadID.clone(), eventLoci.clone());
		
		n.eventID = eventID;
		return n;
	}

	public int hasCode() {
		return HashCodeUtilities.codePair(1 + eventID.hashCode(), HashCodeUtilities.codePair(threadID.hashCode(),
				HashCodeUtilities.codePair(HashCodeUtilities.OBJECT_R_HASH, eventLoci.hashCode())));
	}
}
