package camelspider;

public class AbstractExitMonitorEvent extends AbstractEvent {
	private String eventLoci; //never null!

	public AbstractExitMonitorEvent(ThreadID threadID, String eventLoci) {
		super(threadID);
		this.eventLoci = eventLoci;
	}

	public Object getEventLoci() {
		return eventLoci;
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof AbstractExitMonitorEvent))
			return false;
		AbstractExitMonitorEvent obj = (AbstractExitMonitorEvent) o;
		
		return eventID.equals(obj.eventID) && threadID.equals(obj.threadID) && eventLoci.equals(obj.eventLoci);
	}
	
	public AbstractEvent clone () {
		AbstractExitMonitorEvent n = new AbstractExitMonitorEvent(threadID.clone(), eventLoci);
		
		n.eventID = eventID;
		return n;
	}

	public int hasCode() {
		return HashCodeUtilities.codePair(1 + eventID.hashCode(), HashCodeUtilities.codePair(threadID.hashCode(),
				HashCodeUtilities.codePair(HashCodeUtilities.MONITOR_X_HASH, 1 + eventLoci.hashCode())));
	}
}
