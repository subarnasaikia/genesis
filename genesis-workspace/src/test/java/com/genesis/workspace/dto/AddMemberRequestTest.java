package com.genesis.workspace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genesis.workspace.entity.MemberRole;
import org.junit.jupiter.api.Test;

class AddMemberRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_ShouldWork_WhenJsonIsValid() throws Exception {
        String json = "{\"email\": \"test@example.com\", \"role\": \"ANNOTATOR\"}";
        AddMemberRequest request = objectMapper.readValue(json, AddMemberRequest.class);

        assertThat(request.getEmail()).isEqualTo("test@example.com");
        assertThat(request.getRole()).isEqualTo(MemberRole.ANNOTATOR);
    }

    @Test
    void deserialize_ShouldFail_WhenRoleIsInvalid() {
        String json = "{\"email\": \"test@example.com\", \"role\": \"INVALID\"}";
        try {
            objectMapper.readValue(json, AddMemberRequest.class);
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("MemberRole");
        }
    }
}
