/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.NamespacedAlias;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetNamespacedAliases extends APIServlet.APIRequestHandler {

    static final GetNamespacedAliases instance = new GetNamespacedAliases();

    private GetNamespacedAliases() {
        super(new APITag[] {APITag.ALIASES}, "account", "filter", "firstIndex", "lastIndex", "sortBy", "sortAsc");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        final long accountId = ParameterParser.getAccount(req).getId();
        final String filter = ParameterParser.getFilter(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = Math.max(ParameterParser.getLastIndex(req), 0);

        /* Default sort is by name, other option is by height. Use @sortBy=height */
        boolean sortByHeight = "height".equals(req.getParameter("sortBy"));
        int sortBy = sortByHeight ? NamespacedAlias.SORT_BY_HEIGHT : NamespacedAlias.SORT_BY_NAME;
        boolean sortAsc = "true".equals(req.getParameter("sortAsc"));

        JSONArray aliases = new JSONArray();
        try (
            DbIterator<NamespacedAlias> iterator = (filter == null) ?
                  NamespacedAlias.getAliasesByOwner(accountId, firstIndex, lastIndex, sortBy, sortAsc) :
                  NamespacedAlias.searchAliases(filter, accountId, firstIndex, lastIndex, sortBy, sortAsc) ) {
            while (iterator.hasNext()) {
                aliases.add(JSONData.namespacedAlias(iterator.next()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;
    }

}