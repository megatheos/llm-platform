package com.llmplatform.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 序列化属性测试
 * Property 37: 序列化往返一致性
 * Property 38: JSON输出格式完整性
 * 验证需求：9.5, 9.6, 9.4
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jackson.serialization-inclusion=non_null",
        "spring.jackson.default-property-inclusion=non_null"
})
@DisplayName("序列化属性测试")
class SerializationPropertyTest {

    private ObjectMapper objectMapper;

    // Test data classes
    record SimpleData(Long id, String name, Integer value) {}
    record ComplexData(Long id, String name, LocalDateTime createdAt, List<String> tags, Map<String, Object> metadata) {}
    record NestedData(Long id, String name, SimpleData child) {}

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("简单对象序列化往返一致")
    void simpleObjectSerializationRoundTrip() throws JsonProcessingException {
        SimpleData original = new SimpleData(1L, "test", 42);

        String json = objectMapper.writeValueAsString(original);
        SimpleData deserialized = objectMapper.readValue(json, SimpleData.class);

        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("复杂对象序列化往返一致")
    void complexObjectSerializationRoundTrip() throws JsonProcessingException {
        ComplexData original = new ComplexData(
                1L, "test", LocalDateTime.now(),
                List.of("tag1", "tag2"),
                Map.of("key", "value")
        );

        String json = objectMapper.writeValueAsString(original);
        ComplexData deserialized = objectMapper.readValue(json, ComplexData.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.tags(), deserialized.tags());
    }

    @Test
    @DisplayName("嵌套对象序列化往返一致")
    void nestedObjectSerializationRoundTrip() throws JsonProcessingException {
        SimpleData child = new SimpleData(2L, "child", 10);
        NestedData original = new NestedData(1L, "parent", child);

        String json = objectMapper.writeValueAsString(original);
        NestedData deserialized = objectMapper.readValue(json, NestedData.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.child().id(), deserialized.child().id());
    }

    @Test
    @DisplayName("null值字段不出现在JSON中")
    void nullFieldsExcludedFromJson() throws JsonProcessingException {
        SimpleData data = new SimpleData(1L, "test", null);

        String json = objectMapper.writeValueAsString(data);

        assertFalse(json.contains("\"value\":null"), "null values should be excluded");
        assertTrue(json.contains("\"id\":"));
        assertTrue(json.contains("\"name\":"));
    }

    @Test
    @DisplayName("日期时间正确序列化")
    void dateTimeSerializationCorrect() throws JsonProcessingException {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        SimpleData data = new SimpleData(1L, "test", 42);

        record DateWrapper(SimpleData data, LocalDateTime timestamp) {}
        DateWrapper wrapper = new DateWrapper(data, dateTime);

        String json = objectMapper.writeValueAsString(wrapper);

        assertTrue(json.contains("2024"));
    }

    @Test
    @DisplayName("特殊字符正确转义")
    void specialCharactersEscaped() throws JsonProcessingException {
        String testString = "test\"value\\with/slashes";
        SimpleData data = new SimpleData(1L, testString, 1);

        String json = objectMapper.writeValueAsString(data);
        SimpleData deserialized = objectMapper.readValue(json, SimpleData.class);

        assertEquals(data.name(), deserialized.name());
    }

    @Test
    @DisplayName("Unicode字符正确处理")
    void unicodeCharactersHandled() throws JsonProcessingException {
        String unicodeString = "测试数据 🎉 ñ 中文";
        SimpleData data = new SimpleData(1L, unicodeString, 1);

        String json = objectMapper.writeValueAsString(data);
        SimpleData deserialized = objectMapper.readValue(json, SimpleData.class);

        assertEquals(data.name(), deserialized.name());
    }

    @Test
    @DisplayName("数字字段正确序列化")
    void numericFieldsSerializedCorrectly() throws JsonProcessingException {
        SimpleData data = new SimpleData(123L, "test", 456);

        String json = objectMapper.writeValueAsString(data);
        SimpleData deserialized = objectMapper.readValue(json, SimpleData.class);

        assertEquals(data.id(), deserialized.id());
        assertEquals(data.value(), deserialized.value());
    }

    @Test
    @DisplayName("空列表和空map正确处理")
    void emptyCollectionsHandled() throws JsonProcessingException {
        record CollectionData(Long id, String name, List<String> emptyList, Map<String, String> emptyMap) {}
        CollectionData data = new CollectionData(1L, "test", List.of(), Map.of());

        String json = objectMapper.writeValueAsString(data);
        CollectionData deserialized = objectMapper.readValue(json, CollectionData.class);

        assertTrue(deserialized.emptyList().isEmpty());
        assertTrue(deserialized.emptyMap().isEmpty());
    }

    @Test
    @DisplayName("JSON输出包含所有必要字段")
    void jsonOutputContainsAllRequiredFields() throws JsonProcessingException {
        SimpleData data = new SimpleData(1L, "test", 42);

        String json = objectMapper.writeValueAsString(data);

        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"value\""));
    }

    @Test
    @DisplayName("布尔值正确序列化")
    void booleanValuesSerializedCorrectly() throws JsonProcessingException {
        record BooleanData(Long id, String name, boolean active) {}
        BooleanData data = new BooleanData(1L, "test", true);

        String json = objectMapper.writeValueAsString(data);
        BooleanData deserialized = objectMapper.readValue(json, BooleanData.class);

        assertEquals(data.active(), deserialized.active());
    }

    @Test
    @DisplayName("浮点数正确序列化")
    void floatingPointValuesSerialized() throws JsonProcessingException {
        record DoubleData(Long id, String name, Double value) {}
        DoubleData data = new DoubleData(1L, "test", 3.14159);

        String json = objectMapper.writeValueAsString(data);
        DoubleData deserialized = objectMapper.readValue(json, DoubleData.class);

        assertEquals(data.value(), deserialized.value());
    }
}
