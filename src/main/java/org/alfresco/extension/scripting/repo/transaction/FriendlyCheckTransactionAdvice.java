/*
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
package org.alfresco.extension.scripting.repo.transaction;

import org.alfresco.repo.transaction.CheckTransactionAdvice;
import org.aopalliance.intercept.MethodInvocation;
/**
 * Work around : A transaction has not be started for method 'toString'
 * @author deas
 */
public class FriendlyCheckTransactionAdvice extends CheckTransactionAdvice {

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Class declaringClass = methodInvocation.getMethod().getDeclaringClass();
		if (declaringClass instanceof Object) {
			return methodInvocation.proceed();
		}
		return super.invoke(methodInvocation);
	}
	
	
}
