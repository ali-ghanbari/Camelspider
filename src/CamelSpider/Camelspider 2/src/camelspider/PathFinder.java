package camelspider;

public class PathFinder {
	private AbstractExecution graph;
	
	public PathFinder (AbstractExecution abstractExecution) {
		graph = abstractExecution;
	}
	
	public boolean pathFinder (AbstractEvent src, AbstractEvent dst) {
		class PathVisitor implements AbstractEventVisitor {
			private AbstractEvent target;
			public boolean found;
			
			public PathVisitor (AbstractEvent target) {
				found = false;
				this.target = target;
			}

			public boolean visit(AbstractEvent event) {
				if (event.equals(target))
					found = true;
				return found;
			}
		}
		PathVisitor pv = new PathVisitor (dst);
		
		graph.doDepthFirstSearch(src, true, pv);
		return pv.found;
	}
	
	public boolean isInLoop (AbstractEvent event) {
		class LoopVisitor implements AbstractEventVisitor {
			private AbstractEvent target;
			public boolean found;
			
			public LoopVisitor (AbstractEvent target) {
				found = false;
				this.target = target;
			}

			public boolean visit(AbstractEvent event) {
				if (event.equals(target))
					found = true;
				return found;
			}
		}
		LoopVisitor loopVisitor = new LoopVisitor (event);
		
		for (AbstractEvent s : graph.getSuccsOf(event)) {
			graph.doDepthFirstSearch(s, true, loopVisitor);
			if (loopVisitor.found)
				return true;
		}
		return false;
	}
}
