package BeastProject.yahooAPI;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

public class APIcalls {
    private static final String apikey = "9ef62d9bdcmshfef91e2b707c69cp1724fejsncd7e1e4f3845";
    private static final String apihost = "apidojo-yahoo-finance-v1.p.rapidapi.com";

    public static void main(String[] args) {
        try {
            HttpResponse<String> response = Unirest.get("https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data?symbol=AMRN&region=US")
                    .header("x-rapidapi-key", "9ef62d9bdcmshfef91e2b707c69cp1724fejsncd7e1e4f3845")
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .asString();

            response.getBody();

            JSONObject jsonObject = new JSONObject(response.getBody());
            JSONArray myResponse = jsonObject.getJSONArray("prices");
            ArrayList<StockModel> stockModels = new ArrayList<>();

            for (int i = 0; i < myResponse.length(); i++) {
                JSONObject jsonObject1 = myResponse.getJSONObject(i);
                Date date1 = new Date();
                date1.setTime((int) jsonObject1.get("date"));
                StockModel stockModel = new StockModel(date1, (int) jsonObject1.get("volume"), (double) jsonObject1.get("high"), (double) jsonObject1.get("low"), (double) jsonObject1.get("adjclose"), (double) jsonObject1.get("close"), (double) jsonObject1.get("open"));
                stockModels.add(stockModel);
            }
            System.out.println(stockModels);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param region the region in which the company is, f.eks TSLA is in the US
     * @param handle The handle for the company, it is the abbreviation which for tesla is TSLA for example.
     */
    public static ArrayList<StockModel> gethistoricaldata(String region, String handle) {
        try {
            HttpResponse<String> response = Unirest.get
                    ("https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data?symbol=" + handle + "&region=" + region)
                    .header("x-rapidapi-key", "9ef62d9bdcmshfef91e2b707c69cp1724fejsncd7e1e4f3845")
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .asString();

            response.getBody();

            JSONObject jsonObject = new JSONObject(response.getBody());
            JSONArray myResponse = jsonObject.getJSONArray("prices");
            ArrayList<StockModel> stockModels = new ArrayList<>();

            for (int i = 0; i < myResponse.length(); i++) {
                JSONObject jsonObject1 = myResponse.getJSONObject(i);
                Date date1 = new Date();
                date1.setTime((int) jsonObject1.get("date"));
                StockModel stockModel = new StockModel(date1, (int) jsonObject1.get("volume"), (double) jsonObject1.get("high"), (double) jsonObject1.get("low"), (double) jsonObject1.get("adjclose"), (double) jsonObject1.get("close"), (double) jsonObject1.get("open"));
                stockModels.add(stockModel);
            }
            System.out.println(stockModels);
            return stockModels;
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
