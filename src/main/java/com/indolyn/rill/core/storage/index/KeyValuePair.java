package com.indolyn.rill.core.storage.index;

import com.indolyn.rill.core.common.model.RID;
import com.indolyn.rill.core.common.model.Value;

public record KeyValuePair(Value key, RID rid) {
}
