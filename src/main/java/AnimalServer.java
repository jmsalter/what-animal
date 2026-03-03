import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimalServer {
    public static void main(String[] args) throws Exception {
        setupDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new PageHandler());
        server.createContext("/api/animals", new AnimalHandler());
        server.createContext("/api/animals/last", new LastAnimalHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }

    static void setupDatabase() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:./data/animals");
        Statement statement = connection.createStatement();
        statement.execute("create table if not exists pictures (id integer auto_increment primary key, animal varchar(20), url varchar(500))");
        statement.close();
        connection.close();
    }
}

class PageHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        InputStream input = getClass().getResourceAsStream("/index.html");
        byte[] bytes = input.readAllBytes();
        input.close();

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
    }
}

class AnimalHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> values = readQuery(query);
        String animal = values.get("animal");
        int count = readCount(values.get("count"));
        String save = values.get("save");
        List<String> pictures = getPictures(animal, count);
        int saved = 0;

        if ("yes".equals(save)) {
            saved = savePictures(animal, pictures);
        }

        String json = makeJson(animal, count, save, saved, pictures);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
        OutputStream output = exchange.getResponseBody();
        output.write(json.getBytes(StandardCharsets.UTF_8));
        output.close();
    }

    Map<String, String> readQuery(String query) {
        Map<String, String> map = new HashMap<>();

        if (query == null) {
            return map;
        }

        String[] parts = query.split("&");
        int i = 0;

        while (i < parts.length) {
            String[] one = parts[i].split("=");

            if (one.length == 2) {
                map.put(decode(one[0]), decode(one[1]));
            }

            i = i + 1;
        }

        return map;
    }

    String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    int readCount(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 1;
        }
    }

    List<String> getPictures(String animal, int count) {
        List<String> pictures = new ArrayList<>();
        int i = 0;

        while (i < count) {
            pictures.add(getPictureUrl(animal, i));
            i = i + 1;
        }

        return pictures;
    }

    String getPictureUrl(String animal, int i) {
        int width = 300 + i;
        int height = 200 + i;

        if ("cat".equals(animal)) {
            return "https://placekittens.com/" + width + "/" + height;
        }

        if ("dog".equals(animal)) {
            return "https://placedog.net/" + width + "/" + height;
        }

        if ("bear".equals(animal)) {
            return "https://placebear.com/" + width + "/" + height;
        }

        return "";
    }

    int savePictures(String animal, List<String> pictures) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:./data/animals");
            String sql = "insert into pictures (animal, url) values (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            int i = 0;

            while (i < pictures.size()) {
                statement.setString(1, animal);
                statement.setString(2, pictures.get(i));
                statement.executeUpdate();
                i = i + 1;
            }

            statement.close();
            connection.close();
            return pictures.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    String makeJson(String animal, int count, String save, int saved, List<String> pictures) {
        String json = "{";
        json = json + "\"animal\":\"" + animal + "\",";
        json = json + "\"count\":" + count + ",";
        json = json + "\"save\":\"" + save + "\",";
        json = json + "\"saved\":" + saved + ",";
        json = json + "\"pictures\":[";

        int i = 0;
        while (i < pictures.size()) {
            json = json + "\"" + pictures.get(i) + "\"";

            if (i < pictures.size() - 1) {
                json = json + ",";
            }

            i = i + 1;
        }

        json = json + "]}";
        return json;
    }
}

class LastAnimalHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        AnimalHandler animalHandler = new AnimalHandler();
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> values = animalHandler.readQuery(query);
        String animal = values.get("animal");
        String picture = getLastPicture(animal);
        String json = makeJson(animal, picture);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
        OutputStream output = exchange.getResponseBody();
        output.write(json.getBytes(StandardCharsets.UTF_8));
        output.close();
    }

    String getLastPicture(String animal) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:h2:./data/animals");
            String sql = "select url from pictures where animal = ? order by id desc limit 1";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, animal);
            ResultSet resultSet = statement.executeQuery();
            String picture = "";

            if (resultSet.next()) {
                picture = resultSet.getString("url");
            }

            resultSet.close();
            statement.close();
            connection.close();
            return picture;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    String makeJson(String animal, String picture) {
        String json = "{";
        json = json + "\"animal\":\"" + animal + "\",";
        json = json + "\"picture\":\"" + picture + "\"";
        json = json + "}";
        return json;
    }
}
