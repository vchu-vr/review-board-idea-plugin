// Copyright (c) [2010-2016] Visier Solutions Inc. All rights reserved.

//
// Copyright © [2010-2016] Visier Solutions Inc. All rights reserved.
//
package com.visiercorp.idea.rbplugin.util;

/**
 * @author ritesh
 */
public interface ThrowableFunction<P, R> {
    R throwableCall(P params) throws Exception;
}
