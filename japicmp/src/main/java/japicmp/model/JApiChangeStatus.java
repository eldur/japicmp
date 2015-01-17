package japicmp.model;

import com.google.common.base.Joiner;

public enum JApiChangeStatus {
    NEW, REMOVED, UNCHANGED, MODIFIED;

	public static String knownValues() {
		return Joiner.on(" ").join(values());
	}
}
