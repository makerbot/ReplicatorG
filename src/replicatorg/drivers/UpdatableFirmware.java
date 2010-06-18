package replicatorg.drivers;

import java.util.List;

public interface UpdatableFirmware {
	void updateToVersion(Version v);
	List<Version> getAvailableVersions();
}
