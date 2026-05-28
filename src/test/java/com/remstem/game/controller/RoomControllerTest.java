package com.remstem.game.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remstem.game.model.*;
import com.remstem.game.scheduler.SpinScheduler;
import com.remstem.game.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RoomService roomService;
    @MockBean SpinScheduler spinScheduler;
    @MockBean SimpMessagingTemplate messaging;

    // --- POST /api/rooms ---

    @Test
    void createRoom_returns200_withCodeAndToken() throws Exception {
        Room room = new Room("ABC123", "tok-xyz");
        when(roomService.create()).thenReturn(room);

        mockMvc.perform(post("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("ABC123"))
                .andExpect(jsonPath("$.hostToken").value("tok-xyz"));
    }

    // --- GET /api/rooms/{code} ---

    @Test
    void getRoom_returns200_withRoomView() throws Exception {
        Room room = buildRoom("ABC123");
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));

        mockMvc.perform(get("/api/rooms/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC123"))
                .andExpect(jsonPath("$.state").value("LOBBY"));
    }

    @Test
    void getRoom_returns404_whenNotFound() throws Exception {
        when(roomService.find("XXXXXX")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/XXXXXX"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/rooms/{code}/players ---

    @Test
    void joinRoom_returns200_withPlayerIdAndName() throws Exception {
        Room room = buildRoom("ABC123");
        Player player = new Player("pid-1", "Alice");
        when(roomService.join("ABC123", "Alice")).thenReturn(player);
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));

        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("pid-1"))
                .andExpect(jsonPath("$.playerName").value("Alice"));
    }

    @Test
    void joinRoom_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinRoom_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinRoom_returns404_whenRoomNotFound() throws Exception {
        when(roomService.join("XXXXXX", "Alice")).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/api/rooms/XXXXXX/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinRoom_broadcastsPlayerJoined() throws Exception {
        Room room = buildRoom("ABC123");
        Player player = new Player("pid-1", "Alice");
        when(roomService.join("ABC123", "Alice")).thenReturn(player);
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));

        mockMvc.perform(post("/api/rooms/ABC123/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isOk());

        verify(messaging).convertAndSend(eq("/topic/rooms/ABC123"), any(Object.class));
    }

    // --- PUT /api/rooms/{code}/config ---

    @Test
    void updateConfig_returns200() throws Exception {
        GameConfig config = new GameConfig();
        config.setSpinIntervalMinutes(30);
        config.setIntensity(Intensity.SAVAGE);

        mockMvc.perform(put("/api/rooms/ABC123/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk());

        verify(roomService).updateConfig(eq("ABC123"), any(GameConfig.class));
    }

    // --- POST /api/rooms/{code}/start ---

    @Test
    void startRoom_returns200_andStartsScheduler() throws Exception {
        Room room = buildRoom("ABC123");
        room.setState(GameState.ACTIVE);
        when(roomService.start("ABC123")).thenReturn(room);
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));

        mockMvc.perform(post("/api/rooms/ABC123/start"))
                .andExpect(status().isOk());

        verify(spinScheduler).start("ABC123");
        verify(messaging).convertAndSend(eq("/topic/rooms/ABC123"), any(Object.class));
    }

    @Test
    void startRoom_returns404_whenRoomNotFound() throws Exception {
        when(roomService.start("XXXXXX")).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/api/rooms/XXXXXX/start"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/rooms/{code}/end ---

    @Test
    void endRoom_returns200_withStats() throws Exception {
        Room room = buildRoom("ABC123");
        room.setState(GameState.ACTIVE);
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));
        when(roomService.end("ABC123")).thenReturn(Map.of("players", List.of(), "totalSpins", 3));

        mockMvc.perform(post("/api/rooms/ABC123/end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpins").value(3));

        verify(spinScheduler).stop("ABC123");
        verify(messaging).convertAndSend(eq("/topic/rooms/ABC123"), any(Object.class));
    }

    @Test
    void endRoom_returns404_whenRoomNotFound() throws Exception {
        when(roomService.find("XXXXXX")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rooms/XXXXXX/end"))
                .andExpect(status().isNotFound());
    }

    @Test
    void endRoom_alreadyEnded_returnsStatsWithoutRebroadcast() throws Exception {
        Room room = buildRoom("ABC123");
        room.setState(GameState.ENDED);
        when(roomService.find("ABC123")).thenReturn(Optional.of(room));
        when(roomService.buildStats(room)).thenReturn(Map.of("players", List.of(), "totalSpins", 5));

        mockMvc.perform(post("/api/rooms/ABC123/end"))
                .andExpect(status().isOk());

        verify(spinScheduler, never()).stop(any());
        verify(messaging, never()).convertAndSend(anyString(), any(Object.class));
    }

    // --- DELETE /api/rooms/{code}/players/{id} ---

    @Test
    void removePlayer_returns200() throws Exception {
        mockMvc.perform(delete("/api/rooms/ABC123/players/p1"))
                .andExpect(status().isOk());

        verify(roomService).removePlayer("ABC123", "p1");
    }

    // --- helpers ---

    private Room buildRoom(String code) {
        return new Room(code, "host-token");
    }
}
