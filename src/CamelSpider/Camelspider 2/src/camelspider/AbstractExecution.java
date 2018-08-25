package camelspider;

import java.util.*;

import soot.jimple.spark.sets.EmptyPointsToSet;

public class AbstractExecution {
	public static final int INTERNAL = 0;
	public static final int HEAD = 1;
	public static final int TAIL = 2;
	
	private Set<AbstractEvent> nodes;
	private Map<AbstractEvent, Set<AbstractEvent>> preds; //every node in "nodes" need not have a preds/succs set
	private Map<AbstractEvent, Set<AbstractEvent>> succs;
	private Set<AbstractEvent> heads; //a finished graph must have heads and also tails
	private Set<AbstractEvent> tails;
	//DFS algorithm's local variables
	private boolean forward;
	private AbstractEventVisitor visitor;
	private Set<AbstractEvent> visited;
	
	public AbstractExecution () {
		nodes = new HashSet<AbstractEvent>();
		preds = new HashMap<AbstractEvent, Set<AbstractEvent>>();
		succs = new HashMap<AbstractEvent, Set<AbstractEvent>>();
		heads = new HashSet<AbstractEvent>();
		tails = new HashSet<AbstractEvent>();
		visitor = null;
		visited = null;
	}
		
	public Set <AbstractEvent> getHeads () {
		return heads;
	}
	
	public Set <AbstractEvent> getTails () {
		return tails;
	}
	
	public Set <AbstractEvent> getNodes () {
		return nodes;
	}
	
	public Set <AbstractEvent> getPredsOf (AbstractEvent node) {
		if (!nodes.contains(node)) return null;
		Set <AbstractEvent> preds = this.preds.get(node);
		
		return (preds == null) ? (new HashSet <AbstractEvent> ()) : preds;
	}
	
	public Set <AbstractEvent> getSuccsOf (AbstractEvent node) {
		if (!nodes.contains(node)) return null;
		Set <AbstractEvent> succs = this.succs.get(node);
		
		return (succs == null) ? (new HashSet <AbstractEvent> ()) : succs;
	}
	
	private void doDepthFirstSearch (Set <AbstractEvent> queue) {
		Iterator <AbstractEvent> eIt = queue.iterator();
		boolean leaveDescendants; 
		
		while (eIt.hasNext()) {
			AbstractEvent event = eIt.next();
			
			if (!visited.contains(event)) {				
				visited.add(event);
				leaveDescendants = visitor.visit(event); 
				if (forward) {
					if (!leaveDescendants) 
						doDepthFirstSearch(getSuccsOf (event));
				} else
					if (!leaveDescendants)
						doDepthFirstSearch(getPredsOf(event));
			}
		}
	}
		
	public void doDepthFirstSearch (AbstractEvent origin, boolean forward, AbstractEventVisitor visitor) {
		this.forward = forward;
		this.visitor = visitor;
		if (visited == null)
			visited = new HashSet <AbstractEvent> ();
		else
			visited.clear();
		visited.add(origin);
		if (!visitor.visit(origin))
			doDepthFirstSearch(forward ? getSuccsOf (origin) : getPredsOf (origin));
	}
	
	/**
	 * Returns an instantiated clone of "this" if "fiterTID" == true then "tid" instantiation takes place only
	 * for those events with "old_tid" # "", otherwise instantiation will take place unconditionally.
	 */
	public AbstractExecution cloneAndInstantiate (String eidPrefix, String tidPrefix,
			Multiplicities mult, boolean filterTID) {
		Map <AbstractEvent, AbstractEvent> oldToNew = new HashMap <AbstractEvent, AbstractEvent> ();
		AbstractExecution n = new AbstractExecution ();
		
		for (AbstractEvent e : nodes) {
			AbstractEvent eCloned = e.clone();
			
			eCloned.instantiateEventID(eidPrefix);
			if (!filterTID || (eCloned.getThreadID() != null && !eCloned.getThreadID().getThreadID().isEmpty()))
				eCloned.instantiateThreadID(tidPrefix, mult);
			oldToNew.put(e, eCloned);
			n.nodes.add(eCloned);
		}
		for (AbstractEvent e : preds.keySet()) {
			Set <AbstractEvent> p = new HashSet <AbstractEvent> ();
			Set <AbstractEvent> eSet = preds.get(e);
			
			for (AbstractEvent ePrime : eSet)
				p.add(oldToNew.get(ePrime));
			n.preds.put(oldToNew.get(e), p);
		}
		for (AbstractEvent e : succs.keySet()) {
			Set <AbstractEvent> s = new HashSet <AbstractEvent> ();
			Set <AbstractEvent> eSet = succs.get(e);
			
			for (AbstractEvent ePrime : eSet)
				s.add(oldToNew.get(ePrime));
			n.succs.put(oldToNew.get(e), s);
		}
		for (AbstractEvent e : heads)
			n.heads.add(oldToNew.get(e));
		for (AbstractEvent e : tails)
			n.tails.add(oldToNew.get(e));
		return n;
	}
	
