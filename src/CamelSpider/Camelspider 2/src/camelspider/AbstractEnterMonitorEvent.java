package camelspider;

public class AbstractEnterMonitorEvent extends AbstractEvent {
	private String eventLoci; //never null!

	public AbstractEnterMonitorEvent(ThreadID threadID, String eventLoci) {
		super(threadID);
		this.eventLoci = eventLoci;
	}

	public Object getEventLoci() {
		return eventLoci;
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof AbstractEnterMonitorEvent))
			return false;
		AbstractEnterMonitorEvent obj = (AbstractEnterMonitorEvent) o;
		
		return eventID.equals(obj.eventID) && threadID.equals(obj.threadID) && eventLoci.equals(obj.eventLoci);
	}
	
	public AbstractEvent clone () {
		AbstractEnterMonitorEvent n = new AbstractEnterMonitorEvent(threadID.clone(), eventLoci);
		
		n.eventID = eventID;
		return n;
	}

	public int hasCode() {
		return HashCodeUtilities.codePair(1 + eventID.hashCode(), HashCodeUtilities.codePair(threadID.hashCode(),
						HashCodeUtilities.codePair(HashCodeUtilities.MONITOR_N_HASH, 1 + eventLoci.hashCode())));
	}
}
