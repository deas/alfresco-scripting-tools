/*
 * Andreas Steffan - http://www.contentreich.de
 *
 * Created on Sep 30, 2011
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.extension.scripting.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import de.contentreich.scripting.service.JavascriptShellService;

public class JavascriptServiceGet extends DeclarativeWebScript {
	private JavascriptShellService javascriptShellService;

	public JavascriptShellService getJavascriptShellService() {
		return javascriptShellService;
	}

	public void setJavascriptShellService(JavascriptShellService javascriptShellService) {
		this.javascriptShellService = javascriptShellService;
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status) {
		Map<String, Object> model = new HashMap<String, Object>(7, 1.0f);
		model.put("javascriptShellService", getJavascriptShellService());
		return model;
	}
}