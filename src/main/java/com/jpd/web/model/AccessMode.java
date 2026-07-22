package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessMode", description = """
        How learners gain access to a course.

        * `PUBLIC` - anyone may enroll; the join key is ignored
        * `PRIVATE` - enrollment requires the course join key
        """)
public enum AccessMode {
    PUBLIC,
    PRIVATE
}
