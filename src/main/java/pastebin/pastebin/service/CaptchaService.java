package pastebin.pastebin.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class CaptchaService implements ICaptchaService {

    @Value("${google.recaptcha.key.secret}")
    private String secretKey;

    @Override
    public boolean isCaptchaValid(String recaptchaResponse) {
        String params = "secret=" + secretKey + "&response=" + recaptchaResponse;

        try {
            String url = "https://www.google.com/recaptcha/api/siteverify";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
            outputStreamWriter.write(params);
            outputStreamWriter.flush();
            outputStreamWriter.close();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            bufferedReader.close();

            // Parse the JSON response
            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            boolean success = jsonObject.get("success").getAsBoolean();

            // Add intermediate results to view in the console or logs
            System.out.println("reCaptcha response: " + response.toString());
            System.out.println("reCaptcha success: " + success);

            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
