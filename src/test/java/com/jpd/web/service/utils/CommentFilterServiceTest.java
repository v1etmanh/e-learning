package com.jpd.web.service.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommentFilterServiceTest {

    private CommentFilterService commentFilterService;

    @BeforeEach
    void setUp() {
        commentFilterService = new CommentFilterService();
        // @PostConstruct isn't invoked outside a Spring context; call it manually
        // against the deterministic src/test/resources/blacklist.json fixture
        // ("badword" weight 1.0 + variant "bw1", "worseword" weight 1.5 + variant "ww1").
        commentFilterService.init();
    }

    @Test
    void isToxic_returnsFalse_forNullOrBlankInput() {
        assertThat(commentFilterService.isToxic(null)).isFalse();
        assertThat(commentFilterService.isToxic("")).isFalse();
        assertThat(commentFilterService.isToxic("   ")).isFalse();
    }

    @Test
    void isToxic_returnsFalse_whenScoreBelowThreshold() {
        // "worseword" alone weighs 1.5, below the 2.0 threshold.
        assertThat(commentFilterService.isToxic("just worseword here")).isFalse();
    }

    @Test
    void isToxic_returnsTrue_whenCombinedWeightsReachThreshold() {
        // badword(1.0) + worseword(1.5) = 2.5 >= 2.0
        assertThat(commentFilterService.isToxic("this has badword and worseword")).isTrue();
    }

    @Test
    void isToxic_isCaseInsensitive() {
        assertThat(commentFilterService.isToxic("BADWORD WORSEWORD")).isTrue();
    }

    @Test
    void isToxic_stillDetectsWords_whenSurroundedByPunctuation() {
        assertThat(commentFilterService.isToxic("wow, badword!!! and worseword?")).isTrue();
    }

    @Test
    void isToxic_detectsVariants_sameAsCanonicalWord() {
        // "bw1" is a variant of "badword" (1.0), "ww1" is a variant of "worseword" (1.5)
        assertThat(commentFilterService.isToxic("bw1 and ww1")).isTrue();
    }

    @Test
    void analyze_isConsistentWithIsToxic() {
        String toxicComment = "this has badword and worseword";
        Map<String, Object> result = commentFilterService.analyze(toxicComment);

        assertThat(result.get("toxic")).isEqualTo(true);
        assertThat((double) result.get("score")).isEqualTo(2.5);
        assertThat(result.get("comment")).isEqualTo(toxicComment);

        Map<String, Object> cleanResult = commentFilterService.analyze("nothing wrong here");
        assertThat(cleanResult.get("toxic")).isEqualTo(false);
        assertThat((double) cleanResult.get("score")).isEqualTo(0.0);
    }
}
