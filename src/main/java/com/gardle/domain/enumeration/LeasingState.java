package com.gardle.domain.enumeration;

import java.time.Instant;

public enum LeasingState {
    PAST {
        @Override
        public QueryInstants queryInstants(Instant now) {
            return new QueryInstants(now, null, null);
        }
    },
    ONGOING {
        @Override
        public QueryInstants queryInstants(Instant now) {
            return new QueryInstants(null, now, null);
        }
    },
    FUTURE {
        @Override
        public QueryInstants queryInstants(Instant now) {
            return new QueryInstants(null, null, now);
        }
    };

    public abstract QueryInstants queryInstants(Instant now);

    public static QueryInstants queryInstantsFor(LeasingState state, Instant now) {
        return state == null ? new QueryInstants(null, null, null) : state.queryInstants(now);
    }

    public static final class QueryInstants {
        private final Instant past;
        private final Instant ongoing;
        private final Instant future;

        private QueryInstants(Instant past, Instant ongoing, Instant future) {
            this.past = past;
            this.ongoing = ongoing;
            this.future = future;
        }

        public Instant getPast() { return past; }
        public Instant getOngoing() { return ongoing; }
        public Instant getFuture() { return future; }
    }
}
