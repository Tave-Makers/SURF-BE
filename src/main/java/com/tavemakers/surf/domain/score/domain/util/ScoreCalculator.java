package com.tavemakers.surf.domain.score.domain.util;

import com.tavemakers.surf.domain.score.domain.entity.ScoreComputable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ScoreCalculator {

    public BigDecimal calculateScore(ScoreComputable scoreComputable, BigDecimal delta) {
        return scoreComputable.updateScore(delta);
    }

}
