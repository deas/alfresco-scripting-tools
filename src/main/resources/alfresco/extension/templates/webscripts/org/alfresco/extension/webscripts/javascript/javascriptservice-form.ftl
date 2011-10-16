<#import "/org/springframework/extensions/webscripts/webscripts.lib.html.ftl" as wsLib/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
   <@wsLib.head>Alfresco Javascript Shell</@wsLib.head>
   <body>
     <div>
      <form action="${url.serviceContext}${url.match}" method="post">
         <div>
            <table>
               <tr>
                  <td><img src="${resourceurl('/images/logo/AlfrescoLogo32.png', true)}" alt="Alfresco" /></td>
                  <td><span class="title">Alfresco Javascript Service</span></td>
               </tr>
               <tr>
               	<td colspan="2">Alfresco ${server.edition?html} v${server.version?html}</td>
               </tr>
				<tr>
               	<td>
               		Enable Shell (port ${javascriptShellService.port})
               	</td>
               	<td>
               		<input type="checkbox" value="true" name="shellEnabled" <#if javascriptShellService.isRunning()>checked="yes"</#if>/>
               	</td>	
               	</tr>
               <tr>
               	<td colspan="2" align="right"><input type="submit" name="submit"/></td>
               </tr>
            </table>
         </div>
      </form>
      </div>
   </body>
</html>