package Test.android.Controller;

import Test.android.TestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;



@Slf4j
@RestController
public class MsgSocketController extends TextWebSocketHandler {

    HashMap<String, WebSocketSession> sessionMap = new HashMap<>(); //웹소켓 세션을 담아둘 맵

    JSONParser jsonParser = new JSONParser();
    ObjectMapper mapper = new ObjectMapper();
    String target = null;
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException, ParseException {

        TestData td = new TestData();
        org.json.simple.JSONObject jsonObject = JsonToObjectParser(message.getPayload());

        log.info("first connect" +jsonObject.toString());

        switch (jsonObject.get("type").toString()){
            case "store_user":
                String name = jsonObject.get("name").toString();
                if (sessionMap.containsKey(name)){
                    td.setType("user already exists");
                    String json = mapper.writeValueAsString(td);
                    session.sendMessage(new TextMessage(json));
                }
                else{
                    sessionMap.put(name, session);
                }
                break;
            case "start_call":
                target = jsonObject.get("target").toString();
                td.setType("call_response");

                if (sessionMap.containsKey(target)){
                    td.setData("user is ready for call");
                    String json = mapper.writeValueAsString(td);
                    session.sendMessage(new TextMessage(json));

                }
                else {
                    td.setData("user is not online");
                    String json = mapper.writeValueAsString(td);
                    sessionMap.get(target).sendMessage(new TextMessage(json));

                }
                break;
            case "create_offer":

                target = jsonObject.get("target").toString();
                td.setType("offer_received");

                if (sessionMap.containsKey(target)){
                    td.setName(jsonObject.get("name").toString());
                    JSONObject jobj1 = (JSONObject) jsonParser.parse(jsonObject.get("data").toString());

                    td.setData(jobj1.get("sdp").toString());

                    String json = mapper.writeValueAsString(td);

                    sessionMap.get(target).sendMessage(new TextMessage(json));
                }

                break;
            case "create_answer":

                target = jsonObject.get("target").toString();
                td.setType("answer_received");



                if (sessionMap.containsKey(target)){

                    td.setName(jsonObject.get("name").toString());

                    JSONObject jobj1 = (JSONObject) jsonParser.parse(jsonObject.get("data").toString());

                    td.setData(jobj1.get("sdp").toString());

                    String json = mapper.writeValueAsString(td);
                    sessionMap.get(target).sendMessage(new TextMessage(json));

                }

                break;
            case "ice_candidate":
                target = jsonObject.get("target").toString();
                td.setType("ice_candidate");

                if (sessionMap.containsKey(target)){
                    td.setName(jsonObject.get("name").toString());

                    JSONObject jobj1 = (JSONObject) jsonParser.parse(jsonObject.get("data").toString());

                    td.setData(jobj1);


                    String json = mapper.writeValueAsString(td);


                    sessionMap.get(target).sendMessage(new TextMessage(json));

                }

                break;
        }



    }



    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);



    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionMap.remove(session.getId());
        super.afterConnectionClosed(session, status);
    }

    private static org.json.simple.JSONObject JsonToObjectParser(String jsonStr) {
        JSONParser parser = new JSONParser();

        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(jsonStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
