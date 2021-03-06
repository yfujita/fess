/*
 * Copyright 2012-2015 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.app.web.error;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ErrorForm implements Serializable {

    private static final long serialVersionUID = 1L;

    public Map<String, String[]> fields = new HashMap<>();

    public String q;

    public String url;

    public String num;

    public String sort;

    public String lang;
}