	public boolean equals (Object o) {
		System.err.println ("equals is called on AbstractExecution!");
		return true; //implement!
	}
	
	public AbstractExecution buildAnchors () {
		AbstractEvent headAnchor = new AbstractNeutralEvent();
		AbstractEvent tailAnchor = new AbstractNeutralEvent();
		Set<AbstractEvent> newHeads = new HashSet<AbstractEvent>();
		Set<AbstractEvent> newTails = new HashSet<AbstractEvent>();
		
		newHeads.add(headAnchor);
		newTails.add(tailAnchor);
		nodes.add(headAnchor);
		nodes.add(tailAnchor);
		for (AbstractEvent h : heads)
			preds.put(h, newHeads);
		succs.put(headAnchor, heads);
		for (AbstractEvent t : tails)
			succs.put(t, newTails);
		preds.put(tailAnchor, tails);
		heads = newHeads;
		tails = newTails;
		return this;
	}
	
	public boolean substituteAnchors (AbstractEvent headEvent, AbstractEvent tailEvent) {
		if (heads.size() != 1 || tails.size() != 1)
			return false;
		AbstractEvent headAnchor = heads.iterator().next();
		AbstractEvent tailAnchor = tails.iterator().next();
		
		if (headAnchor instanceof AbstractNeutralEvent && tailAnchor instanceof AbstractNeutralEvent) {
			Set <AbstractEvent> oldHeadS = succs.remove(headAnchor);
			Set <AbstractEvent> oldTailP = preds.remove(tailAnchor);
			
			nodes.remove(headAnchor);
			nodes.remove(tailAnchor);
			nodes.add(headEvent);
			nodes.add(tailEvent);
			heads.remove(headAnchor);
			tails.remove(tailAnchor);
			heads.add(headEvent);
			tails.add(tailEvent);
			for (AbstractEvent e : succs.keySet()) {
				Set<AbstractEvent> succSet = succs.get(e);
				
				if (succSet.contains(headAnchor)) {
					succSet.remove(headAnchor);
					succSet.add(headEvent);
				}
				if (succSet.contains(tailAnchor)) {
					succSet.remove(tailAnchor);
					succSet.add(tailEvent);
				}
			}
			for (AbstractEvent e : preds.keySet()) {
				Set<AbstractEvent> predSet = preds.get(e);
				
				if (predSet.contains(headAnchor)) {
					predSet.remove(headAnchor);
					predSet.add(headEvent);
				}
				if (predSet.contains(tailAnchor)) {
					predSet.remove(tailAnchor);
					predSet.add(tailEvent);
				}
			}
			succs.put(headEvent, oldHeadS);
			preds.put(tailEvent, oldTailP);
			return true;
		}
		return false;
	}
	
	public AbstractExecution addNode (AbstractEvent node, Set <AbstractEvent> preds, 
			Set <AbstractEvent> succs, int category) {
		nodes.add(node);
		if (preds != null) {
			nodes.addAll(preds);
			this.preds.put(node, preds);
		}
		if (succs != null) {
			this.succs.put(node, succs);
			nodes.addAll(succs);
		}
		switch (category) {
		case HEAD:
			this.heads.add(node);
			break;
		case TAIL:
			this.tails.add(node);			
		}
		return this;
	}
	
	public AbstractExecution union (AbstractExecution e) {
		nodes.addAll(e.nodes);
		Iterator <AbstractEvent> it = e.preds.keySet().iterator();
		
		while (it.hasNext()) {
			AbstractEvent node = it.next();
			
			preds.put(node, e.preds.get(node));
		}
		it = e.succs.keySet().iterator();
		while (it.hasNext()) {
			AbstractEvent node = it.next();
			
			succs.put(node, e.succs.get(node));
		}
		return this;
	}
	
	private List<AbstractEvent> netXsAfter(final AbstractEvent event) {
		final List<AbstractEvent> res = new LinkedList <AbstractEvent>();
		AbstractEventVisitor monitorVisitor = new AbstractEventVisitor() {
			List<AbstractEvent> tmp = new LinkedList <AbstractEvent>();
			
			public boolean visit(AbstractEvent e) {
				if (e.getThreadID() != null && event.getThreadID() != null)
					if (!e.getThreadID().equals(event.getThreadID()))
						return true;
				if (e instanceof AbstractEnterMonitorEvent)
					tmp.add(e);
				else if (e instanceof AbstractExitMonitorEvent) {
					AbstractEvent m = null;
					
					for (AbstractEvent t : tmp)						
						if (t.mayMatch(e))
							m = t;
					if (m != null)
						tmp.remove(m);
					else
						res.add(e);
				}
				return false;
			}
		};
		
		doDepthFirstSearch(event, true, monitorVisitor);
		return res;
	}

