package com.tavemakers.surf.domain.score.util;

import com.tavemakers.surf.domain.score.entity.ScoreComputable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ScoreCalculator {

    public BigDecimal calculateScore(ScoreComputable scoreComputable, BigDecimal delta) {
        return scoreComputable.updateScore(delta);
    }

}
