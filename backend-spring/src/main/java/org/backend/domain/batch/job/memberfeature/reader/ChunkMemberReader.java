package org.backend.domain.batch.job.memberfeature.reader;

import lombok.RequiredArgsConstructor;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class ChunkMemberReader implements ItemReader<List<Member>>, ItemStream {

    private final JpaPagingItemReader<Member> delegate;
    private final int chunkSize;


    private final AtomicBoolean exhausted = new AtomicBoolean(false);

    // Step 시작 시 delegate의 EntityManager 등 자원 초기화
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        exhausted.set(false);   // AtomicBoolean으로 가시성 보장
        delegate.open(executionContext);
    }

    // Step 완료/실패 시 자원 정리
    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    /**
     * synchronized: 멀티쓰레드 Step에서 여러 쓰레드가 동시에 read()를 호출할 때
     * JpaPagingItemReader(thread-safe하지 않음)에 대한 접근을 직렬화합니다.
     *
     * 동작 방식:
     *   - read() 자체는 직렬화 (한 번에 하나의 쓰레드만 CHUNK_SIZE만큼 읽음)
     *   - 읽어온 청크의 processor/writer 처리는 각 쓰레드가 병렬로 수행
     */
    @Override
    public synchronized List<Member> read() throws Exception {
        if (exhausted.get()) return null;

        List<Member> chunk = new ArrayList<>(chunkSize);
        for (int i = 0; i < chunkSize; i++) {
            Member member = delegate.read();
            if (member == null) {
                exhausted.set(true);
                break;
            }
            chunk.add(member);
        }
        return chunk.isEmpty() ? null : chunk;
    }
}