	private List<AbstractEvent> netNsBefore(final AbstractEvent event) {
		final List<AbstractEvent> res = new LinkedList <AbstractEvent>();
		AbstractEventVisitor monitorVisitor = new AbstractEventVisitor() {
			List<AbstractEvent> tmp = new LinkedList <AbstractEvent>();
			
			public boolean visit(AbstractEvent e) {
				if (e.getThreadID() != null && event.getThreadID() != null)
					if (!e.getThreadID().equals(event.getThreadID()))
						return true;
				if (e instanceof AbstractExitMonitorEvent)
					tmp.add(e);
				else if (e instanceof AbstractEnterMonitorEvent) {
					AbstractEvent m = null;
					
					for (AbstractEvent t : tmp)						
						if (t.mayMatch(e))
							m = t;
					if (m != null)
						tmp.remove(m);
					else
						res.add(e);
				}
				return false;
			}
		};
		
		doDepthFirstSearch(event, false, monitorVisitor);
		return res;
	}
	
	private boolean isGuarded (AbstractEvent event) {
		List <AbstractEvent> netN = netNsBefore(event);
		
		for (AbstractEvent e : netN)
			if (e.getThreadID().equals(event.getThreadID())
					&& !((String) e.getEventLoci()).equals(EmptyPointsToSet.v().toString()))
					return true;
		return false;
	}
	
	private boolean ordPrime(AbstractEvent e1, AbstractEvent e2) {
		if (e1 instanceof AbstractNeutralEvent || e2 instanceof AbstractNeutralEvent)
			return false;
		//simplest check
		if (e1.getThreadID().equals(e2.getThreadID()))
			return true;
		//monitor events check
		if (e1.isMonitorEvent() && e2.isMonitorEvent() && !e1.getClass().getName().equals(e2.getClass().getName()))
			if (e1.getEventLoci().equals(e2.getEventLoci()) && !((String) e1.getEventLoci()).isEmpty())
				return true;
		if (e1.isMonitorEvent() && e2.isMonitorEvent())
			return false;
		//last resort: looking for paths between the events
		PathFinder pf = new PathFinder (this);
		AbstractEvent srcEvent = null;
		
		if (pf.pathFinder (e1, e2))
			srcEvent = e1;
		else if (pf.pathFinder (e2, e1))
			srcEvent = e2;
		if (srcEvent != null)
			if (!pf.isInLoop(srcEvent))
				return true;
		//all cases have been exhausted
		return false;
	}
	
	private boolean ord(AbstractEvent e1, AbstractEvent e2) {
		if (ordPrime(e1, e2))
			return true;
		List<AbstractEvent> netNsBeforeE1 = netNsBefore(e1);
		List<AbstractEvent> netXsAfterE2 = netXsAfter(e2);
		
		if(netXsAfterE2.size() == 0 || netNsBeforeE1.size() == 0)
			return false;
		for (AbstractEvent eN1 : netNsBeforeE1)
			for (AbstractEvent eX2 : netXsAfterE2)
				if (ordPrime(eN1, eX2))
					return true;
		return false;
	}
	
	public boolean isSafe(List<Warning> warnings) {
		List<AbstractEvent> accessEvents = new ArrayList<AbstractEvent>();
		boolean flag = true;
		int progress = 0;
		
		for(AbstractEvent e : nodes) {
			if(e instanceof AbstractReadEvent || e instanceof AbstractWriteEvent) {
				if(e.mwr() && !isGuarded(e))
					if (!ThreadEscapeAnalysis.v().allCaptured(((FieldObjectLoci)e.getEventLoci()).getLoci())) {
						warnings.add(new MultipleWriteWarning((AbstractWriteEvent) e));
						flag = false;
					}
				accessEvents.add(e);
			}
		}
		System.out.println("Access events are computed (total size = " + accessEvents.size() + ").");
		for(int i = 0; i < accessEvents.size(); i++) {
			AbstractEvent e1 = accessEvents.get(i);
			
			if (!e1.marked()) {
				e1.markEvent();
				for(int j = i + 1; j < accessEvents.size(); j ++) {
					AbstractEvent e2 = accessEvents.get(j);
					
					if(!e2.marked() && e1.conf(e2) && !ord(e1, e2)) {
						Set<String> loci = new HashSet<String> (((FieldObjectLoci) e1.getEventLoci()).getLoci());
						
						e2.markEvent();
						loci.retainAll(((FieldObjectLoci) e2.getEventLoci()).getLoci());	
						if (!ThreadEscapeAnalysis.v().allCaptured(loci)) {
							warnings.add(new ConflictingPairWarning(e1, e2));
							flag = false;
						}
					}
				}
			}	
			progress++;
			if ((progress % 100) == 0)
				System.out.println (progress + " nodes have been processed out of " + accessEvents.size() + " nodes.");
		}
		return flag;
	}
}