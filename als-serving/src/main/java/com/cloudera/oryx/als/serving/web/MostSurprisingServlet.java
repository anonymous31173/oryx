/*
 * Copyright (c) 2014, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.oryx.als.serving.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.cloudera.oryx.als.common.NoSuchUserException;
import com.cloudera.oryx.als.common.NotReadyException;
import com.cloudera.oryx.als.common.OryxRecommender;

/**
 * <p>Responds to a GET request to
 * {@code /mostSurprising/[userID](?howMany=n)}
 * and in turn calls {@link com.cloudera.oryx.als.common.OryxRecommender#mostSurprising(String, int)}.
 * {@code howMany} is the desired number of results to return. If {@code howMany} is not
 * specified, defaults to {@link com.cloudera.oryx.als.serving.web.AbstractALSServlet#DEFAULT_HOW_MANY}.</p>
 *
 * <p>CSV output contains one item per line, and each line is of the form {@code itemID, strength},
 * like {@code 325, 0.53}. Strength is an opaque indicator of the relative surprise of the item.
 * Higher means more surprising</p>
 *
 * @author Sean Owen
 */
public final class MostSurprisingServlet extends AbstractALSServlet {
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    CharSequence pathInfo = request.getPathInfo();
    if (pathInfo == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No path");
      return;
    }
    Iterator<String> pathComponents = SLASH.split(pathInfo).iterator();
    String userID;
    try {
      userID = pathComponents.next();
    } catch (NoSuchElementException nsee) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, nsee.toString());
      return;
    }
    if (pathComponents.hasNext()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path too long");
      return;
    }

    userID = unescapeSlashHack(userID);

    OryxRecommender recommender = getRecommender();
    try {
      outputALSResult(request,
                      response,
                      recommender.mostSurprising(userID, getNumResultsToFetch(request)));
    } catch (NoSuchUserException nsue) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, nsue.toString());
    } catch (NotReadyException nre) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, nre.toString());
    } catch (IllegalArgumentException | UnsupportedOperationException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.toString());
    }
  }

}
