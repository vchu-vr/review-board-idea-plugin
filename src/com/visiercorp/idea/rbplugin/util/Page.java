// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

/*
 * Copyright 2015 Ritesh Kapoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.visiercorp.idea.rbplugin.util;

import java.util.List;

/**
 * @author Ritesh
 */
public class Page<T> {
    private List<T> result;
    private final int offset;
    private final int count;
    private int total;

    public Page(List<T> result, int offset, int count, int total) {
        this.result = result;
        this.offset = offset;
        this.count = count;
        this.total = total;
    }

    public List<T> getResult() {
        return result;
    }

    public int getReviewIndex(T object) {
        List<T> list = getResult();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(object)) return i;
        }
        return -1;
    }

    public int getTotal() {
        return total;
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }
}
