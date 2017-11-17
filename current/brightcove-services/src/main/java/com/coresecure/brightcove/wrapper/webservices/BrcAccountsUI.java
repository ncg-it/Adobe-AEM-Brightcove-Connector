/*

    Adobe AEM Brightcove Connector

    Copyright (C) 2017 Coresecure Inc.

    Authors:    Alessandro Bonfatti
                Yan Kisen
                Pablo Kropilnicki

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    - Additional permission under GNU GPL version 3 section 7
    If you modify this Program, or any covered work, by linking or combining
    it with httpclient 4.1.3, httpcore 4.1.4, httpmine 4.1.3, jsoup 1.7.2,
    squeakysand-commons and squeakysand-osgi (or a modified version of those
    libraries), containing parts covered by the terms of APACHE LICENSE 2.0
    or MIT License, the licensors of this Program grant you additional
    permission to convey the resulting work.

 */

package com.coresecure.brightcove.wrapper.webservices;

//import com.adobe.granite.ui.components.ds.DataSource;
//import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.coresecure.brightcove.wrapper.sling.ConfigurationGrabber;
import com.coresecure.brightcove.wrapper.sling.ConfigurationService;
import com.coresecure.brightcove.wrapper.sling.ServiceUtil;
import com.coresecure.brightcove.wrapper.utils.TextUtil;
import org.apache.commons.collections.Transformer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import org.apache.commons.collections.iterators.TransformIterator;
//import com.adobe.granite.ui.components.ds.ValueMapResource;

@Service
@Component
@Properties(value = {
        @Property(name = "sling.servlet.resourceTypes", value= {"coresecure/brightcove/accountsUI","coresecure/brightcove/playersUI"})
})
public class BrcAccountsUI extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrcAccountsUI.class);

    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws ServletException,
            IOException {
        LOGGER.trace("*POST-requestspath*" + request.getRequestPathInfo().toString());
        routeUIrequest(request, response);
    }


    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException,
            IOException
    {
        LOGGER.trace("*GET-requestspath*" + request.getRequestPathInfo().toString());
        //IF HEADER = something
        routeUIrequest(request, response);
    }


    public void api(final SlingHttpServletRequest request,
                    final SlingHttpServletResponse response) throws ServletException,
            IOException {


        boolean is_authorized = false;

        List<JSONObject> accountsList = new ArrayList<JSONObject>();

        LOGGER.debug("get account");
        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            UserManager userManager = request.getResourceResolver().adaptTo(UserManager.class);
                /* to get the current user */
            Authorizable auth = userManager.getAuthorizable(session.getUserID());
            if (auth != null) {
                List<String> memberOf = new ArrayList<String>();
                Iterator<Group> groups = auth.memberOf();
                while (groups.hasNext() && !is_authorized) {
                    Group group = groups.next();
                    memberOf.add(group.getID());
                }
                ConfigurationGrabber cg = ServiceUtil.getConfigurationGrabber();

                int i = 0;
                for (String account : cg.getAvailableServices()) {
                    LOGGER.debug("get account: " + account);
                    ConfigurationService cs = cg.getConfigurationService(account);
                    List<String> allowedGroups = new ArrayList<String>();
                    allowedGroups.addAll(cs.getAllowedGroupsList());
                    allowedGroups.retainAll(memberOf);

                    String optionText = account;
                    String alias = cs.getAccountAlias();
                    if (TextUtil.notEmpty(alias)) {
                        optionText = String.format("%s [%s]", alias, account);
                    }
                    if (allowedGroups.size() > 0) {
                        JSONObject accountJson = new JSONObject();
                        accountJson.put("text", optionText);
                        accountJson.put("value", account);
                        accountJson.put("id", i);
                        i++;
                        accountsList.add(accountJson);
                    }
                }

            } else {
                LOGGER.debug("not authorized");
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException", e);
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException", e);
        }


/*        DataSource ds = new SimpleDataSource(new TransformIterator(accountsList.iterator(), new Transformer() {
            public Object transform(Object input) {
                try {
                    JSONObject item = (JSONObject) input;

                    ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
                    vm.put("value", item.getString("value"));
                    vm.put("text", item.getString("text"));
                    vm.put("id", item.getString("id"));

                    return new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured", vm);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }));

        request.setAttribute(DataSource.class.getName(), ds);*/

    }





    public void  routeUIrequest( final SlingHttpServletRequest request, final SlingHttpServletResponse response)throws ServletException,
            IOException
    {
        if(request.getResource()!=null && "coresecure/brightcove/accountsUI".equals(request.getResource().getResourceType()))
        {
            LOGGER.trace("*accountsUI*" + request.getResource().getResourceType());
            api(request, response);
        }
        else if(request.getResource()!=null && "coresecure/brightcove/playersUI".equals(request.getResource().getResourceType()))
        {
            LOGGER.trace("*playersUI*" + request.getResource().getResourceType());
            api(request, response);
        }
        else
        {
            LOGGER.trace("*otherRestype*" + request.getResource().getResourceType());

        }

    }

}
