package org.globsframework.json;

import org.globsframework.model.ChangeSet;

public interface PreChangeSet {
    ChangeSet resolve(GlobAccessor globAccessor);
}
