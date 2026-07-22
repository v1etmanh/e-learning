package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Status", description = """
        Review/moderation state of a creator profile.

        * `PENDING` — application submitted, awaiting admin review (the state a new application starts in)
        * `SUCCESS` — approved; the creator may publish courses
        * `BANNED` — permanently blocked by an admin
        * `REJECTED` — application refused
        * `SUSPENDED` — temporarily blocked
        * `UNDER_REVIEW` — being re-examined by an admin
        """)
public enum Status {
    PENDING, SUCCESS,
    BANNED, REJECTED,SUSPENDED,UNDER_REVIEW
}
