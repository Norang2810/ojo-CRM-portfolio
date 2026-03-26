package org.backend.domain.member.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "members")
public class MemberSearchDocument {

    @Id
    private Long memberId;


    // korean -> nori 수정
    @Field(type = FieldType.Text, analyzer = "nori") // 한글 검색
    private String name;

    @Field(type = FieldType.Keyword)
    private String phone;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String status;

    protected MemberSearchDocument() {}

    public MemberSearchDocument(Long memberId, String name, String phone, String email,
                                LocalDateTime createdAt, String status) {
        this.memberId = memberId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
}