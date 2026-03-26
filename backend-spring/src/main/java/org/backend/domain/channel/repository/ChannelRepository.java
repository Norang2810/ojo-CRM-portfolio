package org.backend.domain.channel.repository;

import org.backend.domain.channel.entity.Channel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    // 목록 조회 - adminId 필터 + channelMembers 함께 fetch (memberCount N+1 방지)
    @EntityGraph(attributePaths = {"channelMembers"})
    List<Channel> findAllByAdminId(Long adminId);

    // 단건 조회 - channelMembers 함께 fetch (memberCount 필요한 경우)
    @Query("SELECT c FROM Channel c LEFT JOIN FETCH c.channelMembers WHERE c.id = :id")
    Optional<Channel> findByIdWithMembers(@Param("id") Long id);

    // adminId + channelId 로 소유권 확인용 조회
    @Query("SELECT c FROM Channel c LEFT JOIN FETCH c.channelMembers WHERE c.id = :id AND c.adminId = :adminId")
    Optional<Channel> findByIdAndAdminIdWithMembers(@Param("id") Long id, @Param("adminId") Long adminId);

    boolean existsByIdAndAdminId(Long id, Long adminId);
}