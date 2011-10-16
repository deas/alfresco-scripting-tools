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
package org.alfresco.extension.scripting.javascript;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.NativeMap;
import org.alfresco.repo.jscript.RhinoScriptProcessor;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.tools.shell.Global;
import org.springframework.beans.factory.InitializingBean;

import de.contentreich.scripting.service.JavascriptObjectInitializer;

public class JavascriptHelper implements InitializingBean, JavascriptObjectInitializer {
	private static final Logger logger = Logger.getLogger(JavascriptHelper.class);
	private static JavascriptHelper instance;
	private RhinoScriptProcessor javaScriptProcessor;
	private Repository repositoryHelper;
	// private ScriptService scriptService;
	private ServiceRegistry serviceRegistry;
	private static final WrapFactory wrapFactory = new RhinoWrapFactory();

	public static JavascriptHelper getInstance() {
		return instance;
	}
	
	public RhinoScriptProcessor getJavaScriptProcessor() {
		return javaScriptProcessor;
	}

	public void setJavaScriptProcessor(RhinoScriptProcessor javaScriptProcessor) {
		this.javaScriptProcessor = javaScriptProcessor;
	}
	
	public static Object withTx(final Context context, final Scriptable thisObj,
            final Object[] args, Function funObj) {
    		if (args[0] instanceof Function) {
    			final Function closure = (Function) args[0];
    			RetryingTransactionHelper txnHelper = getInstance().serviceRegistry.getTransactionService().getRetryingTransactionHelper();
    			return txnHelper.doInTransaction(new RetryingTransactionCallback() {
    				@Override
    				public Object execute() throws Throwable {
    					logger.debug("Calling function within tx");
    	    			return closure.call(context, null, thisObj, new Object[] {});
    				}
    			});
    		}
    		return null;
    }

	public static void unsetUser(final Context context, final Scriptable thisObj,
            final Object[] args, Function funObj) {
		logger.debug("Unset user");
		AuthenticationUtil.setRunAsUser(AuthenticationUtil.getGuestUserName());
		getInstance().putUserProperties(AuthenticationUtil.getRunAsUser(), thisObj);
	}

	public static void setUser(final Context context, final Scriptable thisObj,
            final Object[] args, Function funObj) {
		String username = (String) args[0];
		if (getInstance().serviceRegistry.getPersonService().personExists(username)) {
			AuthenticationUtil.setRunAsUser(username);
			getInstance().putUserProperties(AuthenticationUtil.getRunAsUser(), thisObj);
		} else {
			throw new NoSuchPersonException(username);
		}
	}

	public static String whoami(final Context context, final Scriptable thisObj,
            final Object[] args, Function funObj) {
		logger.debug("whoami");
		return AuthenticationUtil.getRunAsUser();
	}

	public static void help(final Context context, final Scriptable thisObj,
            final Object[] args, Function funObj) {
		logger.debug("help");
        PrintStream out = ((Global) funObj.getParentScope()).getOut();
		Global.help(context, thisObj, args, funObj);
        out.println(getMessage("msg.help", null));
	}
	
	private void putUserProperties(String username, final Scriptable thisObj) {
		logger.debug("Put user properties for user " + username);
		NodeRef person = null;
		NodeRef companyHome = null;
		NodeRef userHome = null;
		if (username != null) {
			person = getInstance().repositoryHelper.getPerson();// NoSuchPersonException
		}
		if (person != null) {
			companyHome = getInstance().repositoryHelper.getCompanyHome();
			userHome = getInstance().repositoryHelper.getUserHome(person);
		}
		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("companyhome", companyHome);
        model.put("userhome", userHome);
        model.put("person", person);
        getInstance().putProperties(model, thisObj);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		JavascriptHelper.instance = this;
	}

	@Override
	public void addObjects(Map<String,Object> model, Scriptable scope) {
		
		logger.debug("Add objects");
		Collection<ProcessorExtension> extensions = this.javaScriptProcessor.getProcessorExtensions();
        for (ProcessorExtension ex : extensions) 
        {
        	logger.debug("Adding " + ex.getExtensionName() + " (" + ex.getClass().getName() + ")");
            model.put(ex.getExtensionName(), ex);
        }
        putProperties(model, scope);
		AuthenticationUtil.setRunAsUser(AuthenticationUtil.getGuestUserName());
        putUserProperties(AuthenticationUtil.getRunAsUser(), scope);
	}
	
	
	private void putProperties(Map<String,Object> model, Scriptable scope) {
        // insert supplied object model into root of the default scope
        for (String key : model.keySet())
        {
            // set the root scope on appropriate objects
            // this is used to allow native JS object creation etc.
            Object obj = model.get(key);
        	logger.debug("Processing model " + key + " (" + ((obj != null) ? obj.getClass().getName() : "null")+ ")");
            if (obj instanceof Scopeable)
            {
                ((Scopeable)obj).setScope(scope);
            }
            
            if (obj != null) {
            	// convert/wrap each object to JavaScript compatible
            	Object jsObject = Context.javaToJS(convertToRhino(obj, scope), scope);
            	// insert into the root scope ready for access by the script
            	logger.debug("Putting property " + key + " (" + jsObject.getClass().getName() + ") into scope");
            	ScriptableObject.putProperty(scope, key, jsObject);
            } else {
            	logger.debug("Delete property " + key + " (null) from scope");
            	ScriptableObject.deleteProperty(scope, key);
            }
        }
	}
	
	public void initGlobal(ScriptableObject scope) {
		scope.defineFunctionProperties(new String[] {"setUser", "unsetUser" ,"withTx", "whoami","help"}, JavascriptHelper.class, ScriptableObject.DONTENUM);
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}

	// Shamelessly stolen from rhino
    public static String getMessage(String messageId, Object[] args) {
        Context cx = Context.getCurrentContext();
        Locale locale = cx == null ? Locale.getDefault() : cx.getLocale();

        // ResourceBundle does cacheing.
        ResourceBundle rb = ResourceBundle.getBundle
            ("org.alfresco.extension.scripting.javascript.Messages", locale);

        String formatString;
        try {
            formatString = rb.getString(messageId);
        } catch (java.util.MissingResourceException mre) {
            throw new RuntimeException("no message resource found for message property "
                                       + messageId);
        }

        if (args == null) {
            return formatString;
        } else {
            MessageFormat formatter = new MessageFormat(formatString);
            return formatter.format(args);
        }
    }

    private Object convertToRhino(Object object, Scriptable scope)
    {
        if (object instanceof NodeRef)
        {
            return new ScriptNode((NodeRef) object, this.serviceRegistry, scope);
        }
        return object;
    }

    /**
     * Rhino script value wraper
     */
    private static class RhinoWrapFactory extends WrapFactory
    {
    	/* (non-Javadoc)
    	 * @see org.mozilla.javascript.WrapFactory#wrapAsJavaObject(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object, java.lang.Class)
    	 */
        public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class staticType)
        {
            if (javaObject instanceof Map && !(javaObject instanceof ScriptableHashMap))
            {
                return new NativeMap(scope, (Map)javaObject);
            }
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }
    }

	@Override
	public void initContext(Context context) {
		context.setWrapFactory(wrapFactory);
		
	}
    

}
