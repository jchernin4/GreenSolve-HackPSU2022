package org.hackpsu;

import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import io.javalin.Javalin;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class HttpServer {
    private static HttpServer instance;
    public MongoManager mongoManager;
    private final Javalin app;

    public static HttpServer getInstance() {
        if (instance == null) {
            instance = new HttpServer();
        }

        return instance;
    }

    private HttpServer() {
        System.out.println("Initializing MongoDB....");
        mongoManager = MongoManager.getInstance();
        System.out.println("Finished initializing MongoDB.");


        System.out.println("Initializing Javalin...");
        app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    it.anyHost();
                });
            });
        }).start(Constants.HTTP_SERVER_PORT);
        //app = Javalin.create().start(Constants.HTTP_SERVER_PORT);
        System.out.println("Finished initializing Javalin.");
    }

    public void start() {
        app.post("/register", ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            Document doc = Document.parse(ctx.body());

            System.out.println(doc.toJson());

            MongoManager.getInstance().registerUser(doc.getString("phone"), doc.getString("zip"));
        });

        app.post("/events", ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Headers", "*");

            Document doc = Document.parse(ctx.body());

            System.out.println(doc.toJson());

            SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy");
            Date date = parser.parse(doc.getString("date"));

            String dateToString = parser.format(date);

            MongoManager.getInstance().createEvent(doc.getString("name"), doc.getString("phone"), doc.getString("address"), doc.getString("zip"), dateToString, doc.getString("time"), doc.getString("details"));
            Main.sendBlastToZip(doc.getString("zip"), Main.generateEventInfo(doc));
        });

        app.post("/sms", ctx -> {
            ctx.contentType("application/json");
            String body = ctx.body();
            int bodyIndex = body.indexOf("&Body=") + 6;
            body = body.substring(bodyIndex, body.indexOf("&FromCountry"));
            System.out.println("Text: " + body);
        });
    }
}
