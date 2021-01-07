package BeastProject.yahooAPI;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.util.Date;

public class APIcalls {
    private static final String apikey = "9ef62d9bdcmshfef91e2b707c69cp1724fejsncd7e1e4f3845";
    private static final String apihost = "apidojo-yahoo-finance-v1.p.rapidapi.com";
    private static SpaceRepository stockRepository;

    public APIcalls () { stockRepository = new SpaceRepository(); }

    public static void main(String[] args) {
        // And then we ofc need to add it with the same handle. The handle and region needs to come from a user.
        // The name is made op of the handle/region.
        APIcalls apicalls = new APIcalls();
        apicalls.findStock("AMRN","US");
        apicalls.findStock("TSLA","US");
        System.out.println("hej");
    }

    /**
     * This endpoint returns historical data for a stock, and that is the opening value of 255 dates, and closing,
     * that days high, low and things such as that.
     * The api will return a datatype of either double or integer depending on the amount of zeros.
     * We therefore need to check if the type is integer, and if so cast it to a double...
     * @param region the region in which the company is, f.eks TSLA is in the US
     * @param handle The handle for the company, it is the abbreviation which for tesla is TSLA for example.
     */
    public SequentialSpace gethistoricaldata(String handle, String region) {
        SequentialSpace sequentialSpace = new SequentialSpace();
        try {
            String requestaddress = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data?symbol=" + handle + "&region=" + region;
            HttpResponse<String> response = Unirest.get(requestaddress)
                    .header("x-rapidapi-key", apikey).header("x-rapidapi-host", apihost).asString();


            JSONObject jsonObject = new JSONObject(response.getBody());
            JSONArray myResponse = jsonObject.getJSONArray("prices");

            for (int i = 0; i < myResponse.length(); i++) {
                JSONObject jsonObject1 = myResponse.getJSONObject(i);
                Date date1 = new Date();
                // There is a need to find out if the line contains the correct fields. In case of a split happening for a stock
                // A new entry will be added to the HttpResponse with different fields.
                if (jsonObject1.has("volume")) {
                date1.setTime((int) jsonObject1.get("date"));
                int volume = (int) jsonObject1.get("volume");
                double high, low, adjclose, close, open;
                // There is a need to check which type it is. If the stock prize doesnt have any decimals the api returns
                // The datatype integer instead of double, and therefore we get this clustered code.
                if (myResponse.getJSONObject(i).get("high").getClass() == Integer.class) {
                    high = (int) myResponse.getJSONObject(i).get("high");
                } else {
                    high = (double) myResponse.getJSONObject(i).get("high");
                }

                if (myResponse.getJSONObject(i).get("low").getClass() == Integer.class) {
                    low = (int) myResponse.getJSONObject(i).get("low");
                } else {
                    low = (double) myResponse.getJSONObject(i).get("low");
                }

                if (myResponse.getJSONObject(i).get("adjclose").getClass() == Integer.class) {
                    adjclose = (int) myResponse.getJSONObject(i).get("adjclose");
                } else {
                    adjclose = (double) myResponse.getJSONObject(i).get("adjclose");
                }

                if (myResponse.getJSONObject(i).get("close").getClass() == Integer.class) {
                    close = (int) myResponse.getJSONObject(i).get("close");
                } else {
                    close = (double) myResponse.getJSONObject(i).get("close");
                }

                if (myResponse.getJSONObject(i).get("open").getClass() == Integer.class) {
                    open = (int) myResponse.getJSONObject(i).get("open");
                } else {
                    open = (double) myResponse.getJSONObject(i).get("open");
                }
                StockModel stockModel = new StockModel(date1, volume, high, low, adjclose, close, open);
                try {
                    sequentialSpace.put(stockModel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            }
            getStockRepository().add(handle + "/" + region, sequentialSpace);
            return sequentialSpace;
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return sequentialSpace;
    }

    /**
     * This returns a stock. It either finds the stock in the already gathered stocks or queries the API.
     * @param handle
     * @param region
     * @return
     */
    public SequentialSpace findStock(String handle, String region) {
        SequentialSpace sequentialSpace = (SequentialSpace) stockRepository.get(handle + "/" + region);
        if (sequentialSpace == null) {
            return gethistoricaldata(handle, region);
        } else {
            return sequentialSpace;
        }
    }

    public static SpaceRepository getStockRepository() {
        return stockRepository;
    }
}
