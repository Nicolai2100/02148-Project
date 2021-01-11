package BeastProject.yahooAPI;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.util.Date;

public class APIcalls {
    private static final String apikey = "9ef62d9bdcmshfef91e2b707c69cp1724fejsncd7e1e4f3845";
    private static final String apihost = "apidojo-yahoo-finance-v1.p.rapidapi.com";
    // The stockRepository has tuples containing of handle/region in the identifier and then the sequentialspace in the 2nd field.
    private static SpaceRepository stockRepository;

    public APIcalls () { stockRepository = new SpaceRepository(); }

    public static void main(String[] args) {
        APIcalls apicalls = new APIcalls();
        apicalls.findStock("AMRN","US");
        // apicalls.findStock("TSLA","US");
        SequentialSpace sequentialSpace = (SequentialSpace) getStockRepository().get("AMRN/US");
        StockModel stock = apicalls.findLatestStockInfo(sequentialSpace);
    }

    /**
     * This endpoint returns historical data for a stock, and that is the opening value of 255 dates, and closing,
     * that days high, low and things such as that.
     * The api will return a datatype of either double or integer depending on the amount of zeros.
     * We therefore need to check if the type is integer, and if so cast it to a double...
     * @param region the region in which the company is, for example TSLA is in the US
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
                // There is a need to find out if the line contains the correct fields. In case of a split happening for a stock
                // A new entry will be added to the HttpResponse with different fields.
                if (jsonObject1.has("volume")) {
                // It has to be converted to milli seconds.
                // If you multiply it with a 1000 earlier it overflowcalendars, it has to be done in the setTime function.
                long time = ((int) jsonObject1.get("date"));
                Date date1 = new Date();
                date1.setTime(1000 * time);
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

    /**
     * Finds the latest info in a stock. It takes the sequential space as the input.'
     * Returns rubbish if it cant be found.
     * @param sequentialSpace
     * @return
     */
    public StockModel findLatestStockInfo(SequentialSpace sequentialSpace) {
        try {
            Object[] stockModel = sequentialSpace.query(new FormalField(StockModel.class));
            return (StockModel) stockModel[0];
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date date = new Date();
        return new StockModel(date, 0, 0, 0, 0, 0, 0);
    }
}
