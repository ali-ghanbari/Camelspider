package camelspider;

public enum Multiplicities {
	UNIQUE, MULTIPLE, UNKNOWN;

	public Multiplicities oTimes(Multiplicities m2) {
		if (this == MULTIPLE || m2 == MULTIPLE)
			return MULTIPLE;
		if (this == UNKNOWN && m2 == UNKNOWN)
			return UNKNOWN;
		return UNIQUE;
	}

	public String toString(Multiplicities m) {
		switch (m) {
		case UNIQUE:
			return "1";
		case MULTIPLE:
			return "*";
		default:
			return "?";
		}
	}
	
	public int hasCode () {
		switch (this) {
		case UNIQUE:
			return 1;
		case MULTIPLE:
			return 2;
		default:
			return 3;
		}
	}
}
