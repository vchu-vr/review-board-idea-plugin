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

package com.visiercorp.idea.rbplugin.reviewboard.model;

/**
 * @author ritesh on 24/7/15.
 */
public class RBGroupList extends RBModel {

    public Group groups[];

    public static class Group {
        public String display_name;
        public String name;
        public int id;
        public String visible;
    }
}
