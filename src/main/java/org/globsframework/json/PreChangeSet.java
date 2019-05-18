package org.globsframework.json;

import org.globsframework.model.ChangeSet;

interface PreChangeSet {
    ChangeSet resolve(GlobAccessor globAccessor);
}
