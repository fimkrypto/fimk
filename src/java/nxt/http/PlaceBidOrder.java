package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.MofoAsset;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class PlaceBidOrder extends CreateTransaction {

    static final PlaceBidOrder instance = new PlaceBidOrder();

    private PlaceBidOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceNQT", "orderFeeNQT");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        long feeNQT = ParameterParser.getFeeNQT(req);
        long orderFeeNQT = Asset.privateEnabled() ? ParameterParser.getOrderFeeNQT(req) : 0;
        Account account = ParameterParser.getSenderAccount(req);

        long totalNQT = Convert.safeMultiply(priceNQT, quantityQNT);
        
        try {
            if (Convert.safeAdd(feeNQT, totalNQT) > account.getUnconfirmedBalanceNQT()) {
                return NOT_ENOUGH_FUNDS;
            }
        } catch (ArithmeticException e) {
            return NOT_ENOUGH_FUNDS;
        }
        
        if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(asset)) {
                
            long minOrderFeeNQT = MofoAsset.calculateOrderFee(asset.getId(), totalNQT);
            if (minOrderFeeNQT > orderFeeNQT) {
                JSONObject response = new JSONObject();
                response.put("error", "Insufficient \"orderFeeNQT\": minimum of " + Long.valueOf(minOrderFeeNQT) + " required");
                return JSON.prepare(response);
            }
            
            try {
                if (Convert.safeAdd( Convert.safeAdd(feeNQT, totalNQT), orderFeeNQT) > account.getUnconfirmedBalanceNQT()) {
                    return NOT_ENOUGH_FUNDS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_FUNDS;
            }
        }

        Attachment attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset.getId(), quantityQNT, priceNQT, orderFeeNQT);
        return createTransaction(req, account, attachment);
    }

}
