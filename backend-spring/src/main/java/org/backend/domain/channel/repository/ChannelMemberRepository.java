package org.backend.domain.channel.repository;

import org.backend.domain.channel.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, Long> {

    @Query("SELECT cm FROM ChannelMember cm JOIN FETCH cm.channel WHERE cm.channel.id = :channelId")
    List<ChannelMember> findByChannelId(@Param("channelId") Long channelId);

    Optional<ChannelMember> findByChannelIdAndMemberId(Long channelId, Long memberId);

    boolean existsByChannelIdAndMemberId(Long channelId, Long memberId);

    @Query("SELECT cm.memberId FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.memberId IN :memberIds")
    Set<Long> findExistingMemberIds(@Param("channelId") Long channelId, @Param("memberIds") List<Long> memberIds);

    // 여러 명 한번에 삭제
    @Modifying
    @Query("DELETE FROM ChannelMember cm WHERE cm.channel.id = :channelId AND cm.memberId IN :memberIds")
    void deleteByChannelIdAndMemberIdIn(@Param("channelId") Long channelId, @Param("memberIds") List<Long> memberIds);

    // 채널 전체 비우기
    @Modifying
    @Query("DELETE FROM ChannelMember cm WHERE cm.channel.id = :channelId")
    void deleteAllByChannelId(@Param("channelId") Long channelId);
}