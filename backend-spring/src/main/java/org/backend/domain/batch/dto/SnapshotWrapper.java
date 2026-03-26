package org.backend.domain.batch.dto;

import org.backend.domain.analysis.entity.Analysis;
import org.backend.domain.analysis.entity.Invoice;
import org.backend.domain.batch.entity.SnapshotBilling;

public record SnapshotWrapper(
    Invoice invoice,
    SnapshotBilling lastSnapshot,
    Analysis analysis
) {}
