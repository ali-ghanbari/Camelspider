package camelspider;

public class AbstractWriteEvent extends AbstractEvent {
	private FieldObjectLoci eventLoci;

	public AbstractWriteEvent(ThreadID threadID, FieldObjectLoci eventLoci) {
		super(threadID);
		this.eventLoci = eventLoci;
	}

	public Object getEventLoci() {
		return eventLoci;
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof AbstractWriteEvent))
			return false;
		AbstractWriteEvent obj = (AbstractWriteEvent) o;
		
		return eventID.equals(obj.eventID) && threadID.equals(obj.threadID) && eventLoci.equals(obj.eventLoci);
	}
	
	public AbstractEvent clone () {
		AbstractWriteEvent n = new AbstractWriteEvent(threadID.clone(), eventLoci.clone());
		
		n.eventID = eventID;
		return n;
	}

	public int hasCode() {
		return HashCodeUtilities.codePair(1 + eventID.hashCode(), HashCodeUtilities.codePair(threadID.hashCode(),
				HashCodeUtilities.codePair(HashCodeUtilities.OBJECT_W_HASH, eventLoci.hashCode())));
	}
}
