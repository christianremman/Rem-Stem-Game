package com.remstem.game.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsEvent {
    private String type;
    private Object payload;

    public static WsEvent of(String type, Object payload) {
        return new WsEvent(type, payload);
    }

    public static WsEvent of(String type) {
        return new WsEvent(type, Map.of());
    }
}
